package com.example.ontology.domain.exception;

public class ModelNotFoundException extends OntologyException {

    public ModelNotFoundException(String modelId) {
        super("Ontology model not found: " + modelId);
    }
}
