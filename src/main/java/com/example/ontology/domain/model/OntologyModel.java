package com.example.ontology.domain.model;

import java.nio.file.Path;
import java.time.Instant;

/**
 * Aggregate root describing one ontology model loaded into the server.
 *
 * <p>The ontology definition (T-Box), the OBDA mapping, the business rules and
 * optionally a local A-Box file are always local files.  When an OBDA mapping
 * is present the instances stay in TDSQL and are virtualised by Ontop; when only
 * a local A-Box file is provided (e.g. an ontology generated from a document)
 * the instances are read directly from that file.</p>
 */
public class OntologyModel {

    private final String id;
    private final String name;
    private final String description;
    private final String profile;
    private final String jdbcUrl;
    private final Path ontologyFile;
    private final Path mappingFile;
    private final Path rulesFile;
    private final Path instancesFile;
    /** tdsql | local | mixed */
    private final String aboxSource;
    private final Instant createdAt;

    private volatile ModelState state = ModelState.REGISTERED;
    private volatile ModelStatistics statistics;
    private volatile Instant loadedAt;
    private volatile Instant reasonedAt;
    private volatile Instant materializedAt;
    private volatile Integer materializedTriples;

    public OntologyModel(String id, String name, String description, String profile, String jdbcUrl,
                         Path ontologyFile, Path mappingFile, Path rulesFile, Path instancesFile,
                         String aboxSource, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.profile = profile;
        this.jdbcUrl = jdbcUrl;
        this.ontologyFile = ontologyFile;
        this.mappingFile = mappingFile;
        this.rulesFile = rulesFile;
        this.instancesFile = instancesFile;
        this.aboxSource = aboxSource;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getProfile() {
        return profile;
    }

    public String getJdbcUrl() {
        return jdbcUrl;
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

    public String getAboxSource() {
        return aboxSource;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public ModelState getState() {
        return state;
    }

    public void setState(ModelState state) {
        this.state = state;
    }

    public ModelStatistics getStatistics() {
        return statistics;
    }

    public void setStatistics(ModelStatistics statistics) {
        this.statistics = statistics;
    }

    public Instant getLoadedAt() {
        return loadedAt;
    }

    public void setLoadedAt(Instant loadedAt) {
        this.loadedAt = loadedAt;
    }

    public Instant getReasonedAt() {
        return reasonedAt;
    }

    public void setReasonedAt(Instant reasonedAt) {
        this.reasonedAt = reasonedAt;
    }

    public Instant getMaterializedAt() {
        return materializedAt;
    }

    public void setMaterializedAt(Instant materializedAt) {
        this.materializedAt = materializedAt;
    }

    public Integer getMaterializedTriples() {
        return materializedTriples;
    }

    public void setMaterializedTriples(Integer materializedTriples) {
        this.materializedTriples = materializedTriples;
    }
}
