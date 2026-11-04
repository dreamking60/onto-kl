package com.example.ontology.modeling.dto;

/** Options of {@code POST /modeling/projects/{id}/generate}. */
public class GenerateRequest {

    /** Optional explicit namespace, e.g. http://company.org/ontology/contract# */
    private String namespace;
    private Boolean includeSourceComments;

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public Boolean getIncludeSourceComments() {
        return includeSourceComments;
    }

    public void setIncludeSourceComments(Boolean includeSourceComments) {
        this.includeSourceComments = includeSourceComments;
    }
}
