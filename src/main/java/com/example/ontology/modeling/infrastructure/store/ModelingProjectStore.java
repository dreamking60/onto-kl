package com.example.ontology.modeling.infrastructure.store;

import com.example.ontology.modeling.config.ModelingProperties;
import com.example.ontology.modeling.domain.ModelingProject;
import com.example.ontology.modeling.domain.ModelingStatistics;
import com.example.ontology.modeling.domain.ModelingStatus;
import com.example.ontology.modeling.domain.exception.ModelingException;
import com.example.ontology.modeling.domain.exception.ModelingProjectNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * File-system storage of modeling projects:
 * {@code data/modeling/{projectId}/source.txt, extraction.json, ontology.ttl,
 * instances.ttl, meta.json}.
 */
@Component
public class ModelingProjectStore {

    private static final String TEXT_FILE = "source.txt";
    private static final String EXTRACTION_FILE = "extraction.json";
    private static final String ONTOLOGY_FILE = "ontology.ttl";
    private static final String INSTANCES_FILE = "instances.ttl";
    private static final String META_FILE = "meta.json";

    private final Path dataDir;
    private final ObjectMapper mapper;

    public ModelingProjectStore(ModelingProperties properties, ObjectMapper mapper) {
        this.dataDir = properties.dataDirPath();
        this.mapper = mapper;
        try {
            Files.createDirectories(dataDir);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create modeling data directory " + dataDir, e);
        }
    }

    public Path projectDir(String projectId) {
        return dataDir.resolve(projectId);
    }

    public Path sourceTextFile(String projectId) {
        return projectDir(projectId).resolve(TEXT_FILE);
    }

    public Path extractionFile(String projectId) {
        return projectDir(projectId).resolve(EXTRACTION_FILE);
    }

    public Path ontologyFile(String projectId) {
        return projectDir(projectId).resolve(ONTOLOGY_FILE);
    }

    public Path instancesFile(String projectId) {
        return projectDir(projectId).resolve(INSTANCES_FILE);
    }

    public Path metaFile(String projectId) {
        return projectDir(projectId).resolve(META_FILE);
    }

