package com.example.ontology.domain.exception;

/** Computing the inference closure failed (HTTP 500). */
public class ReasoningException extends OntologyException {

    public ReasoningException(String message) {
        super(message);
    }

    public ReasoningException(String message, Throwable cause) {
        super(message, cause);
    }
}
