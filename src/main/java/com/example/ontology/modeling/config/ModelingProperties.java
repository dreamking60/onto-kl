package com.example.ontology.modeling.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Configuration of the document-driven ontology modeling module
 * (prefix {@code app.modeling}).
 */
@ConfigurationProperties(prefix = "app.modeling")
public class ModelingProperties {

    private String dataDir = "data/modeling";
    private String namespaceBase = "http://example.org/model/";
    private int chunkSize = 3500;
    private int chunkOverlap = 200;
    private int maxChunks = 15;
    private long maxDocumentBytes = 5L * 1024L * 1024L;
    private Ai ai = new Ai();

    public Path dataDirPath() {
        Path path = Paths.get(dataDir);
        return path.isAbsolute()
                ? path.toAbsolutePath().normalize()
                : Paths.get("").toAbsolutePath().resolve(path).normalize();
    }

    /** Namespace of a project, e.g. {@code http://example.org/model/p-123#}. */
    public String namespaceFor(String projectId, String override) {
        String ns = (override == null || override.trim().isEmpty()) ? namespaceBase : override.trim();
        if (!ns.endsWith("#") && !ns.endsWith("/")) {
            ns = ns + "#";
        }
        return override == null || override.trim().isEmpty() ? ns + projectId + "#" : ns;
    }

    public String getDataDir() {
        return dataDir;
    }

    public void setDataDir(String dataDir) {
        this.dataDir = dataDir;
    }

    public String getNamespaceBase() {
        return namespaceBase;
    }

    public void setNamespaceBase(String namespaceBase) {
        this.namespaceBase = namespaceBase;
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    public int getChunkOverlap() {
        return chunkOverlap;
    }

    public void setChunkOverlap(int chunkOverlap) {
        this.chunkOverlap = chunkOverlap;
    }

    public int getMaxChunks() {
        return maxChunks;
    }

    public void setMaxChunks(int maxChunks) {
        this.maxChunks = maxChunks;
    }

    public long getMaxDocumentBytes() {
        return maxDocumentBytes;
    }

    public void setMaxDocumentBytes(long maxDocumentBytes) {
        this.maxDocumentBytes = maxDocumentBytes;
    }

    public Ai getAi() {
        return ai;
    }

    public void setAi(Ai ai) {
        this.ai = ai;
    }

    public static class Ai {
        /** ollama | openai | heuristic | mock */
        private String provider = "ollama";
        private String baseUrl = "http://127.0.0.1:11434";
        private String model = "qwen2.5:7b";
        private String apiKey = "";
        private int timeoutSeconds = 180;
        private double temperature = 0.2;
        private boolean fallbackToHeuristic = true;

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public int getTimeoutSeconds() {
            return timeoutSeconds;
        }

        public void setTimeoutSeconds(int timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }

        public double getTemperature() {
            return temperature;
        }

        public void setTemperature(double temperature) {
            this.temperature = temperature;
        }

        public boolean isFallbackToHeuristic() {
            return fallbackToHeuristic;
        }

        public void setFallbackToHeuristic(boolean fallbackToHeuristic) {
            this.fallbackToHeuristic = fallbackToHeuristic;
        }
    }
}
