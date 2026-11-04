package com.example.ontology.controller;

import com.example.ontology.dto.request.ReasonRequest;
import com.example.ontology.dto.response.ExplainResponse;
import com.example.ontology.dto.response.ReasoningResponse;
import com.example.ontology.service.ReasoningService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

/** Reasoning endpoints: run the reasoner and explain inferred statements. */
@RestController
@Validated
@Tag(name = "Reasoning", description = "Compute the inference closure and explain derivations")
public class ReasoningController {

    private final ReasoningService reasoningService;

    public ReasoningController(ReasoningService reasoningService) {
        this.reasoningService = reasoningService;
    }

    @Operation(summary = "Reason over a loaded model",
            description = "Runs the business rules and the OWL entailment, optionally persists the "
                    + "derived triples into the A-Box store and returns a sample of the closure.")
    @PostMapping(path = "/reason", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ReasoningResponse reason(@Valid @RequestBody ReasonRequest request) {
        return reasoningService.reason(request);
    }

    @Operation(summary = "Explain inferred statements of an individual",
            description = "Returns derivation traces (business rule / OWL rule + premises) for the "
                    + "statements about one individual, e.g. subject=:emp/5")
    @GetMapping(path = "/models/{modelId}/explain", produces = MediaType.APPLICATION_JSON_VALUE)
    public ExplainResponse explain(@PathVariable String modelId,
                                   @RequestParam String subject,
                                   @RequestParam(defaultValue = "50")
                                   @Min(1) @Max(500) int limit) {
        return reasoningService.explain(modelId, subject, limit);
    }
}
