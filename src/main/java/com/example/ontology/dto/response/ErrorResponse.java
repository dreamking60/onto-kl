package com.example.ontology.dto.response;

import java.time.Instant;

/** Uniform error body returned by the global exception handler. */
public class ErrorResponse {

    private Instant timestamp;
    private int status;
    private String error;
    private String code;
    private String message;
    private String path;
    private Object details;

    public static ErrorResponse of(int status, String error, String code, String message, String path) {
        return of(status, error, code, message, path, null);
    }

    public static ErrorResponse of(int status, String error, String code, String message,
                                   String path, Object details) {
        ErrorResponse response = new ErrorResponse();
        response.timestamp = Instant.now();
        response.status = status;
        response.error = error;
        response.code = code;
        response.message = message;
        response.path = path;
        response.details = details;
        return response;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public int getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public String getPath() {
        return path;
    }

    public Object getDetails() {
        return details;
    }
}
