package com.example.ontology.dto.response;

import com.example.ontology.domain.model.ModelStatistics;
import com.example.ontology.domain.model.OntologyModel;

import java.time.Instant;

/** Compact representation used by {@code GET /models}. */
public class ModelSummary {

    private String id;
    private String name;
    private String state;
    private String profile;
    private String aboxSource;
    private Instant createdAt;
    private Instant loadedAt;
    private Instant reasonedAt;
    private long tboxTriples;
    private long virtualAboxTriples;
    private long inferenceModelTriples;
    private long inferredApplicationTriples;

    public static ModelSummary from(OntologyModel model) {
        ModelSummary summary = new ModelSummary();
        summary.id = model.getId();
        summary.name = model.getName();
        summary.state = model.getState().name();
        summary.profile = model.getProfile();
        summary.aboxSource = model.getAboxSource();
        summary.createdAt = model.getCreatedAt();
        summary.loadedAt = model.getLoadedAt();
        summary.reasonedAt = model.getReasonedAt();
        ModelStatistics statistics = model.getStatistics();
        if (statistics != null) {
            summary.tboxTriples = statistics.getTboxTriples();
            summary.virtualAboxTriples = statistics.getVirtualAboxTriples();
            summary.inferenceModelTriples = statistics.getInferenceModelTriples();
            summary.inferredApplicationTriples = statistics.getInferredApplicationTriples();
        }
        return summary;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getState() {
        return state;
    }

    public String getProfile() {
        return profile;
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

    public long getTboxTriples() {
        return tboxTriples;
    }

    public long getVirtualAboxTriples() {
        return virtualAboxTriples;
    }

    public long getInferenceModelTriples() {
        return inferenceModelTriples;
    }

    public long getInferredApplicationTriples() {
        return inferredApplicationTriples;
    }
}
