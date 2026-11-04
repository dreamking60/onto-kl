package com.example.ontology.modeling.infrastructure.ai;

import com.example.ontology.modeling.domain.AiOptions;
import com.example.ontology.modeling.domain.AiStatus;
import com.example.ontology.modeling.domain.exception.ModelingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.Collections;

/** Creates the configured chat-completion client. */
@Component
public class LlmClientFactory {

    private final ObjectMapper mapper;

    public LlmClientFactory(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public String providerOf(AiOptions options) {
        String provider = options.getProvider() == null ? "ollama" : options.getProvider().trim().toLowerCase();
        if (provider.isEmpty()) {
            provider = "ollama";
        }
        return provider;
    }

    public boolean isHeuristic(AiOptions options) {
        return "heuristic".equals(providerOf(options));
    }

    public LlmClient create(AiOptions options) {
        String provider = providerOf(options);
        switch (provider) {
            case "ollama":
                return new OllamaLlmClient(options, mapper);
            case "openai":
                return new OpenAiLlmClient(options, mapper);
            case "mock":
                return new MockLlmClient();
            case "heuristic":
                throw new ModelingException("Provider 'heuristic' is handled by the module itself, "
                        + "no HTTP client is created");
            default:
                throw new ModelingException("Unknown AI provider '" + provider
                        + "' (expected ollama, openai, heuristic or mock)");
        }
    }

    public AiStatus status(AiOptions options) {
        String provider = providerOf(options);
        if ("heuristic".equals(provider)) {
            AiStatus status = new AiStatus("heuristic", "in-process", "-");
            status.setReachable(true);
            status.setModels(Collections.emptyList());
            status.setMessage("Module-only rule-based extractor (no AI model required)");
            return status;
        }
        if ("mock".equals(provider)) {
            return new MockLlmClient().status();
        }
        try (LlmClient client = create(options)) {
            return client.status();
        } catch (RuntimeException e) {
            AiStatus status = new AiStatus(provider, options.getBaseUrl(), options.getModel());
            status.setReachable(false);
            status.setMessage(e.getMessage());
            return status;
        }
    }
}