    /** Creates the project directory, stores the original source file and normalized text. */
    public void create(String projectId, String fileName, byte[] sourceBytes, String text) {
        Path dir = projectDir(projectId);
        try {
            Files.createDirectories(dir);
            String extension = extensionOf(fileName);
            Files.write(dir.resolve("source." + extension), sourceBytes);
            Files.write(sourceTextFile(projectId), text.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new ModelingException("Cannot store modeling project files: " + e.getMessage(), e);
        }
    }

    public String readText(String projectId) {
        return readString(sourceTextFile(projectId));
    }

    public void writeExtraction(String projectId, String json) {
        writeString(extractionFile(projectId), json);
    }

    public String readExtraction(String projectId) {
        return readString(extractionFile(projectId));
    }

    public void writeOntology(String projectId, String turtle) {
        writeString(ontologyFile(projectId), turtle);
    }

    public String readOntology(String projectId) {
        return readString(ontologyFile(projectId));
    }

    public void writeInstances(String projectId, String turtle) {
        writeString(instancesFile(projectId), turtle);
    }

    public String readInstances(String projectId) {
        return readString(instancesFile(projectId));
    }

    public void saveMeta(ModelingProject project) {
        project.setUpdatedAt(Instant.now());
        try {
            Files.createDirectories(projectDir(project.getId()));
            Files.write(metaFile(project.getId()),
                    mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(ProjectMeta.from(project)));
        } catch (IOException e) {
            throw new ModelingException("Cannot save modeling project metadata: " + e.getMessage(), e);
        }
    }

    public ModelingProject load(String projectId) {
        Path meta = metaFile(projectId);
        if (!Files.isRegularFile(meta)) {
            throw new ModelingProjectNotFoundException(projectId);
        }
        try {
            ProjectMeta stored = mapper.readValue(meta.toFile(), ProjectMeta.class);
            ModelingProject project = stored.toProject();
            project.setSourceTextFile(sourceTextFile(projectId));
            project.setExtractionFile(extractionFile(projectId));
            project.setOntologyFile(ontologyFile(projectId));
            project.setInstancesFile(instancesFile(projectId));
            return project;
        } catch (IOException e) {
            throw new ModelingException("Cannot read modeling project metadata: " + e.getMessage(), e);
        }
    }

    public List<ModelingProject> list() {
        if (!Files.isDirectory(dataDir)) {
            return new ArrayList<>();
        }
        try (Stream<Path> stream = Files.list(dataDir)) {
            return stream.filter(Files::isDirectory)
                    .map(path -> path.getFileName().toString())
                    .filter(id -> Files.isRegularFile(metaFile(id)))
                    .map(this::load)
                    .sorted(Comparator.comparing(ModelingProject::getCreatedAt,
                            Comparator.nullsLast(Comparator.reverseOrder())))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new ModelingException("Cannot list modeling projects: " + e.getMessage(), e);
        }
    }

    public void delete(String projectId) {
        deleteRecursively(projectDir(projectId));
    }

    private void writeString(Path file, String content) {
        try {
            Files.createDirectories(file.getParent());
            Files.write(file, content.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new ModelingException("Cannot write " + file.getFileName() + ": " + e.getMessage(), e);
        }
    }

    private static String readString(Path file) {
        if (!Files.isRegularFile(file)) {
            throw new ModelingException("File not found in modeling project: " + file.getFileName());
        }
        try {
            return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ModelingException("Cannot read " + file.getFileName() + ": " + e.getMessage(), e);
        }
    }

    private static String extensionOf(String fileName) {
        String name = fileName == null ? "" : fileName;
        int dot = name.lastIndexOf('.');
        String extension = dot >= 0 && dot < name.length() - 1
                ? name.substring(dot + 1).toLowerCase(java.util.Locale.ROOT)
                : "txt";
        return extension.matches("[a-z0-9]{1,10}") ? extension : "txt";
    }

    private static void deleteRecursively(Path dir) {
        if (!Files.exists(dir)) {
            return;
        }
        try (Stream<Path> stream = Files.walk(dir)) {
            for (Path path : stream.sorted(Comparator.reverseOrder()).collect(Collectors.toList())) {
                Files.deleteIfExists(path);
            }
        } catch (IOException e) {
            throw new ModelingException("Cannot delete modeling project files: " + e.getMessage(), e);
        }
    }

    /** Serializable metadata (kept separate so Path fields are not serialised). */
    public static class ProjectMeta {
        public String id;
        public String name;
        public String sourceFileName;
        public String sourceType;
        public String status;
        public String namespace;
        public String aiProvider;
        public String aiModel;
        public String modelId;
        public Instant createdAt;
        public Instant updatedAt;
        public List<String> warnings = new ArrayList<>();
        public ModelingStatistics statistics = new ModelingStatistics();
        public String error;

        public static ProjectMeta from(ModelingProject project) {
            ProjectMeta meta = new ProjectMeta();
            meta.id = project.getId();
            meta.name = project.getName();
            meta.sourceFileName = project.getSourceFileName();
            meta.sourceType = project.getSourceType();
            meta.status = project.getStatus().name();
            meta.namespace = project.getNamespace();
            meta.aiProvider = project.getAiProvider();
            meta.aiModel = project.getAiModel();
            meta.modelId = project.getModelId();
            meta.createdAt = project.getCreatedAt();
            meta.updatedAt = project.getUpdatedAt();
            meta.warnings = project.getWarnings();
            meta.statistics = project.getStatistics();
            meta.error = project.getError();
            return meta;
        }

        public ModelingProject toProject() {
            ModelingProject project = new ModelingProject();
            project.setId(id);
            project.setName(name);
            project.setSourceFileName(sourceFileName);
            project.setSourceType(sourceType);
            project.setStatus(status == null ? ModelingStatus.CREATED : ModelingStatus.valueOf(status));
            project.setNamespace(namespace);
            project.setAiProvider(aiProvider);
            project.setAiModel(aiModel);
            project.setModelId(modelId);
            project.setCreatedAt(createdAt);
            project.setUpdatedAt(updatedAt);
            project.setWarnings(warnings == null ? new ArrayList<>() : warnings);
            project.setStatistics(statistics == null ? new ModelingStatistics() : statistics);
            project.setError(error);
            return project;
        }
    }
}
