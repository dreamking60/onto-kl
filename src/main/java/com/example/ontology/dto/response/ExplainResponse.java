package com.example.ontology.dto.response;

import com.example.ontology.domain.model.ExplainResult;

import java.util.Set;

/** Response of {@code GET /models/{modelId}/explain}. */
public class ExplainResponse {

    private String modelId;
    private String subject;
    private Set<String> types;
    private int inferredStatements;
    private String trace;

    public static ExplainResponse from(ExplainResult result) {
        ExplainResponse response = new ExplainResponse();
        response.modelId = result.getModelId();
        response.subject = result.getSubject();
        response.types = result.getTypes();
        response.inferredStatements = result.getInferredStatements();
        response.trace = result.getTrace();
        return response;
    }

    public String getModelId() {
        return modelId;
    }

    public String getSubject() {
        return subject;
    }

    public Set<String> getTypes() {
        return types;
    }

    public int getInferredStatements() {
        return inferredStatements;
    }

    public String getTrace() {
        return trace;
    }
}
