package com.example.ontology.modeling.service;

import com.example.ontology.dto.request.LoadModelRequest;
import com.example.ontology.dto.request.ReasonRequest;
import com.example.ontology.dto.response.ModelDetail;
import com.example.ontology.dto.response.ReasoningResponse;
import com.example.ontology.infrastructure.store.ModelRegistry;
import com.example.ontology.modeling.config.ModelingProperties;
import com.example.ontology.modeling.domain.AiOptions;
import com.example.ontology.modeling.domain.AiStatus;
import com.example.ontology.modeling.domain.ExtractedOntology;
import com.example.ontology.modeling.domain.ExtractionOutcome;
import com.example.ontology.modeling.domain.GeneratedOntology;
import com.example.ontology.modeling.domain.ModelingProject;
import com.example.ontology.modeling.domain.ModelingStatistics;
import com.example.ontology.modeling.domain.ModelingStatus;
import com.example.ontology.modeling.domain.SourceDocument;
import com.example.ontology.modeling.domain.exception.ModelingException;
import com.example.ontology.modeling.dto.ExtractRequest;
import com.example.ontology.modeling.dto.GenerateRequest;
import com.example.ontology.modeling.dto.LoadGeneratedRequest;
import com.example.ontology.modeling.dto.ModelingLoadResponse;
import com.example.ontology.modeling.dto.ModelingProjectDetail;
import com.example.ontology.modeling.dto.ModelingProjectSummary;
import com.example.ontology.modeling.dto.PipelineRequest;
import com.example.ontology.modeling.dto.PipelineResponse;
import com.example.ontology.modeling.infrastructure.ai.LlmClientFactory;
import com.example.ontology.modeling.infrastructure.build.OntologyTurtleBuilder;
import com.example.ontology.modeling.infrastructure.extract.AiOntologyExtractor;
import com.example.ontology.modeling.infrastructure.extract.DocumentTextExtractor;
import com.example.ontology.modeling.infrastructure.store.ModelingProjectStore;
import com.example.ontology.service.ModelService;
import com.example.ontology.service.ReasoningService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Use cases of the document-driven ontology modeling module:
 * source document &rarr; AI/heuristic extraction &rarr; T-Box + A-Box Turtle
 * &rarr; load into the reasoning model registry.
 */
@Service
public class ModelingService {

    private static final Logger log = LoggerFactory.getLogger(ModelingService.class);
    private static final int PREVIEW_LIMIT = 12000;

    private final ModelingProperties properties;
    private final ModelingProjectStore store;
    private final DocumentTextExtractor documentExtractor;
    private final AiOntologyExtractor ontologyExtractor;
    private final OntologyTurtleBuilder turtleBuilder;
    private final LlmClientFactory llmClientFactory;
    private final ObjectMapper mapper;
    private final ModelService modelService;
    private final ReasoningService reasoningService;
    private final ModelRegistry modelRegistry;

    public ModelingService(ModelingProperties properties, ModelingProjectStore store,
                           DocumentTextExtractor documentExtractor, AiOntologyExtractor ontologyExtractor,
                           OntologyTurtleBuilder turtleBuilder, LlmClientFactory llmClientFactory,
                           ObjectMapper mapper, ModelService modelService,
                           ReasoningService reasoningService, ModelRegistry modelRegistry) {
        this.properties = properties;
        this.store = store;
        this.documentExtractor = documentExtractor;
        this.ontologyExtractor = ontologyExtractor;
        this.turtleBuilder = turtleBuilder;
        this.llmClientFactory = llmClientFactory;
        this.mapper = mapper;
        this.modelService = modelService;
        this.reasoningService = reasoningService;
        this.modelRegistry = modelRegistry;
    }

    // ------------------------------------------------------------------
    // Projects
    // ------------------------------------------------------------------

