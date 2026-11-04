package com.example.ontology.modeling.domain;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** One document-driven ontology modeling project. */
public class ModelingProject {

    private String id;
    private String name;
    private String sourceFileName;
    private String sourceType;
    private ModelingStatus status = ModelingStatus.CREATED;
    private String namespace;
    private Path sourceTextFile;
    private Path extractionFile;
    private Path ontologyFile;
    private Path instancesFile;
    private String aiProvider;
    private String aiModel;
    private String modelId;
    private Instant createdAt;
    private Instant updatedAt;
    private List<String> warnings = new ArrayList<>();
    private ModelingStatistics statistics = new ModelingStatistics();
    private String error;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSourceFileName() {
        return sourceFileName;
    }

    public void setSourceFileName(String sourceFileName) {
        this.sourceFileName = sourceFileName;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public ModelingStatus getStatus() {
        return status;
    }

    public void setStatus(ModelingStatus status) {
        this.status = status;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public Path getSourceTextFile() {
        return sourceTextFile;
    }

    public void setSourceTextFile(Path sourceTextFile) {
        this.sourceTextFile = sourceTextFile;
    }

    public Path getExtractionFile() {
        return extractionFile;
    }

    public void setExtractionFile(Path extractionFile) {
        this.extractionFile = extractionFile;
    }

    public Path getOntologyFile() {
        return ontologyFile;
    }

    public void setOntologyFile(Path ontologyFile) {
        this.ontologyFile = ontologyFile;
    }

    public Path getInstancesFile() {
        return instancesFile;
    }

    public void setInstancesFile(Path instancesFile) {
        this.instancesFile = instancesFile;
    }

    public String getAiProvider() {
        return aiProvider;
    }

    public void setAiProvider(String aiProvider) {
        this.aiProvider = aiProvider;
    }

    public String getAiModel() {
        return aiModel;
    }

    public void setAiModel(String aiModel) {
        this.aiModel = aiModel;
    }

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }

    public ModelingStatistics getStatistics() {
        return statistics;
    }

    public void setStatistics(ModelingStatistics statistics) {
        this.statistics = statistics;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
