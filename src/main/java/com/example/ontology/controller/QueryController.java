package com.example.ontology.controller;

import com.example.ontology.dto.request.QueryRequest;
import com.example.ontology.dto.response.QueryResponse;
import com.example.ontology.service.QueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;

/** SPARQL query endpoint over the inference closure of a loaded model. */
@RestController
@Validated
@Tag(name = "Query", description = "SPARQL over the inference closure")
public class QueryController {

    private final QueryService queryService;

    public QueryController(QueryService queryService) {
        this.queryService = queryService;
    }

    @Operation(summary = "Run a SPARQL query (JSON body)",
            description = "SELECT / ASK / CONSTRUCT / DESCRIBE over asserted + inferred triples. "
                    + "The common prefixes (:, rdf:, rdfs:, owl:, xsd:) are pre-bound.")
    @PostMapping(path = "/query", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public QueryResponse query(@Valid @RequestBody QueryRequest request) {
        return queryService.query(request.getModelId(), request.getSparql(), request.getLimit());
    }

    @Operation(summary = "Run a SPARQL query (query parameters)")
    @GetMapping(path = "/query", produces = MediaType.APPLICATION_JSON_VALUE)
    public QueryResponse queryGet(@RequestParam @NotBlank String modelId,
                                  @RequestParam @NotBlank String sparql,
                                  @RequestParam(required = false) Integer limit) {
        return queryService.query(modelId, sparql, limit);
    }
}
