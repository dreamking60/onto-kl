package com.example.ontology.dto.response;

import com.example.ontology.domain.model.OntologyModel;

import java.time.Instant;

/** Detailed representation used by {@code GET /models/{modelId}} and {@code POST /load}. */
public class ModelDetail {

    private String id;
    private String name;
    private String description;
    private String state;
    private String profile;
    private String jdbcUrl;
    private String database;
    private String ontologyFile;
    private String mappingFile;
    private String rulesFile;
    private String instancesFile;
    private String aboxSource;
    private Instant createdAt;
    private Instant loadedAt;
    private Instant reasonedAt;
    private Instant materializedAt;
    private Integer materializedTriples;
    private StatisticsDto statistics;

    public static ModelDetail from(OntologyModel model) {
        ModelDetail detail = new ModelDetail();
        detail.id = model.getId();
        detail.name = model.getName();
        detail.description = model.getDescription();
        detail.state = model.getState().name();
        detail.profile = model.getProfile();
        detail.jdbcUrl = model.getJdbcUrl();
        detail.ontologyFile = String.valueOf(model.getOntologyFile());
        detail.mappingFile = String.valueOf(model.getMappingFile());
        detail.rulesFile = model.getRulesFile() == null ? null : String.valueOf(model.getRulesFile());
        detail.instancesFile = model.getInstancesFile() == null ? null : String.valueOf(model.getInstancesFile());
        detail.aboxSource = model.getAboxSource();
        detail.createdAt = model.getCreatedAt();
        detail.loadedAt = model.getLoadedAt();
        detail.reasonedAt = model.getReasonedAt();
        detail.materializedAt = model.getMaterializedAt();
        detail.materializedTriples = model.getMaterializedTriples();
        detail.statistics = StatisticsDto.from(model.getStatistics());
        return detail;
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

    public String getState() {
        return state;
    }

    public String getProfile() {
        return profile;
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public String getOntologyFile() {
        return ontologyFile;
    }

    public String getMappingFile() {
        return mappingFile;
    }

    public String getRulesFile() {
        return rulesFile;
    }

    public String getInstancesFile() {
        return instancesFile;
    }

    public String getAboxSource() {
        return aboxSource;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLoadedAt() {
        return loadedAt;
    }

    public Instant getReasonedAt() {
        return reasonedAt;
    }

    public Instant getMaterializedAt() {
        return materializedAt;
    }

    public Integer getMaterializedTriples() {
        return materializedTriples;
    }

    public StatisticsDto getStatistics() {
        return statistics;
    }
}