    public ModelingProjectDetail create(String name, String fileName, String mimeType, byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new ModelingException("No source document given: upload a file or provide inline content");
        }
        if (bytes.length > properties.getMaxDocumentBytes()) {
            throw new ModelingException("Document is too large (" + bytes.length + " bytes, limit "
                    + properties.getMaxDocumentBytes() + ")");
        }
        SourceDocument document = documentExtractor.extract(fileName, mimeType, bytes);
        String projectId = nextProjectId();
        ModelingProject project = new ModelingProject();
        project.setId(projectId);
        project.setName(isBlank(name) ? defaultName(document.getFileName()) : name.trim());
        project.setSourceFileName(document.getFileName());
        project.setSourceType(document.getType());
        project.setStatus(ModelingStatus.CREATED);
        project.setCreatedAt(Instant.now());
        project.setUpdatedAt(Instant.now());
        ModelingStatistics statistics = new ModelingStatistics();
        statistics.setTextChars(document.getText().length());
        project.setStatistics(statistics);
        store.create(projectId, document.getFileName(), bytes, document.getText());
        project.setSourceTextFile(store.sourceTextFile(projectId));
        project.setExtractionFile(store.extractionFile(projectId));
        project.setOntologyFile(store.ontologyFile(projectId));
        project.setInstancesFile(store.instancesFile(projectId));
        store.saveMeta(project);
        log.info("Created modeling project '{}' from '{}' ({} chars)",
                projectId, document.getFileName(), document.getText().length());
        return detail(project);
    }

    public List<ModelingProjectSummary> list() {
        return store.list().stream().map(ModelingProjectSummary::from).collect(Collectors.toList());
    }

    public ModelingProjectDetail get(String projectId) {
        return detail(store.load(projectId));
    }

    public void delete(String projectId) {
        ModelingProject project = store.load(projectId);
        if (project.getModelId() != null && modelRegistry.exists(project.getModelId())) {
            try {
                modelService.delete(project.getModelId());
            } catch (RuntimeException e) {
                log.warn("Could not unload model {} of project {}: {}", project.getModelId(), projectId, e.getMessage());
            }
        }
        store.delete(projectId);
    }

    // ------------------------------------------------------------------
    // Pipeline steps
    // ------------------------------------------------------------------

    public ModelingProjectDetail extract(String projectId, ExtractRequest request) {
        ModelingProject project = store.load(projectId);
        ExtractRequest options = request == null ? new ExtractRequest() : request;
        String text = store.readText(projectId);
        int chunkSize = options.getChunkSize() == null ? properties.getChunkSize() : options.getChunkSize();
        int maxChunks = options.getMaxChunks() == null ? properties.getMaxChunks() : options.getMaxChunks();
        AiOptions aiOptions = resolveAiOptions(options);

        long started = System.currentTimeMillis();
        try {
            SourceDocument document = new SourceDocument(project.getSourceFileName(), null,
                    project.getSourceType(), text);
            ExtractionOutcome outcome = ontologyExtractor.extract(document, aiOptions, chunkSize, maxChunks);
            ExtractedOntology extraction = outcome.getOntology();

            try {
                store.writeExtraction(projectId, mapper.writerWithDefaultPrettyPrinter()
                        .writeValueAsString(extraction));
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                throw new ModelingException("Cannot serialise extraction result: " + e.getMessage(), e);
            }

            ModelingStatistics statistics = project.getStatistics();
            statistics.setChunkCount(extraction.getChunkCount());
            statistics.setClasses(extraction.getClasses().size());
            statistics.setObjectProperties(extraction.getObjectProperties().size());
            statistics.setDatatypeProperties(extraction.getDatatypeProperties().size());
            statistics.setIndividuals(extraction.getIndividuals().size());
            statistics.setRelations(extraction.getRelations().size());
            statistics.setLiterals(extraction.getLiterals().size());
            statistics.setExtractionMillis(System.currentTimeMillis() - started);

            project.setAiProvider(aiOptions.getProvider());
            project.setAiModel(llmClientFactory.isHeuristic(aiOptions)
                    ? "heuristic"
                    : aiOptions.getModel());
            project.setWarnings(new ArrayList<>(outcome.getWarnings()));
            project.setError(null);
            project.setStatus(ModelingStatus.EXTRACTED);
            store.saveMeta(project);
            log.info("Extracted ontology for project '{}': {} classes, {} individuals, provider={}",
                    projectId, statistics.getClasses(), statistics.getIndividuals(), aiOptions.getProvider());
            return detail(project);
        } catch (RuntimeException e) {
            project.setStatus(ModelingStatus.FAILED);
            project.setError(e.getMessage());
            store.saveMeta(project);
            throw e;
        }
    }

    public ModelingProjectDetail generate(String projectId, GenerateRequest request) {
        ModelingProject project = store.load(projectId);
        if (!store.extractionFile(projectId).toFile().isFile()) {
            throw new ModelingException("Project '" + projectId
                    + "' has no extraction yet; call /extract first");
        }
        GenerateRequest options = request == null ? new GenerateRequest() : request;
        ExtractedOntology extraction;
        try {
            extraction = mapper.readValue(store.readExtraction(projectId), ExtractedOntology.class);
        } catch (IOException e) {
            throw new ModelingException("Cannot read extraction of project '" + projectId + "': "
                    + e.getMessage(), e);
        }

        String namespace = properties.namespaceFor(projectId, options.getNamespace());
        boolean includeSource = options.getIncludeSourceComments() == null
                || options.getIncludeSourceComments();
        long started = System.currentTimeMillis();
        GeneratedOntology generated = turtleBuilder.build(extraction, namespace,
                project.getSourceFileName(), includeSource);
        store.writeOntology(projectId, generated.getTboxTurtle());
        store.writeInstances(projectId, generated.getAboxTurtle());

        ModelingStatistics statistics = project.getStatistics();
        statistics.setClasses(generated.getClasses());
        statistics.setObjectProperties(generated.getObjectProperties());
        statistics.setDatatypeProperties(generated.getDatatypeProperties());
        statistics.setIndividuals(generated.getIndividuals());
        statistics.setTboxTriples(generated.getTboxTriples());
        statistics.setAboxTriples(generated.getAboxTriples());
        statistics.setGenerationMillis(System.currentTimeMillis() - started);

        project.setNamespace(namespace);
        project.setStatus(ModelingStatus.GENERATED);
        project.setError(null);
        store.saveMeta(project);
        log.info("Generated ontology for project '{}': T-Box {} triples, A-Box {} triples",
                projectId, generated.getTboxTriples(), generated.getAboxTriples());
        return detail(project);
    }

    public ModelingLoadResponse load(String projectId, LoadGeneratedRequest request) {
        ModelingProject project = store.load(projectId);
        if (!store.ontologyFile(projectId).toFile().isFile()
                || !store.instancesFile(projectId).toFile().isFile()) {
            throw new ModelingException("Project '" + projectId
                    + "' has no generated ontology yet; call /generate first");
        }
        LoadGeneratedRequest options = request == null ? new LoadGeneratedRequest() : request;
        String modelId = isBlank(options.getModelId())
                ? "modeling-" + projectId
                : options.getModelId().trim();
        if (modelRegistry.exists(modelId)) {
            boolean replace = options.getReplace() == null || options.getReplace();
            if (!replace) {
                throw new ModelingException("Reasoning model '" + modelId + "' already exists; "
                        + "choose another modelId or set replace=true");
            }
            modelService.delete(modelId);
        }

        LoadModelRequest loadRequest = new LoadModelRequest();
        loadRequest.setModelId(modelId);
        loadRequest.setName(isBlank(options.getName())
                ? (project.getName() + " (生成)")
                : options.getName().trim());
        loadRequest.setDescription("Generated from document '" + project.getSourceFileName()
                + "' by the ontology modeling module");
        loadRequest.setOntologyPath(store.ontologyFile(projectId).toAbsolutePath().toString());
        loadRequest.setInstancesPath(store.instancesFile(projectId).toAbsolutePath().toString());

        ModelDetail model = modelService.load(loadRequest);
        project.setModelId(modelId);
        project.setStatus(ModelingStatus.LOADED);
        project.setError(null);
        store.saveMeta(project);
        log.info("Loaded generated ontology of project '{}' as reasoning model '{}'", projectId, modelId);

        ReasoningResponse reasoning = null;
        boolean autoReason = options.getAutoReason() == null || options.getAutoReason();
        if (autoReason) {
            try {
                ReasonRequest reasonRequest = new ReasonRequest();
                reasonRequest.setModelId(modelId);
                reasonRequest.setMaterialize(false);
                reasoning = reasoningService.reason(reasonRequest);
            } catch (RuntimeException e) {
                project.getWarnings().add("Model loaded but automatic reasoning failed: " + e.getMessage());
                store.saveMeta(project);
                log.warn("Automatic reasoning failed for generated model {}: {}", modelId, e.getMessage());
            }
        }
        return new ModelingLoadResponse(detail(project), model, reasoning);
    }

    public PipelineResponse pipeline(String projectId, PipelineRequest request) {
        PipelineRequest options = request == null ? new PipelineRequest() : request;
        extract(projectId, options.getExtract());
        generate(projectId, options.getGenerate());
        ModelingLoadResponse loaded = load(projectId, options.getLoad());
        return new PipelineResponse(loaded.getProject(), loaded);
    }

    public AiStatus aiStatus() {
        AiOptions options = new AiOptions();
        ModelingProperties.Ai defaults = properties.getAi();
        options.setProvider(defaults.getProvider());
        options.setBaseUrl(defaults.getBaseUrl());
        options.setModel(defaults.getModel());
        options.setApiKey(defaults.getApiKey());
        options.setTimeoutSeconds(defaults.getTimeoutSeconds());
        AiStatus status = llmClientFactory.status(options);
        return status;
    }

    // ------------------------------------------------------------------
    // Content access
    // ------------------------------------------------------------------

    public String readOntology(String projectId) {
        store.load(projectId);
        return store.readOntology(projectId);
    }

    public String readInstances(String projectId) {
        store.load(projectId);
        return store.readInstances(projectId);
    }

    public String readExtraction(String projectId) {
        store.load(projectId);
        return store.readExtraction(projectId);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private AiOptions resolveAiOptions(ExtractRequest request) {
        ModelingProperties.Ai defaults = properties.getAi();
        AiOptions options = new AiOptions();
        options.setProvider(firstNonBlank(request.getProvider(), defaults.getProvider()));
        validateProvider(options.getProvider());
        options.setBaseUrl(firstNonBlank(request.getBaseUrl(), defaults.getBaseUrl()));
        options.setModel(firstNonBlank(request.getModel(), defaults.getModel()));
        options.setApiKey(firstNonBlank(request.getApiKey(), defaults.getApiKey()));
        options.setTemperature(request.getTemperature() != null
                ? request.getTemperature() : defaults.getTemperature());
        options.setFallbackToHeuristic(request.getFallbackToHeuristic() != null
                ? request.getFallbackToHeuristic() : defaults.isFallbackToHeuristic());
        options.setTimeoutSeconds(request.getTimeoutSeconds() != null
                ? request.getTimeoutSeconds() : defaults.getTimeoutSeconds());
        return options;
    }

    private ModelingProjectDetail detail(ModelingProject project) {
        ModelingProjectDetail detail = ModelingProjectDetail.from(project);
        Map<String, String> files = new LinkedHashMap<>();
        files.put("sourceText", absolute(project.getSourceTextFile()));
        files.put("extraction", absolute(project.getExtractionFile()));
        files.put("ontology", absolute(project.getOntologyFile()));
        files.put("instances", absolute(project.getInstancesFile()));
        detail.setFiles(files);
        detail.setExtractionPreview(preview(project.getExtractionFile()));
        detail.setOntologyPreview(preview(project.getOntologyFile()));
        detail.setInstancesPreview(preview(project.getInstancesFile()));
        return detail;
    }

    private static String absolute(Path path) {
        return path == null ? null : path.toAbsolutePath().toString();
    }

    private static String preview(Path file) {
        if (file == null || !Files.isRegularFile(file)) {
            return null;
        }
        try {
            String content = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
            if (content.length() <= PREVIEW_LIMIT) {
                return content;
            }
            return content.substring(0, PREVIEW_LIMIT) + "\n\n... (preview truncated, use the download endpoint)";
        } catch (IOException e) {
            return null;
        }
    }

    private static String nextProjectId() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        return "p-" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(java.time.ZoneOffset.UTC)
                .format(Instant.now()) + "-" + suffix;
    }

    private static String defaultName(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return "未命名建模项目";
        }
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }

    private static void validateProvider(String provider) {
        String value = provider == null ? "" : provider.trim().toLowerCase();
        switch (value) {
            case "ollama":
            case "openai":
            case "heuristic":
            case "mock":
                return;
            default:
                throw new ModelingException("Unknown AI provider '" + provider
                        + "' (expected ollama, openai, heuristic or mock)");
        }
    }

    private static String firstNonBlank(String first, String fallback) {
        return first == null || first.trim().isEmpty() ? fallback : first.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
