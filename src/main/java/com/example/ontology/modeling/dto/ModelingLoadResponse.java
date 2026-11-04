package com.example.ontology.modeling.dto;

import com.example.ontology.dto.response.ModelDetail;
import com.example.ontology.dto.response.ReasoningResponse;

/** Response of loading a generated ontology into the reasoning model registry. */
public class ModelingLoadResponse {

    private ModelingProjectDetail project;
    private ModelDetail model;
    private ReasoningResponse reasoning;

    public ModelingLoadResponse(ModelingProjectDetail project, ModelDetail model, ReasoningResponse reasoning) {
        this.project = project;
        this.model = model;
        this.reasoning = reasoning;
    }

    public ModelingProjectDetail getProject() {
        return project;
    }

    public ModelDetail getModel() {
        return model;
    }

    public ReasoningResponse getReasoning() {
        return reasoning;
    }
}
