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

/** Talks to a local Ollama server ({@code POST /api/chat}, {@code GET /api/tags}). */
public class OllamaLlmClient implements LlmClient {

    private final AiOptions options;
    private final ObjectMapper mapper;
    private final HttpClient http;
    private final String baseUrl;

    public OllamaLlmClient(AiOptions options, ObjectMapper mapper) {
        this.options = options;
        this.mapper = mapper;
        this.baseUrl = normalizeBaseUrl(options.getBaseUrl());
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        int timeout = options.getTimeoutSeconds() == null ? 180 : options.getTimeoutSeconds();
        try {
            ObjectNode body = mapper.createObjectNode();
            body.put("model", options.getModel());
            body.put("stream", false);
            ArrayNode messages = body.putArray("messages");
            messages.addObject().put("role", "system").put("content", systemPrompt);
            messages.addObject().put("role", "user").put("content", userPrompt);
            ObjectNode aiOptions = body.putObject("options");
            if (options.getTemperature() != null) {
                aiOptions.put("temperature", options.getTemperature());
            }

            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/api/chat"))
                    .timeout(Duration.ofSeconds(timeout))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new AiServiceException("Ollama returned HTTP " + response.statusCode()
                        + ": " + truncate(response.body()));
            }
            JsonNode node = mapper.readTree(response.body());
            String content = node.path("message").path("content").asText("");
            if (content.trim().isEmpty()) {
                content = node.path("response").asText("");
            }
            if (content.trim().isEmpty()) {
                throw new AiServiceException("Ollama returned an empty completion");
            }
            return content;
        } catch (AiServiceException e) {
            throw e;
        } catch (IOException e) {
            throw new AiServiceException("Cannot call Ollama at " + baseUrl + ": " + e.getMessage()
                    + " (is `ollama serve` running and model '" + options.getModel() + "' pulled?)", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AiServiceException("Ollama call interrupted", e);
        }
    }

    @Override
    public AiStatus status() {
        AiStatus status = new AiStatus("ollama", baseUrl, options.getModel());
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/api/tags"))
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            status.setReachable(response.statusCode() == 200);
            if (response.statusCode() == 200) {
                JsonNode node = mapper.readTree(response.body());
                List<String> names = new ArrayList<>();
                node.path("models").forEach(model -> names.add(model.path("name").asText()));
                status.setModels(names);
                status.setMessage(names.isEmpty()
                        ? "Ollama is running but no model is installed (`ollama pull " + options.getModel() + "`)"
                        : "Ollama is running");
            } else {
                status.setMessage("Ollama returned HTTP " + response.statusCode());
            }
        } catch (IOException e) {
            status.setReachable(false);
            status.setMessage("Cannot connect to Ollama at " + baseUrl + ": " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            status.setReachable(false);
            status.setMessage("Ollama status check interrupted");
        }
        return status;
    }

    @Override
    public String name() {
        return "ollama";
    }

    private static String normalizeBaseUrl(String url) {
        String value = (url == null || url.trim().isEmpty()) ? "http://127.0.0.1:11434" : url.trim();
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
