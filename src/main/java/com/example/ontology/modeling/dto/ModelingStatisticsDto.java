package com.example.ontology.modeling.dto;

import com.example.ontology.modeling.domain.ModelingStatistics;

/** Statistics of a modeling project. */
public class ModelingStatisticsDto {

    private long textChars;
    private int chunkCount;
    private int classes;
    private int objectProperties;
    private int datatypeProperties;
    private int individuals;
    private int relations;
    private int literals;
    private long tboxTriples;
    private long aboxTriples;
    private long extractionMillis;
    private long generationMillis;

    public static ModelingStatisticsDto from(ModelingStatistics statistics) {
        if (statistics == null) {
            return null;
        }
        ModelingStatisticsDto dto = new ModelingStatisticsDto();
        dto.textChars = statistics.getTextChars();
        dto.chunkCount = statistics.getChunkCount();
        dto.classes = statistics.getClasses();
        dto.objectProperties = statistics.getObjectProperties();
        dto.datatypeProperties = statistics.getDatatypeProperties();
        dto.individuals = statistics.getIndividuals();
        dto.relations = statistics.getRelations();
        dto.literals = statistics.getLiterals();
        dto.tboxTriples = statistics.getTboxTriples();
        dto.aboxTriples = statistics.getAboxTriples();
        dto.extractionMillis = statistics.getExtractionMillis();
        dto.generationMillis = statistics.getGenerationMillis();
        return dto;
    }

    public long getTextChars() {
        return textChars;
    }

    public int getChunkCount() {
        return chunkCount;
    }

    public int getClasses() {
        return classes;
    }

    public int getObjectProperties() {
        return objectProperties;
    }

    public int getDatatypeProperties() {
        return datatypeProperties;
    }

    public int getIndividuals() {
        return individuals;
    }

    public int getRelations() {
        return relations;
    }

    public int getLiterals() {
        return literals;
    }

    public long getTboxTriples() {
        return tboxTriples;
    }

    public long getAboxTriples() {
        return aboxTriples;
    }

    public long getExtractionMillis() {
        return extractionMillis;
    }

    public long getGenerationMillis() {
        return generationMillis;
    }
}
