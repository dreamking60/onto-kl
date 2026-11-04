package com.example.ontology.modeling.domain;

/** AI settings for one extraction run (merged from defaults + request overrides). */
public class AiOptions {

    private String provider;
    private String baseUrl;
    private String model;
    private String apiKey;
    private Double temperature;
    private Boolean fallbackToHeuristic;
    private Integer timeoutSeconds;

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

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Boolean getFallbackToHeuristic() {
        return fallbackToHeuristic;
    }

    public void setFallbackToHeuristic(Boolean fallbackToHeuristic) {
        this.fallbackToHeuristic = fallbackToHeuristic;
    }

    public Integer getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(Integer timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }
}
