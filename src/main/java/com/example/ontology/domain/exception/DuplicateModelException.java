package com.example.ontology.domain.exception;

public class DuplicateModelException extends OntologyException {

    public DuplicateModelException(String modelId) {
        super("Ontology model already loaded: " + modelId
                + " (delete it first or choose another modelId)");
    }
}
