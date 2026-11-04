package com.example.ontology.modeling.domain;

import java.util.List;

/** Reachability / configuration of the configured AI backend. */
public class AiStatus {

    private String provider;
    private String baseUrl;
    private String model;
    private boolean reachable;
    private List<String> models;
    private String message;

    public AiStatus(String provider, String baseUrl, String model) {
        this.provider = provider;
        this.baseUrl = baseUrl;
        this.model = model;
    }

    public String getProvider() {
        return provider;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getModel() {
        return model;
    }

    public boolean isReachable() {
        return reachable;
    }

    public void setReachable(boolean reachable) {
        this.reachable = reachable;
    }

    public List<String> getModels() {
        return models;
    }

    public void setModels(List<String> models) {
        this.models = models;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
