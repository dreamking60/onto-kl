package com.example.ontology.modeling.controller;

import com.example.ontology.modeling.domain.exception.ModelingException;
import com.example.ontology.modeling.dto.AiStatusDto;
import com.example.ontology.modeling.dto.CreateProjectRequest;
import com.example.ontology.modeling.dto.ExtractRequest;
import com.example.ontology.modeling.dto.GenerateRequest;
import com.example.ontology.modeling.dto.LoadGeneratedRequest;
import com.example.ontology.modeling.dto.ModelingLoadResponse;
import com.example.ontology.modeling.dto.ModelingProjectDetail;
import com.example.ontology.modeling.dto.ModelingProjectSummary;
import com.example.ontology.modeling.dto.PipelineRequest;
import com.example.ontology.modeling.dto.PipelineResponse;
import com.example.ontology.modeling.service.ModelingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Document-driven ontology modeling API:
 * upload a source document, extract a T-Box + A-Box with an AI model (or the
 * module's heuristic extractor), then load the generated ontology as a normal
 * reasoning model.
 */
@RestController
@RequestMapping("/modeling")
@Tag(name = "Ontology Modeling",
        description = "Build T-Box + A-Box documents from laws / manuals / books and load them into the reasoning model registry")
public class ModelingController {

    private final ModelingService modelingService;

    public ModelingController(ModelingService modelingService) {
        this.modelingService = modelingService;
    }

    @Operation(summary = "Create a modeling project from inline text")
    @PostMapping(path = "/projects", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ModelingProjectDetail create(@Valid @RequestBody CreateProjectRequest request) {
        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new ModelingException("Field 'content' is required (or use POST /modeling/projects/upload)");
        }
        String fileName = request.getFileName() == null || request.getFileName().trim().isEmpty()
                ? "source.txt" : request.getFileName().trim();
        return modelingService.create(request.getName(), fileName, request.getMimeType(),
                request.getContent().getBytes(StandardCharsets.UTF_8));
    }

    @Operation(summary = "Create a modeling project from an uploaded file",
            description = "Supported: txt, md, html, pdf, docx")
    @PostMapping(path = "/projects/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ModelingProjectDetail upload(@RequestParam(value = "file", required = false) MultipartFile file,
                                        @RequestParam(value = "name", required = false) String name) {
        if (file == null || file.isEmpty()) {
            throw new ModelingException("Multipart field 'file' is required");
        }
        try {
            return modelingService.create(name, file.getOriginalFilename(), file.getContentType(), file.getBytes());
        } catch (IOException e) {
            throw new ModelingException("Cannot read uploaded file: " + e.getMessage(), e);
        }
    }

    @Operation(summary = "List modeling projects")
    @GetMapping(path = "/projects", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ModelingProjectSummary> list() {
        return modelingService.list();
    }

    @Operation(summary = "Get one modeling project")
    @GetMapping(path = "/projects/{projectId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ModelingProjectDetail get(@PathVariable String projectId) {
        return modelingService.get(projectId);
    }

    @Operation(summary = "Delete a modeling project (and unload its reasoning model)")
    @DeleteMapping(path = "/projects/{projectId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String projectId) {
        modelingService.delete(projectId);
    }

    @Operation(summary = "Extract ontology candidates from the source document",
            description = "provider = ollama (default) | openai | heuristic | mock; "
                    + "all AI options are optional and default to app.modeling.ai.*")
    @PostMapping(path = "/projects/{projectId}/extract", produces = MediaType.APPLICATION_JSON_VALUE)
    public ModelingProjectDetail extract(@PathVariable String projectId,
                                         @RequestBody(required = false) ExtractRequest request) {
        return modelingService.extract(projectId, request);
    }

    @Operation(summary = "Generate the T-Box and A-Box Turtle documents from the extraction")
    @PostMapping(path = "/projects/{projectId}/generate", produces = MediaType.APPLICATION_JSON_VALUE)
    public ModelingProjectDetail generate(@PathVariable String projectId,
                                          @RequestBody(required = false) GenerateRequest request) {
        return modelingService.generate(projectId, request);
    }

    @Operation(summary = "Load the generated ontology as a reasoning model",
            description = "The generated model appears in GET /models and can be reasoned/queries like any other model.")
    @PostMapping(path = "/projects/{projectId}/load", produces = MediaType.APPLICATION_JSON_VALUE)
    public ModelingLoadResponse load(@PathVariable String projectId,
                                     @RequestBody(required = false) LoadGeneratedRequest request) {
        return modelingService.load(projectId, request);
    }

    @Operation(summary = "Run extract + generate + load in one call")
    @PostMapping(path = "/projects/{projectId}/pipeline", produces = MediaType.APPLICATION_JSON_VALUE)
    public PipelineResponse pipeline(@PathVariable String projectId,
                                     @RequestBody(required = false) PipelineRequest request) {
        return modelingService.pipeline(projectId, request);
    }

    @Operation(summary = "Download the generated T-Box (Turtle)")
    @GetMapping(path = "/projects/{projectId}/ontology", produces = "text/turtle;charset=UTF-8")
    public ResponseEntity<String> ontology(@PathVariable String projectId) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/turtle;charset=UTF-8"))
                .body(modelingService.readOntology(projectId));
    }

    @Operation(summary = "Download the generated A-Box (Turtle)")
    @GetMapping(path = "/projects/{projectId}/instances", produces = "text/turtle;charset=UTF-8")
    public ResponseEntity<String> instances(@PathVariable String projectId) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/turtle;charset=UTF-8"))
                .body(modelingService.readInstances(projectId));
    }

    @Operation(summary = "Download the extraction result (JSON)")
    @GetMapping(path = "/projects/{projectId}/extraction", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> extraction(@PathVariable String projectId) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(modelingService.readExtraction(projectId));
    }

    @Operation(summary = "AI backend configuration and reachability",
            description = "For Ollama shows the installed models; API keys are never returned.")
    @GetMapping(path = "/ai/status", produces = MediaType.APPLICATION_JSON_VALUE)
    public AiStatusDto aiStatus() {
        return AiStatusDto.from(modelingService.aiStatus());
    }
}
