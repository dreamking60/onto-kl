package com.example.ontology.domain.exception;

/** The requested ontology/mapping/parameters are invalid (HTTP 400). */
public class InvalidModelException extends OntologyException {

    public InvalidModelException(String message) {
        super(message);
    }

    public InvalidModelException(String message, Throwable cause) {
        super(message, cause);
    }
}
