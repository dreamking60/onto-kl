package com.example.ontology.modeling.dto;

import com.example.ontology.modeling.domain.AiStatus;

import java.util.List;

/** Response of {@code GET /modeling/ai/status}. */
public class AiStatusDto {

    private String provider;
    private String baseUrl;
    private String model;
    private boolean reachable;
    private List<String> models;
    private String message;

    public static AiStatusDto from(AiStatus status) {
        AiStatusDto dto = new AiStatusDto();
        dto.provider = status.getProvider();
        dto.baseUrl = status.getBaseUrl();
        dto.model = status.getModel();
        dto.reachable = status.isReachable();
        dto.models = status.getModels();
        dto.message = status.getMessage();
        return dto;
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

    public List<String> getModels() {
        return models;
    }

    public String getMessage() {
        return message;
    }
}
