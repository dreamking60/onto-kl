package com.example.ontology.domain.model;

/** Triple / inference statistics of one loaded ontology model. */
public class ModelStatistics {

    private long tboxTriples;
    private long virtualAboxTriples;
    private long assertedTriples;
    private long afterBusinessRulesTriples;
    private long inferenceModelTriples;
    private long inferredTriples;
    private long inferredApplicationTriples;
    private long loadMillis;
    private long reasonMillis;
    private OntologySummary ontology;

    public long getTboxTriples() {
        return tboxTriples;
    }

    public void setTboxTriples(long tboxTriples) {
        this.tboxTriples = tboxTriples;
    }

    public long getVirtualAboxTriples() {
        return virtualAboxTriples;
    }

    public void setVirtualAboxTriples(long virtualAboxTriples) {
        this.virtualAboxTriples = virtualAboxTriples;
    }

    public long getAssertedTriples() {
        return assertedTriples;
    }

    public void setAssertedTriples(long assertedTriples) {
        this.assertedTriples = assertedTriples;
    }

    public long getAfterBusinessRulesTriples() {
        return afterBusinessRulesTriples;
    }

    public void setAfterBusinessRulesTriples(long afterBusinessRulesTriples) {
        this.afterBusinessRulesTriples = afterBusinessRulesTriples;
    }

    public long getInferenceModelTriples() {
        return inferenceModelTriples;
    }

    public void setInferenceModelTriples(long inferenceModelTriples) {
        this.inferenceModelTriples = inferenceModelTriples;
    }

    public long getInferredTriples() {
        return inferredTriples;
    }

    public void setInferredTriples(long inferredTriples) {
        this.inferredTriples = inferredTriples;
    }

    public long getInferredApplicationTriples() {
        return inferredApplicationTriples;
    }

    public void setInferredApplicationTriples(long inferredApplicationTriples) {
        this.inferredApplicationTriples = inferredApplicationTriples;
    }

    public long getLoadMillis() {
        return loadMillis;
    }

    public void setLoadMillis(long loadMillis) {
        this.loadMillis = loadMillis;
    }

    public long getReasonMillis() {
        return reasonMillis;
    }

    public void setReasonMillis(long reasonMillis) {
        this.reasonMillis = reasonMillis;
    }

    public OntologySummary getOntology() {
        return ontology;
    }

    public void setOntology(OntologySummary ontology) {
        this.ontology = ontology;
    }
}
