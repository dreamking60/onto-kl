package com.example.ontology.infrastructure.db;

import com.example.ontology.domain.exception.ModelLoadException;
import com.example.ontology.domain.model.DbConfig;
import com.example.ontology.domain.model.InferredTriple;
import com.example.ontology.domain.model.TripleObjectKind;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Control-plane JDBC access to the TDSQL/MySQL database that holds the A-Box
 * (instances) of one ontology model.
 *
 * <p>Ontop opens its own connections; this class is used for schema bootstrap,
 * statistics and persisting the inference results.</p>
 */
public final class InstanceDatabase implements AutoCloseable {

    private final Connection connection;
    private final Path schemaSql;
    private final Path seedSql;
    private final String inferredTable;

    public InstanceDatabase(DbConfig dbConfig, Path schemaSql, Path seedSql, String inferredTable) {
        this.schemaSql = schemaSql;
        this.seedSql = seedSql;
        this.inferredTable = inferredTable;
        try {
            Class.forName(dbConfig.getDriver());
            this.connection = DriverManager.getConnection(
                    dbConfig.getUrl(), dbConfig.getUsername(), dbConfig.getPassword());
        } catch (ClassNotFoundException e) {
            throw new ModelLoadException("JDBC driver not on the classpath: " + dbConfig.getDriver(), e);
        } catch (SQLException e) {
            throw new ModelLoadException("Cannot connect to " + dbConfig.getUrl()
                    + " (profile=" + dbConfig.getProfile() + "): " + e.getMessage(), e);
        }
    }

    // ------------------------------------------------------------------
    // Bootstrap / inspection
    // ------------------------------------------------------------------

    public void initializeSchemaAndData() {
        runScript(schemaSql);
        runScript(seedSql);
    }

    public void runScript(Path script) {
        if (!Files.isRegularFile(script)) {
            throw new ModelLoadException("SQL script not found: " + script.toAbsolutePath());
        }
        String sql;
        try {
            sql = new String(Files.readAllBytes(script), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ModelLoadException("Cannot read SQL script " + script.toAbsolutePath(), e);
        }
        StringBuilder cleaned = new StringBuilder();
        for (String line : sql.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("--") || trimmed.startsWith("//")) {
                continue;
            }
            cleaned.append(line).append('\n');
        }
        try (Statement statement = connection.createStatement()) {
            for (String raw : cleaned.toString().split(";")) {
                String statementSql = raw.trim();
                if (!statementSql.isEmpty()) {
                    statement.execute(statementSql);
                }
            }
        } catch (SQLException e) {
            throw new ModelLoadException("SQL script failed (" + script.getFileName() + "): "
                    + e.getMessage(), e);
        }
    }

    public String productDescription() {
        try {
            DatabaseMetaData meta = connection.getMetaData();
            return meta.getDatabaseProductName() + " " + meta.getDatabaseProductVersion()
                    + " (driver " + meta.getDriverName() + " " + meta.getDriverVersion() + ")";
        } catch (SQLException e) {
            return "unknown database";
        }
    }

    public boolean tableExists(String table) {
        try {
            DatabaseMetaData meta = connection.getMetaData();
            for (String candidate : new String[]{table, table.toUpperCase(), table.toLowerCase()}) {
                try (ResultSet rs = meta.getTables(null, null, candidate, new String[]{"TABLE"})) {
                    if (rs.next()) {
                        return true;
                    }
                }
            }
            return false;
        } catch (SQLException e) {
            throw new ModelLoadException("Cannot inspect database metadata: " + e.getMessage(), e);
        }
    }

    public int count(String table) {
        if (!table.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            throw new IllegalArgumentException("Illegal table name: " + table);
        }
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new ModelLoadException("Cannot count rows of " + table + ": " + e.getMessage(), e);
        }
    }

    // ------------------------------------------------------------------
    // Materialisation of inferred triples
    // ------------------------------------------------------------------

    /** Replaces the inferred-triples table content in one transaction. */
    public int replaceInferred(List<InferredTriple> triples) {
        if (!inferredTable.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            throw new IllegalArgumentException("Illegal materialisation table name: " + inferredTable);
        }
        String insert = "INSERT INTO " + inferredTable
                + " (subject, predicate, object, object_kind, datatype, lang) VALUES (?, ?, ?, ?, ?, ?)";
        boolean autoCommit = true;
        try {
            autoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try (Statement delete = connection.createStatement()) {
                delete.executeUpdate("DELETE FROM " + inferredTable);
            }
            try (PreparedStatement ps = connection.prepareStatement(insert)) {
                for (InferredTriple triple : triples) {
                    ps.setString(1, triple.getSubject());
                    ps.setString(2, triple.getPredicate());
                    ps.setString(3, triple.getObject());
                    ps.setString(4, triple.getObjectKind() == null
                            ? TripleObjectKind.IRI.name() : triple.getObjectKind().name());
                    ps.setString(5, triple.getDatatype());
                    ps.setString(6, triple.getLanguage());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            connection.commit();
            return triples.size();
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException ignored) {
                // best effort
            }
            throw new ModelLoadException("Cannot materialise inferred triples: " + e.getMessage(), e);
        } finally {
            try {
                connection.setAutoCommit(autoCommit);
            } catch (SQLException ignored) {
                // best effort
            }
        }
    }

    /** Reads materialised triples back (used by tests / diagnostics). */
    public List<InferredTriple> readInferred(int limit) {
        if (!inferredTable.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            throw new IllegalArgumentException("Illegal materialisation table name: " + inferredTable);
        }
        String sql = "SELECT subject, predicate, object, object_kind, datatype, lang FROM "
                + inferredTable + " ORDER BY subject, predicate, object";
        List<InferredTriple> triples = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next() && triples.size() < limit) {
                String kind = rs.getString(4);
                triples.add(new InferredTriple(rs.getString(1), rs.getString(2), rs.getString(3),
                        kind == null ? TripleObjectKind.IRI : TripleObjectKind.valueOf(kind),
                        rs.getString(5), rs.getString(6)));
            }
            return triples;
        } catch (SQLException e) {
            throw new ModelLoadException("Cannot read materialised triples: " + e.getMessage(), e);
        }
    }

    @Override
    public void close() {
        try {
            connection.close();
        } catch (SQLException ignored) {
            // best effort
        }
    }
}
