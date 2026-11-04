package com.example.ontology.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;
import java.nio.file.Paths;

/** Strongly typed application configuration (prefix {@code app}). */
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private String dataDir = "data/models";
    private String defaultProfile = "tdsql";
    private int maxQueryRows = 500;
    private int maxInferredSample = 50;

    private Reasoning reasoning = new Reasoning();
    private Defaults defaults = new Defaults();
    private Init init = new Init();
    private DataSources datasource = new DataSources();

    // ------------------------------------------------------------------
    // Path helpers
    // ------------------------------------------------------------------

    /** Absolute path of the local directory that stores ontology/mapping files. */
    public Path dataDirPath() {
        return resolve(dataDir);
    }

    /** Resolves a configured path against the process working directory. */
    public Path resolve(String path) {
        Path candidate = Paths.get(path);
        if (candidate.isAbsolute()) {
            return candidate.normalize();
        }
        return Paths.get("").toAbsolutePath().resolve(candidate).normalize();
    }

    public Path schemaSqlPath() {
        return resolve(init.getSchemaSql());
    }

    public Path seedSqlPath() {
        return resolve(init.getSeedSql());
    }

    // ------------------------------------------------------------------
    // Getters / setters (Spring Boot relaxed binding)
    // ------------------------------------------------------------------

    public String getDataDir() {
        return dataDir;
    }

    public void setDataDir(String dataDir) {
        this.dataDir = dataDir;
    }

    public String getDefaultProfile() {
        return defaultProfile;
    }

    public void setDefaultProfile(String defaultProfile) {
        this.defaultProfile = defaultProfile;
    }

    public int getMaxQueryRows() {
        return maxQueryRows;
    }

    public void setMaxQueryRows(int maxQueryRows) {
        this.maxQueryRows = maxQueryRows;
    }

    public int getMaxInferredSample() {
        return maxInferredSample;
    }

    public void setMaxInferredSample(int maxInferredSample) {
        this.maxInferredSample = maxInferredSample;
    }

    public Reasoning getReasoning() {
        return reasoning;
    }

    public void setReasoning(Reasoning reasoning) {
        this.reasoning = reasoning;
    }

    public Defaults getDefaults() {
        return defaults;
    }

    public void setDefaults(Defaults defaults) {
        this.defaults = defaults;
    }

    public Init getInit() {
        return init;
    }

    public void setInit(Init init) {
        this.init = init;
    }

    public DataSources getDatasource() {
        return datasource;
    }

    public void setDatasource(DataSources datasource) {
        this.datasource = datasource;
    }

    // ------------------------------------------------------------------
    // Nested groups
    // ------------------------------------------------------------------

    public static class Reasoning {
        private boolean materializeEnabled = true;
        private String materializeTable = "inferred_triple";

        public boolean isMaterializeEnabled() {
            return materializeEnabled;
        }

        public void setMaterializeEnabled(boolean materializeEnabled) {
            this.materializeEnabled = materializeEnabled;
        }

        public String getMaterializeTable() {
            return materializeTable;
        }

        public void setMaterializeTable(String materializeTable) {
            this.materializeTable = materializeTable;
        }
    }

    public static class Defaults {
        private String ontologyPath = "ontology/company.ttl";
        private String mappingPath = "mapping/company.obda";
        private String rulesPath = "ontology/company.rules";

        public String getOntologyPath() {
            return ontologyPath;
        }

        public void setOntologyPath(String ontologyPath) {
            this.ontologyPath = ontologyPath;
        }

        public String getMappingPath() {
            return mappingPath;
        }

        public void setMappingPath(String mappingPath) {
            this.mappingPath = mappingPath;
        }

        public String getRulesPath() {
            return rulesPath;
        }

        public void setRulesPath(String rulesPath) {
            this.rulesPath = rulesPath;
        }
    }

    public static class Init {
        private String schemaSql = "sql/schema.sql";
        private String seedSql = "sql/seed.sql";

        public String getSchemaSql() {
            return schemaSql;
        }

        public void setSchemaSql(String schemaSql) {
            this.schemaSql = schemaSql;
        }

        public String getSeedSql() {
            return seedSql;
        }

        public void setSeedSql(String seedSql) {
            this.seedSql = seedSql;
        }
    }

    public static class DataSources {
        private Tdsql tdsql = new Tdsql();
        private H2 h2 = new H2();

        public Tdsql getTdsql() {
            return tdsql;
        }

        public void setTdsql(Tdsql tdsql) {
            this.tdsql = tdsql;
        }

        public H2 getH2() {
            return h2;
        }

        public void setH2(H2 h2) {
            this.h2 = h2;
        }
    }

    public static class Tdsql {
        private String driver = "com.mysql.cj.jdbc.Driver";
        private String url;
        private String username;
        private String password;

        public String getDriver() {
            return driver;
        }

        public void setDriver(String driver) {
            this.driver = driver;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    public static class H2 {
        private String driver = "org.h2.Driver";
        private String urlTemplate = "jdbc:h2:mem:onto_{modelId};MODE=MySQL;DB_CLOSE_DELAY=-1";
        private String username = "sa";
        private String password = "";

        public String getDriver() {
            return driver;
        }

        public void setDriver(String driver) {
            this.driver = driver;
        }

        public String getUrlTemplate() {
            return urlTemplate;
        }

        public void setUrlTemplate(String urlTemplate) {
            this.urlTemplate = urlTemplate;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}
