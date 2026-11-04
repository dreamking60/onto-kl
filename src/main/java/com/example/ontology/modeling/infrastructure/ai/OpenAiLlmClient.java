package com.example.ontology.modeling.infrastructure.ai;

import com.example.ontology.modeling.domain.AiOptions;
import com.example.ontology.modeling.domain.AiStatus;
import com.example.ontology.modeling.domain.exception.AiServiceException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/** Talks to an OpenAI-compatible chat-completions endpoint (vLLM, LM Studio, Ollama /v1, ...). */
public class OpenAiLlmClient implements LlmClient {

    private final AiOptions options;
    private final ObjectMapper mapper;
    private final HttpClient http;
    private final String baseUrl;

    public OpenAiLlmClient(AiOptions options, ObjectMapper mapper) {
        this.options = options;
        this.mapper = mapper;
        this.baseUrl = normalize(options.getBaseUrl(), "http://127.0.0.1:8000");
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        int timeout = options.getTimeoutSeconds() == null ? 180 : options.getTimeoutSeconds();
        try {
            ObjectNode body = mapper.createObjectNode();
            body.put("model", options.getModel());
            body.put("stream", false);
            if (options.getTemperature() != null) {
                body.put("temperature", options.getTemperature());
            }
            ArrayNode messages = body.putArray("messages");
            messages.addObject().put("role", "system").put("content", systemPrompt);
            messages.addObject().put("role", "user").put("content", userPrompt);

            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(chatUrl()))
                    .timeout(Duration.ofSeconds(timeout))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)));
            if (options.getApiKey() != null && !options.getApiKey().trim().isEmpty()) {
                builder.header("Authorization", "Bearer " + options.getApiKey().trim());
            }
            HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new AiServiceException("OpenAI-compatible endpoint returned HTTP "
                        + response.statusCode() + ": " + truncate(response.body()));
            }
            JsonNode node = mapper.readTree(response.body());
            String content = node.path("choices").path(0).path("message").path("content").asText("");
            if (content.trim().isEmpty()) {
                throw new AiServiceException("OpenAI-compatible endpoint returned an empty completion");
            }
            return content;
        } catch (AiServiceException e) {
            throw e;
        } catch (IOException e) {
            throw new AiServiceException("Cannot call OpenAI-compatible endpoint at " + baseUrl
                    + ": " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AiServiceException("OpenAI-compatible call interrupted", e);
        }
    }

    @Override
    public AiStatus status() {
        AiStatus status = new AiStatus("openai", baseUrl, options.getModel());
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(modelsUrl()))
                    .timeout(Duration.ofSeconds(3)).GET();
            if (options.getApiKey() != null && !options.getApiKey().trim().isEmpty()) {
                builder.header("Authorization", "Bearer " + options.getApiKey().trim());
            }
            HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            status.setReachable(response.statusCode() == 200);
            if (response.statusCode() == 200) {
                JsonNode node = mapper.readTree(response.body());
                List<String> names = new ArrayList<>();
                node.path("data").forEach(model -> names.add(model.path("id").asText()));
                status.setModels(names);
                status.setMessage("OpenAI-compatible endpoint is reachable");
            } else {
                status.setMessage("Endpoint returned HTTP " + response.statusCode());
            }
        } catch (IOException e) {
            status.setReachable(false);
            status.setMessage("Cannot connect to " + baseUrl + ": " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            status.setReachable(false);
            status.setMessage("Status check interrupted");
        }
        return status;
    }

    @Override
    public String name() {
        return "openai";
    }

    private String chatUrl() {
        return baseUrl.endsWith("/v1") ? baseUrl + "/chat/completions" : baseUrl + "/v1/chat/completions";
    }

    private String modelsUrl() {
        return baseUrl.endsWith("/v1") ? baseUrl + "/models" : baseUrl + "/v1/models";
    }

    private static String normalize(String url, String defaultValue) {
        String value = (url == null || url.trim().isEmpty()) ? defaultValue : url.trim();
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    private static String truncate(String value) {
        if (value == null) {
            return "";
        }
        return value.length() <= 500 ? value : value.substring(0, 500) + "...";
    }
}
