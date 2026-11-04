package com.example.ontology.modeling.infrastructure.ai;

import com.example.ontology.modeling.domain.AiStatus;

/** Minimal chat-completion abstraction used by the ontology extractor. */
public interface LlmClient extends AutoCloseable {

    String complete(String systemPrompt, String userPrompt);

    AiStatus status();

    String name();

    @Override
    default void close() {
    }
}
