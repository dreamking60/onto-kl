package com.example.ontology.modeling.infrastructure.ai;

import com.example.ontology.modeling.domain.AiStatus;

import java.util.Collections;

/**
 * Deterministic client used by automated tests (and offline demos):
 * returns a fenced JSON answer that exercises the lenient JSON parser.
 */
public class MockLlmClient implements LlmClient {

    private static final String ANSWER = "```json\n" + "{"
            + "\"classes\":["
            + "{\"name\":\"Document\",\"label\":\"文档\",\"comment\":\"源文档\"},"
            + "{\"name\":\"Concept\",\"label\":\"概念\",\"comment\":\"从文档中抽取的概念\"},"
            + "{\"name\":\"Contract\",\"label\":\"合同\",\"comment\":\"合同类\"}"
            + "],"
            + "\"objectProperties\":["
            + "{\"name\":\"mentions\",\"label\":\"提及\",\"domain\":\"Document\",\"range\":\"Concept\"},"
            + "{\"name\":\"partOf\",\"label\":\"属于\",\"domain\":\"Contract\",\"range\":\"Document\"}"
            + "],"
            + "\"datatypeProperties\":["
            + "{\"name\":\"hasTitle\",\"label\":\"标题\",\"domain\":\"Document\",\"range\":\"xsd:string\"},"
            + "{\"name\":\"hasText\",\"label\":\"正文\",\"domain\":\"Contract\",\"range\":\"xsd:string\"}"
            + "],"
            + "\"subClassOf\":[{\"sub\":\"Contract\",\"superClass\":\"Document\"}],"
            + "\"individuals\":["
            + "{\"name\":\"doc1\",\"label\":\"示例文档\",\"class\":\"Document\"},"
            + "{\"name\":\"contract1\",\"label\":\"示例合同\",\"class\":\"Contract\"},"
            + "{\"name\":\"concept1\",\"label\":\"示例概念\",\"class\":\"Concept\"}"
            + "],"
            + "\"relations\":["
            + "{\"subject\":\"contract1\",\"predicate\":\"partOf\",\"object\":\"doc1\"},"
            + "{\"subject\":\"doc1\",\"predicate\":\"mentions\",\"object\":\"concept1\"}"
            + "],"
            + "\"literals\":["
            + "{\"subject\":\"doc1\",\"predicate\":\"hasTitle\",\"value\":\"示例文档\",\"datatype\":\"xsd:string\",\"language\":\"zh\"}"
            + "]"
            + "}\n```";

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        return ANSWER;
    }

    @Override
    public AiStatus status() {
        AiStatus status = new AiStatus("mock", "in-process", "mock");
        status.setReachable(true);
        status.setModels(Collections.singletonList("mock"));
        status.setMessage("Deterministic mock client (for tests / offline demos)");
        return status;
    }

    @Override
    public String name() {
        return "mock";
    }
}
