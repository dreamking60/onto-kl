package com.example.ontology.infrastructure.loader;

import com.example.ontology.config.AppProperties;
import com.example.ontology.domain.exception.InvalidModelException;
import com.example.ontology.domain.exception.ModelLoadException;
import com.example.ontology.domain.model.DbConfig;
import com.example.ontology.domain.model.ModelState;
import com.example.ontology.domain.model.ModelStatistics;
import com.example.ontology.domain.model.OntologyModel;
import com.example.ontology.domain.model.OntologySummary;
import com.example.ontology.infrastructure.db.InstanceDatabase;
import com.example.ontology.infrastructure.ontop.OntopVirtualGraph;
import com.example.ontology.infrastructure.reasoning.JenaReasoningEngine;
import com.example.ontology.infrastructure.store.ModelContext;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

/**
 * Boots one ontology model: validates the local T-Box, connects to the
 * relational store, builds the Ontop virtual graph, pulls the virtual A-Box and
 * prepares the Jena reasoning engine.
 */
@Component
public class ModelLoader {

    private final AppProperties properties;

    public ModelLoader(AppProperties properties) {
        this.properties = properties;
    }

    public ModelContext load(LoadSpec spec) {
        long startedAt = System.currentTimeMillis();
        OntologySummary summary = OntologyAnalyzer.analyze(spec.getOntologyFile());

        boolean obda = spec.getMappingFile() != null;
        boolean localAbox = spec.getInstancesFile() != null;
        if (!obda && !localAbox) {
            throw new InvalidModelException("Model '" + spec.getModelId()
                    + "' needs either an OBDA mapping (TDSQL-backed A-Box) or a local instances file (A-Box)");
        }

        InstanceDatabase database = null;
        OntopVirtualGraph ontop = null;
        try {
            Model abox;
            String aboxSource;
            if (obda) {
                if (spec.getDbConfig() == null) {
                    throw new InvalidModelException("An OBDA mapping requires a database configuration");
                }
                database = new InstanceDatabase(spec.getDbConfig(),
                        properties.schemaSqlPath(), properties.seedSqlPath(),
                        properties.getReasoning().getMaterializeTable());
                if (spec.isInitDatabase() || "h2".equals(spec.getDbConfig().getProfile())) {
                    database.initializeSchemaAndData();
                }
                if (!database.tableExists("employee")) {
                    throw new ModelLoadException("The database at " + spec.getDbConfig().getUrl()
                            + " does not contain the A-Box tables (table 'employee' is missing). "
                            + "Initialise it with POST /load {\"initDatabase\":true} "
                            + "or run ./scripts/init-tdsql.sh for a TDSQL/MySQL database.");
                }
                ontop = OntopVirtualGraph.open(spec.getDbConfig(), spec.getOntologyFile(), spec.getMappingFile());
                abox = ontop.loadAllTriples();
                if (localAbox) {
                    abox.add(loadLocalAbox(spec.getInstancesFile()));
                    aboxSource = "mixed";
                } else {
                    aboxSource = "tdsql";
                }
            } else {
                abox = loadLocalAbox(spec.getInstancesFile());
                aboxSource = "local";
            }

            JenaReasoningEngine engine = new JenaReasoningEngine(
                    spec.getOntologyFile(), spec.getRulesFile(), abox);

            ModelStatistics statistics = engine.statistics();
            statistics.setOntology(summary);
            statistics.setLoadMillis(System.currentTimeMillis() - startedAt);

            String profile = obda ? spec.getDbConfig().getProfile() : "local";
            String jdbcUrl = obda ? spec.getDbConfig().getUrl() : null;
            OntologyModel model = new OntologyModel(
                    spec.getModelId(),
                    spec.getName(),
                    spec.getDescription(),
                    profile,
                    jdbcUrl,
                    spec.getOntologyFile(),
                    spec.getMappingFile(),
                    spec.getRulesFile(),
                    spec.getInstancesFile(),
                    aboxSource,
                    Instant.now());
            model.setState(ModelState.LOADED);
            model.setLoadedAt(Instant.now());
            model.setStatistics(statistics);
            return new ModelContext(model, database, ontop, engine);
        } catch (ModelLoadException e) {
            if (ontop != null) {
                ontop.close();
            }
            if (database != null) {
                database.close();
            }
            if (looksLikeOntologyOrMappingProblem(e)) {
                throw new InvalidModelException("Invalid ontology or mapping specification: "
                        + rootMessage(e), e);
            }
            throw e;
        } catch (RuntimeException e) {
            if (ontop != null) {
                ontop.close();
            }
            if (database != null) {
                database.close();
            }
            if (e instanceof InvalidModelException) {
                throw e;
            }
            throw new ModelLoadException("Failed to load ontology model '" + spec.getModelId()
                    + "': " + rootMessage(e), e);
        }
    }

    private static Model loadLocalAbox(Path instancesFile) {
        if (!Files.isRegularFile(instancesFile)) {
            throw new InvalidModelException("Local A-Box instances file not found: " + instancesFile);
        }
        try {
            if (Files.size(instancesFile) == 0) {
                return ModelFactory.createDefaultModel();
            }
            return RDFDataMgr.loadModel(instancesFile.toUri().toString());
        } catch (IOException e) {
            throw new InvalidModelException("Cannot read local A-Box instances file: " + instancesFile, e);
        } catch (RuntimeException e) {
            throw new InvalidModelException("Cannot parse local A-Box instances file " + instancesFile
                    + ": " + e.getMessage(), e);
        }
    }

    private static boolean looksLikeOntologyOrMappingProblem(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String name = current.getClass().getSimpleName();
            if (name.contains("OBDASpecification")
                    || name.contains("MappingException")
                    || name.contains("InvalidMapping")
                    || name.contains("InvalidOntology")
                    || name.contains("OntologyException")) {
                return true;
            }
            if (current.getCause() == current) {
                break;
            }
            current = current.getCause();
        }
        return false;
    }

    private static String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return message == null ? current.getClass().getSimpleName() : message;
    }

    /** Everything needed to boot one model. */
    public static final class LoadSpec {
        private final String modelId;
        private final String name;
        private final String description;
        private final Path ontologyFile;
        private final Path mappingFile;
        private final Path rulesFile;
        private final Path instancesFile;
        private final DbConfig dbConfig;
        private final boolean initDatabase;

        public LoadSpec(String modelId, String name, String description, Path ontologyFile,
                        Path mappingFile, Path rulesFile, Path instancesFile,
                        DbConfig dbConfig, boolean initDatabase) {
            this.modelId = modelId;
            this.name = name;
            this.description = description;
            this.ontologyFile = ontologyFile;
            this.mappingFile = mappingFile;
            this.rulesFile = rulesFile;
            this.instancesFile = instancesFile;
            this.dbConfig = dbConfig;
            this.initDatabase = initDatabase;
        }

        public String getModelId() {
            return modelId;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public Path getOntologyFile() {
            return ontologyFile;
        }

        public Path getMappingFile() {
            return mappingFile;
        }

        public Path getRulesFile() {
            return rulesFile;
        }

        public Path getInstancesFile() {
            return instancesFile;
        }

        public DbConfig getDbConfig() {
            return dbConfig;
        }

        public boolean isInitDatabase() {
            return initDatabase;
        }
    }
}
