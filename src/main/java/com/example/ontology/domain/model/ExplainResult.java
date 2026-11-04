package com.example.ontology.domain.model;

import java.util.Set;

/** Result of explaining the inferred statements of one individual. */
public final class ExplainResult {

    private final String modelId;
    private final String subject;
    private final Set<String> types;
    private final String trace;
    private final int inferredStatements;

    public ExplainResult(String modelId, String subject, Set<String> types,
                         String trace, int inferredStatements) {
        this.modelId = modelId;
        this.subject = subject;
        this.types = types;
        this.trace = trace;
        this.inferredStatements = inferredStatements;
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

    public String getTrace() {
        return trace;
    }

    public int getInferredStatements() {
        return inferredStatements;
    }
}
