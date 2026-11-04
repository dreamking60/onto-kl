package com.example.ontology.domain.exception;

/** Base class for all business exceptions of the ontology server. */
public class OntologyException extends RuntimeException {

    public OntologyException(String message) {
        super(message);
    }

    public OntologyException(String message, Throwable cause) {
        super(message, cause);
    }
}
