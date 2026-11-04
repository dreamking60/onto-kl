package com.example.ontology.dto.response;

import com.example.ontology.domain.model.ModelStatistics;

/** Triple / inference statistics returned by the REST API. */
public class StatisticsDto {

    private long tboxTriples;
    private long virtualAboxTriples;
    private long assertedTriples;
    private long afterBusinessRulesTriples;
    private long inferenceModelTriples;
    private long inferredTriples;
    private long inferredApplicationTriples;
    private long loadMillis;
    private long reasonMillis;
    private OntologySummaryDto ontology;

    public static StatisticsDto from(ModelStatistics statistics) {
        if (statistics == null) {
            return null;
        }
        StatisticsDto dto = new StatisticsDto();
        dto.tboxTriples = statistics.getTboxTriples();
        dto.virtualAboxTriples = statistics.getVirtualAboxTriples();
        dto.assertedTriples = statistics.getAssertedTriples();
        dto.afterBusinessRulesTriples = statistics.getAfterBusinessRulesTriples();
        dto.inferenceModelTriples = statistics.getInferenceModelTriples();
        dto.inferredTriples = statistics.getInferredTriples();
        dto.inferredApplicationTriples = statistics.getInferredApplicationTriples();
        dto.loadMillis = statistics.getLoadMillis();
        dto.reasonMillis = statistics.getReasonMillis();
        dto.ontology = OntologySummaryDto.from(statistics.getOntology());
        return dto;
    }

    public long getTboxTriples() {
        return tboxTriples;
    }

    public long getVirtualAboxTriples() {
        return virtualAboxTriples;
    }

    public long getAssertedTriples() {
        return assertedTriples;
    }

    public long getAfterBusinessRulesTriples() {
        return afterBusinessRulesTriples;
    }

    public long getInferenceModelTriples() {
        return inferenceModelTriples;
    }

    public long getInferredTriples() {
        return inferredTriples;
    }

    public long getInferredApplicationTriples() {
        return inferredApplicationTriples;
    }

    public long getLoadMillis() {
        return loadMillis;
    }

    public long getReasonMillis() {
        return reasonMillis;
    }

    public OntologySummaryDto getOntology() {
        return ontology;
    }
}
