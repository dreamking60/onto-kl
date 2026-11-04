package com.example.ontology.infrastructure.store;

import com.example.ontology.config.AppProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Stores the local ontology definitions (T-Box), OBDA mapping and business
 * rules of every loaded model under {@code app.data-dir}/{modelId}.
 *
 * <p>The database only holds instances; ontology definitions always stay on
 * the local filesystem.</p>
 */
@Component
public class ModelFileStore {

    private static final String ONTOLOGY_FILE = "ontology.ttl";
    private static final String MAPPING_FILE = "mapping.obda";
    private static final String RULES_FILE = "rules.rules";
    private static final String INSTANCES_FILE = "instances.ttl";

    private final Path dataDir;

    public ModelFileStore(AppProperties properties) {
        this.dataDir = properties.dataDirPath();
        try {
            Files.createDirectories(dataDir);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create data directory " + dataDir, e);
        }
    }

    public Path modelDir(String modelId) {
        return dataDir.resolve(modelId);
    }

    public Path ontologyFile(String modelId) {
        return modelDir(modelId).resolve(ONTOLOGY_FILE);
    }

    public Path mappingFile(String modelId) {
        return modelDir(modelId).resolve(MAPPING_FILE);
    }

    public Path rulesFile(String modelId) {
        return modelDir(modelId).resolve(RULES_FILE);
    }

    public Path instancesFile(String modelId) {
        return modelDir(modelId).resolve(INSTANCES_FILE);
    }

    /** Persists the sources of a model, replacing any previous version. */
    public void store(String modelId, byte[] ontology, byte[] mapping, byte[] rules) {
        store(modelId, ontology, mapping, rules, null);
    }

    /**
     * Persists ontology / mapping / rules / (optional) local A-Box instances.
     * A model needs at least an OBDA mapping or a local instances file.
     */
    public void store(String modelId, byte[] ontology, byte[] mapping, byte[] rules, byte[] instances) {
        Path dir = modelDir(modelId);
        deleteRecursively(dir);
        try {
            Files.createDirectories(dir);
            Files.write(ontologyFile(modelId), ontology);
            if (mapping != null && mapping.length > 0) {
                Files.write(mappingFile(modelId), mapping);
            }
            if (rules != null && rules.length > 0) {
                Files.write(rulesFile(modelId), rules);
            }
            if (instances != null && instances.length > 0) {
                Files.write(instancesFile(modelId), instances);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot store model files under " + dir, e);
        }
    }

    public boolean isStored(String modelId) {
        return Files.isRegularFile(ontologyFile(modelId))
                && (Files.isRegularFile(mappingFile(modelId))
                || Files.isRegularFile(instancesFile(modelId)));
    }

    /** Ids of models whose files exist on disk (stored by a previous run). */
    public List<String> listStoredModelIds() {
        if (!Files.isDirectory(dataDir)) {
            return new ArrayList<>();
        }
        try (Stream<Path> stream = Files.list(dataDir)) {
            return stream.filter(Files::isDirectory)
                    .map(path -> path.getFileName().toString())
                    .sorted()
                    .collect(Collectors.toList());
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    public void delete(String modelId) {
        deleteRecursively(modelDir(modelId));
    }

    private void deleteRecursively(Path dir) {
        if (!Files.exists(dir)) {
            return;
        }
        try (Stream<Path> stream = Files.walk(dir)) {
            List<Path> paths = stream.sorted(Comparator.reverseOrder()).collect(Collectors.toList());
            for (Path path : paths) {
                Files.deleteIfExists(path);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot delete " + dir, e);
        }
    }
}
