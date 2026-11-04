package com.example.ontology.controller;

import com.example.ontology.dto.request.LoadModelRequest;
import com.example.ontology.dto.response.ModelDetail;
import com.example.ontology.dto.response.ModelSummary;
import com.example.ontology.service.ModelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * Model lifecycle endpoints: load a model, list loaded models, inspect one
 * model and unload it.
 */
@RestController
@Tag(name = "Models", description = "Load, inspect and unload ontology models")
public class ModelController {

    private final ModelService modelService;

    public ModelController(ModelService modelService) {
        this.modelService = modelService;
    }

    @Operation(summary = "Load an ontology model",
            description = "Registers a model: local ontology (T-Box) + OBDA mapping + business rules, "
                    + "connected to a TDSQL/MySQL (or embedded H2) A-Box. "
                    + "Without sources the demo ontology under ontology/ and mapping/ is used.")
    @PostMapping(path = "/load", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ModelDetail load(@Valid @RequestBody(required = false) LoadModelRequest request) {
        return modelService.load(request == null ? new LoadModelRequest() : request);
    }

    @Operation(summary = "List loaded models")
    @GetMapping(path = "/models", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ModelSummary> list() {
        return modelService.list();
    }

    @Operation(summary = "Get one loaded model")
    @GetMapping(path = "/models/{modelId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ModelDetail get(@PathVariable String modelId) {
        return modelService.get(modelId);
    }

    @Operation(summary = "Unload a model",
            description = "Closes the Ontop/Jena runtime of the model and deletes its local files.")
    @DeleteMapping(path = "/models/{modelId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String modelId) {
        modelService.delete(modelId);
    }
}
