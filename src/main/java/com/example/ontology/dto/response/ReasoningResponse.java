package com.example.ontology.dto.response;

import java.time.Instant;
import java.util.List;

/** Response of {@code POST /reason}. */
public class ReasoningResponse {

    private String modelId;
    private String state;
    private boolean computed;
    private long durationMs;
    private boolean materializeEnabled;
    private int materializedTriples;
    private long inferredTotal;
    private int inferredReturned;
    private List<InferredTripleDto> inferredTriples;
    private Instant reasonedAt;
    private StatisticsDto statistics;

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public boolean isComputed() {
        return computed;
    }

    public void setComputed(boolean computed) {
        this.computed = computed;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }

    public boolean isMaterializeEnabled() {
        return materializeEnabled;
    }

    public void setMaterializeEnabled(boolean materializeEnabled) {
        this.materializeEnabled = materializeEnabled;
    }

    public int getMaterializedTriples() {
        return materializedTriples;
    }

    public void setMaterializedTriples(int materializedTriples) {
        this.materializedTriples = materializedTriples;
    }

    public long getInferredTotal() {
        return inferredTotal;
    }

    public void setInferredTotal(long inferredTotal) {
        this.inferredTotal = inferredTotal;
    }

    public int getInferredReturned() {
        return inferredReturned;
    }

    public void setInferredReturned(int inferredReturned) {
        this.inferredReturned = inferredReturned;
    }

    public List<InferredTripleDto> getInferredTriples() {
        return inferredTriples;
    }

    public void setInferredTriples(List<InferredTripleDto> inferredTriples) {
        this.inferredTriples = inferredTriples;
    }

    public Instant getReasonedAt() {
        return reasonedAt;
    }

    public void setReasonedAt(Instant reasonedAt) {
        this.reasonedAt = reasonedAt;
    }

    public StatisticsDto getStatistics() {
        return statistics;
    }

    public void setStatistics(StatisticsDto statistics) {
        this.statistics = statistics;
    }
}
