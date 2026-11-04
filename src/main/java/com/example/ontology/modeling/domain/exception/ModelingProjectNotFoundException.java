package com.example.ontology.modeling.domain.exception;

import com.example.ontology.domain.exception.OntologyException;

public class ModelingProjectNotFoundException extends OntologyException {

    public ModelingProjectNotFoundException(String projectId) {
        super("Ontology modeling project not found: " + projectId);
    }
}
