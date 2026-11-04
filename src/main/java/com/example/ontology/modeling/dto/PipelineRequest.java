package com.example.ontology.modeling.dto;

/** Body of {@code POST /modeling/projects/{id}/pipeline}: extract + generate + load. */
public class PipelineRequest {

    private ExtractRequest extract;
    private GenerateRequest generate;
    private LoadGeneratedRequest load;

    public ExtractRequest getExtract() {
        return extract;
    }

    public void setExtract(ExtractRequest extract) {
        this.extract = extract;
    }

    public GenerateRequest getGenerate() {
        return generate;
    }

    public void setGenerate(GenerateRequest generate) {
        this.generate = generate;
    }

    public LoadGeneratedRequest getLoad() {
        return load;
    }

    public void setLoad(LoadGeneratedRequest load) {
        this.load = load;
    }
}
