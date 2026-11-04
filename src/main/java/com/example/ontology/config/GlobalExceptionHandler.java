package com.example.ontology.config;

import com.example.ontology.domain.exception.DuplicateModelException;
import com.example.ontology.domain.exception.InvalidModelException;
import com.example.ontology.domain.exception.InvalidQueryException;
import com.example.ontology.domain.exception.ModelLoadException;
import com.example.ontology.domain.exception.ModelNotFoundException;
import com.example.ontology.domain.exception.OntologyException;
import com.example.ontology.domain.exception.ReasoningException;
import com.example.ontology.dto.response.ErrorResponse;
import com.example.ontology.modeling.domain.exception.AiServiceException;
import com.example.ontology.modeling.domain.exception.ModelingException;
import com.example.ontology.modeling.domain.exception.ModelingProjectNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Central exception handler: every error leaves the REST API as a uniform
 * {@link ErrorResponse} JSON document with an HTTP status that reflects the
 * failure category.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ModelingProjectNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleModelingProjectNotFound(ModelingProjectNotFoundException e,
                                                                      HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "MODELING_PROJECT_NOT_FOUND", e.getMessage(), request, null);
    }

    @ExceptionHandler(AiServiceException.class)
    public ResponseEntity<ErrorResponse> handleAiService(AiServiceException e, HttpServletRequest request) {
        return build(HttpStatus.BAD_GATEWAY, "AI_SERVICE_ERROR", e.getMessage(), request, null);
    }

    @ExceptionHandler(ModelingException.class)
    public ResponseEntity<ErrorResponse> handleModeling(ModelingException e, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "MODELING_ERROR", e.getMessage(), request, null);
    }

    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleUploadTooLarge(
            org.springframework.web.multipart.MaxUploadSizeExceededException e, HttpServletRequest request) {
        return build(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE",
                "Uploaded document exceeds the configured multipart limit", request, null);
    }

    @ExceptionHandler(ModelNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ModelNotFoundException e, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "MODEL_NOT_FOUND", e.getMessage(), request, null);
    }

    @ExceptionHandler(DuplicateModelException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(DuplicateModelException e, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "MODEL_ALREADY_EXISTS", e.getMessage(), request, null);
    }

    @ExceptionHandler(InvalidModelException.class)
    public ResponseEntity<ErrorResponse> handleInvalidModel(InvalidModelException e, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "INVALID_MODEL", e.getMessage(), request, null);
    }

    @ExceptionHandler(InvalidQueryException.class)
    public ResponseEntity<ErrorResponse> handleInvalidQuery(InvalidQueryException e, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "INVALID_QUERY", e.getMessage(), request, null);
    }

    @ExceptionHandler(ModelLoadException.class)
    public ResponseEntity<ErrorResponse> handleLoadFailure(ModelLoadException e, HttpServletRequest request) {
        log.warn("Model load failure: {}", e.getMessage());
        return build(HttpStatus.BAD_GATEWAY, "MODEL_LOAD_FAILED", e.getMessage(), request, null);
    }

    @ExceptionHandler(ReasoningException.class)
    public ResponseEntity<ErrorResponse> handleReasoningFailure(ReasoningException e, HttpServletRequest request) {
        log.error("Reasoning failure", e);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "REASONING_FAILED", e.getMessage(), request, null);
    }

    @ExceptionHandler(OntologyException.class)
    public ResponseEntity<ErrorResponse> handleOntologyException(OntologyException e, HttpServletRequest request) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "ONTOLOGY_ERROR", e.getMessage(), request, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e,
                                                          HttpServletRequest request) {
        Map<String, String> details = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(error -> details.put(error.getField(), error.getDefaultMessage()));
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
                "Request validation failed", request, details);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException e,
                                                                   HttpServletRequest request) {
        Map<String, String> details = e.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(),
                        ConstraintViolation::getMessage,
                        (first, second) -> first,
                        LinkedHashMap::new));
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
                "Request validation failed", request, details);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            IllegalArgumentException.class})
    public ResponseEntity<ErrorResponse> handleBadRequest(Exception e, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", e.getMessage(), request, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e, HttpServletRequest request) {
        log.error("Unexpected error while serving " + request.getMethod() + " " + request.getRequestURI(), e);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "Unexpected server error: " + e.getClass().getSimpleName(), request, null);
    }

    private static ResponseEntity<ErrorResponse> build(HttpStatus status, String code, String message,
                                                       HttpServletRequest request, Object details) {
        ErrorResponse body = ErrorResponse.of(status.value(), status.getReasonPhrase(), code,
                message, request.getRequestURI(), details);
        return ResponseEntity.status(status).body(body);
    }
}
