package com.example.ontology.modeling.dto;

/** Options of {@code POST /modeling/projects/{id}/load}. */
public class LoadGeneratedRequest {

    private String modelId;
    private String name;
    private String description;
    private Boolean autoReason;
    private Boolean replace;

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getAutoReason() {
        return autoReason;
    }

    public void setAutoReason(Boolean autoReason) {
        this.autoReason = autoReason;
    }

    public Boolean getReplace() {
        return replace;
    }

    public void setReplace(Boolean replace) {
        this.replace = replace;
    }
}
