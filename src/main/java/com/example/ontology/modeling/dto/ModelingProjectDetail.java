package com.example.ontology.modeling.dto;

import com.example.ontology.modeling.domain.ModelingProject;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Detailed representation of a modeling project. */
public class ModelingProjectDetail {

    private String id;
    private String name;
    private String status;
    private String sourceFileName;
    private String sourceType;
    private String namespace;
    private String aiProvider;
    private String aiModel;
    private String modelId;
    private Instant createdAt;
    private Instant updatedAt;
    private List<String> warnings;
    private String error;
    private ModelingStatisticsDto statistics;
    private Map<String, String> files;
    private String extractionPreview;
    private String ontologyPreview;
    private String instancesPreview;

    public static ModelingProjectDetail from(ModelingProject project) {
        ModelingProjectDetail detail = new ModelingProjectDetail();
        detail.id = project.getId();
        detail.name = project.getName();
        detail.status = project.getStatus().name();
        detail.sourceFileName = project.getSourceFileName();
        detail.sourceType = project.getSourceType();
        detail.namespace = project.getNamespace();
        detail.aiProvider = project.getAiProvider();
        detail.aiModel = project.getAiModel();
        detail.modelId = project.getModelId();
        detail.createdAt = project.getCreatedAt();
        detail.updatedAt = project.getUpdatedAt();
        detail.warnings = project.getWarnings();
        detail.error = project.getError();
        detail.statistics = ModelingStatisticsDto.from(project.getStatistics());
        return detail;
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

    public String getNamespace() {
        return namespace;
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

    public List<String> getWarnings() {
        return warnings;
    }

    public String getError() {
        return error;
    }

    public ModelingStatisticsDto getStatistics() {
        return statistics;
    }

    public Map<String, String> getFiles() {
        return files;
    }

    public void setFiles(Map<String, String> files) {
        this.files = files;
    }

    public String getExtractionPreview() {
        return extractionPreview;
    }

    public void setExtractionPreview(String extractionPreview) {
        this.extractionPreview = extractionPreview;
    }

    public String getOntologyPreview() {
        return ontologyPreview;
    }

    public void setOntologyPreview(String ontologyPreview) {
        this.ontologyPreview = ontologyPreview;
    }

    public String getInstancesPreview() {
        return instancesPreview;
    }

    public void setInstancesPreview(String instancesPreview) {
        this.instancesPreview = instancesPreview;
    }
}
