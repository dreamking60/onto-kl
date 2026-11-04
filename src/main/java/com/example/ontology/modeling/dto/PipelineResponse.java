package com.example.ontology.modeling.dto;

/** Response of {@code POST /modeling/projects/{id}/pipeline}. */
public class PipelineResponse {

    private ModelingProjectDetail project;
    private ModelingLoadResponse loaded;

    public PipelineResponse(ModelingProjectDetail project, ModelingLoadResponse loaded) {
        this.project = project;
        this.loaded = loaded;
    }

    public ModelingProjectDetail getProject() {
        return project;
    }

    public ModelingLoadResponse getLoaded() {
        return loaded;
    }
}
