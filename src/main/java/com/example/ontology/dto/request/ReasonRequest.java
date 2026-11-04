package com.example.ontology.dto.request;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

/** Request body of {@code POST /reason}. */
public class ReasonRequest {

    @NotBlank(message = "modelId is required")
    private String modelId;

    /** Persist the inferred closure into the relational store; null = application default. */
    private Boolean materialize;

    /** Maximum number of inferred triples returned in the response. */
    @Min(0)
    @Max(5000)
    private Integer inferredLimit;

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }

    public Boolean getMaterialize() {
        return materialize;
    }

    public void setMaterialize(Boolean materialize) {
        this.materialize = materialize;
    }

    public Integer getInferredLimit() {
        return inferredLimit;
    }

    public void setInferredLimit(Integer inferredLimit) {
        this.inferredLimit = inferredLimit;
    }
}
