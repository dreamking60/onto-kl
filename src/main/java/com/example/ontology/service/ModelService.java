package com.example.ontology.service;

import com.example.ontology.config.AppProperties;
import com.example.ontology.domain.exception.DuplicateModelException;
import com.example.ontology.domain.exception.InvalidModelException;
import com.example.ontology.domain.model.DbConfig;
import com.example.ontology.dto.request.LoadModelRequest;
import com.example.ontology.dto.response.ModelDetail;
import com.example.ontology.dto.response.ModelSummary;
import com.example.ontology.infrastructure.loader.DbConfigResolver;
import com.example.ontology.infrastructure.loader.ModelLoader;
import com.example.ontology.infrastructure.store.ModelContext;
import com.example.ontology.infrastructure.store.ModelFileStore;
import com.example.ontology.infrastructure.store.ModelRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Use cases behind {@code /load} and {@code /models}: load an ontology model
 * (local T-Box + OBDA mapping + relational A-Box) into the server and expose
 * the registry.
 */
@Service
public class ModelService {

    private static final Logger log = LoggerFactory.getLogger(ModelService.class);
    private static final Pattern MODEL_ID = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9_-]{0,63}$");

    private final AppProperties properties;
    private final ModelFileStore fileStore;
    private final ModelRegistry registry;
    private final ModelLoader loader;
    private final DbConfigResolver dbConfigResolver;

    public ModelService(AppProperties properties, ModelFileStore fileStore, ModelRegistry registry,
                        ModelLoader loader, DbConfigResolver dbConfigResolver) {
        this.properties = properties;
        this.fileStore = fileStore;
        this.registry = registry;
        this.loader = loader;
        this.dbConfigResolver = dbConfigResolver;
    }

    /** Loads a model. Synchronized because a load boots Ontop/Jena and writes local files. */
    public synchronized ModelDetail load(LoadModelRequest request) {
        String modelId = resolveModelId(request.getModelId());
        if (registry.exists(modelId)) {
            throw new DuplicateModelException(modelId);
        }

        byte[] ontology = resolveRequiredSource("ontology",
                request.getOntologyPath(), request.getOntologyContent(),
                properties.getDefaults().getOntologyPath());
        byte[] instances = resolveOptionalSource(
                request.getInstancesPath(), request.getInstancesContent(), null);
        // The default OBDA mapping is only used for the built-in demo model;
        // an upload with a local A-Box file must not silently fall back to it.
        byte[] mapping = resolveSource("mapping",
                request.getMappingPath(), request.getMappingContent(),
                instances == null ? properties.getDefaults().getMappingPath() : null);
        if (mapping == null && instances == null) {
            throw new InvalidModelException("A model needs either an OBDA mapping "
                    + "(mappingPath/mappingContent, A-Box in TDSQL) or a local A-Box file "
                    + "(instancesPath/instancesContent)");
        }
        // The demo business rules are only meaningful for the OBDA demo model;
        // generated/file-based A-Box models must not silently inherit them.
        byte[] rules = resolveOptionalSource(
                request.getRulesPath(), request.getRulesContent(),
                instances == null ? properties.getDefaults().getRulesPath() : null);

        fileStore.store(modelId, ontology, mapping, rules, instances);

        DbConfig dbConfig = mapping == null ? null : dbConfigResolver.resolve(modelId, request.getProfile(),
                request.getJdbcUrl(), request.getJdbcUsername(),
                request.getJdbcPassword(), request.getJdbcDriver());
        boolean initDatabase = Boolean.TRUE.equals(request.getInitDatabase());
        String name = isBlank(request.getName()) ? modelId : request.getName().trim();

        log.info("Loading ontology model '{}' (profile={}, db={}, aboxSource={})", modelId,
                dbConfig == null ? "local" : dbConfig.getProfile(),
                dbConfig == null ? "-" : dbConfig.getUrl(),
                mapping == null ? "local" : (instances == null ? "tdsql" : "mixed"));
        try {
            ModelContext context = loader.load(new ModelLoader.LoadSpec(
                    modelId,
                    name,
                    request.getDescription(),
                    fileStore.ontologyFile(modelId),
                    mapping == null ? null : fileStore.mappingFile(modelId),
                    rules == null ? null : fileStore.rulesFile(modelId),
                    instances == null ? null : fileStore.instancesFile(modelId),
                    dbConfig,
                    initDatabase));
            registry.register(context);
            log.info("Ontology model '{}' loaded: {} virtual A-Box triples",
                    modelId, context.getModel().getStatistics().getVirtualAboxTriples());
            return toDetail(context);
        } catch (RuntimeException e) {
            // Do not leave half-written files behind for a failed load.
            fileStore.delete(modelId);
            throw e;
        }
    }

    public List<ModelSummary> list() {
        return registry.list().stream()
                .map(context -> ModelSummary.from(context.getModel()))
                .collect(Collectors.toList());
    }

    public ModelDetail get(String modelId) {
        return toDetail(registry.require(modelId));
    }

    public void delete(String modelId) {
        registry.remove(modelId);
        fileStore.delete(modelId);
        log.info("Ontology model '{}' removed", modelId);
    }

    // ------------------------------------------------------------------

    private ModelDetail toDetail(ModelContext context) {
        ModelDetail detail = ModelDetail.from(context.getModel());
        try {
            detail.setDatabase(context.getDatabase().productDescription());
        } catch (RuntimeException e) {
            detail.setDatabase("unavailable");
        }
        return detail;
    }

    private static String resolveModelId(String requested) {
        if (isBlank(requested)) {
            return "model-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        }
        String modelId = requested.trim();
        if (!MODEL_ID.matcher(modelId).matches()) {
            throw new InvalidModelException("Invalid modelId '" + modelId
                    + "': use letters, digits, '_' or '-' (max 64 chars)");
        }
        return modelId;
    }

    private byte[] resolveRequiredSource(String kind, String path, String content, String defaultPath) {
        byte[] bytes = resolveSource(kind, path, content, defaultPath);
        if (bytes == null || bytes.length == 0) {
            throw new InvalidModelException("No " + kind + " given: provide " + kind + "Path or "
                    + kind + "Content (or configure app.defaults." + kind + "-path)");
        }
        return bytes;
    }

    private byte[] resolveOptionalSource(String path, String content, String defaultPath) {
        return resolveSource("rules", path, content, defaultPath);
    }

    private byte[] resolveSource(String kind, String path, String content, String defaultPath) {
        if (!isBlank(content)) {
            return content.getBytes(StandardCharsets.UTF_8);
        }
        if (!isBlank(path)) {
            return readFile(properties.resolve(path), kind);
        }
        if (!isBlank(defaultPath)) {
            Path defaultFile = properties.resolve(defaultPath);
            if (Files.isRegularFile(defaultFile)) {
                return readFile(defaultFile, kind);
            }
        }
        return null;
    }

    private static byte[] readFile(Path file, String kind) {
        if (!Files.isRegularFile(file)) {
            throw new InvalidModelException(kind + " file not found: " + file.toAbsolutePath());
        }
        try {
            return Files.readAllBytes(file);
        } catch (IOException e) {
            throw new InvalidModelException("Cannot read " + kind + " file " + file.toAbsolutePath(), e);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
