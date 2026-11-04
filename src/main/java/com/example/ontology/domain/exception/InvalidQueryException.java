package com.example.ontology.domain.exception;

/** Invalid SPARQL query (HTTP 400). */
public class InvalidQueryException extends OntologyException {

    public InvalidQueryException(String message) {
        super(message);
    }

    public InvalidQueryException(String message, Throwable cause) {
        super(message, cause);
    }
}
