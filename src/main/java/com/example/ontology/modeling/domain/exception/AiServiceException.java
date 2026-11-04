package com.example.ontology.modeling.domain.exception;

import com.example.ontology.domain.exception.OntologyException;

/** The configured AI backend is unreachable or returned an unusable answer (HTTP 502). */
public class AiServiceException extends OntologyException {

    public AiServiceException(String message) {
        super(message);
    }

    public AiServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
