package com.example.ontology.modeling.dto;

import com.example.ontology.modeling.domain.ModelingProject;

import java.time.Instant;

/** Compact representation of a modeling project. */
public class ModelingProjectSummary {

    private String id;
    private String name;
    private String status;
    private String sourceFileName;
    private String sourceType;
    private String aiProvider;
    private String aiModel;
    private String modelId;
    private Instant createdAt;
    private Instant updatedAt;
    private ModelingStatisticsDto statistics;

    public static ModelingProjectSummary from(ModelingProject project) {
        ModelingProjectSummary summary = new ModelingProjectSummary();
        summary.id = project.getId();
        summary.name = project.getName();
        summary.status = project.getStatus().name();
        summary.sourceFileName = project.getSourceFileName();
        summary.sourceType = project.getSourceType();
        summary.aiProvider = project.getAiProvider();
        summary.aiModel = project.getAiModel();
        summary.modelId = project.getModelId();
        summary.createdAt = project.getCreatedAt();
        summary.updatedAt = project.getUpdatedAt();
        summary.statistics = ModelingStatisticsDto.from(project.getStatistics());
        return summary;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getStatus() {
        return status;
    }

    public String getSourceFileName() {
        return sourceFileName;
    }

    public String getSourceType() {
        return sourceType;
    }

    public String getAiProvider() {
        return aiProvider;
    }

    public String getAiModel() {
        return aiModel;
    }

    public String getModelId() {
        return modelId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public ModelingStatisticsDto getStatistics() {
        return statistics;
    }
}
