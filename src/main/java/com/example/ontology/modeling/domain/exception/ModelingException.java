package com.example.ontology.modeling.domain.exception;

import com.example.ontology.domain.exception.OntologyException;

/** Generic modeling-module error (HTTP 400). */
public class ModelingException extends OntologyException {

    public ModelingException(String message) {
        super(message);
    }

    public ModelingException(String message, Throwable cause) {
        super(message, cause);
    }
}
