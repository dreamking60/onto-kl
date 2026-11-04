package com.example.ontology.domain.exception;

/** Loading a model failed because of the database / Ontop layer (HTTP 502). */
public class ModelLoadException extends OntologyException {

    public ModelLoadException(String message) {
        super(message);
    }

    public ModelLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
