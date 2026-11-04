package com.example.ontology.modeling.infrastructure.extract;

import com.example.ontology.modeling.config.ModelingProperties;
import com.example.ontology.modeling.domain.AiOptions;
import com.example.ontology.modeling.domain.ExtractedOntology;
import com.example.ontology.modeling.domain.ExtractionOutcome;
import com.example.ontology.modeling.domain.SourceDocument;
import com.example.ontology.modeling.domain.exception.AiServiceException;
import com.example.ontology.modeling.infrastructure.ai.LlmClient;
import com.example.ontology.modeling.infrastructure.ai.LlmClientFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Runs the document extraction: AI chunks (Ollama / OpenAI-compatible) or the
 * module-native heuristic extractor, with configurable fallback.
 */
@Component
public class AiOntologyExtractor {

    private static final String SYSTEM_PROMPT = String.join("\n",
            "你是一名严谨的本体工程师。请阅读用户给出的文档片段，抽取领域本体和实例。",
            "只输出一个 JSON 对象，不要输出 Markdown 代码块、解释或多余文本。JSON 结构如下：",
            "{",
            "  \"classes\": [{\"name\":\"章节类型或概念名\", \"label\":\"中文名称\", \"comment\":\"一句说明\"}],",
            "  \"objectProperties\": [{\"name\":\"关系名\", \"label\":\"中文名\", \"domain\":\"类名\", \"range\":\"类名\"}],",
            "  \"datatypeProperties\": [{\"name\":\"属性名\", \"label\":\"中文名\", \"domain\":\"类名\", \"range\":\"xsd:string\"}],",
            "  \"subClassOf\": [{\"sub\":\"子类名\", \"superClass\":\"父类名\"}],",
            "  \"individuals\": [{\"name\":\"实例标识\", \"label\":\"显示名\", \"class\":\"类名\"}],",
            "  \"relations\": [{\"subject\":\"实例标识\", \"predicate\":\"关系名\", \"object\":\"实例标识\"}],",
            "  \"literals\": [{\"subject\":\"实例标识\", \"predicate\":\"属性名\", \"value\":\"值\", \"datatype\":\"xsd:string\", \"language\":\"zh\"}]",
            "}",
            "要求：name 使用简短、稳定、无空格的标识符（中英文均可）；label 用中文；只抽取文本明确支持的内容；",
            "最多 15 个类、20 个个体；不确定的内容不要编造。");

    private final LlmClientFactory clientFactory;
    private final HeuristicOntologyExtractor heuristicExtractor;
    private final OntologyJsonParser jsonParser;
    private final TextChunker chunker;
    private final ModelingProperties properties;

    public AiOntologyExtractor(LlmClientFactory clientFactory,
                               HeuristicOntologyExtractor heuristicExtractor,
                               OntologyJsonParser jsonParser,
                               TextChunker chunker,
                               ModelingProperties properties) {
        this.clientFactory = clientFactory;
        this.heuristicExtractor = heuristicExtractor;
        this.jsonParser = jsonParser;
        this.chunker = chunker;
        this.properties = properties;
    }

    public ExtractionOutcome extract(SourceDocument document, AiOptions options,
                                     int chunkSize, int maxChunks) {
        List<String> warnings = new ArrayList<>();
        String provider = clientFactory.providerOf(options);
        if ("heuristic".equals(provider)) {
            warnings.add("Using the module heuristic extractor (no AI model).");
            return new ExtractionOutcome(heuristicExtractor.extract(document.getText(), document.getFileName()),
                    warnings, false);
        }

        boolean fallback = options.getFallbackToHeuristic() == null
                ? properties.getAi().isFallbackToHeuristic()
                : options.getFallbackToHeuristic();
        TextChunker.Result chunking = chunker.chunk(document.getText(), chunkSize,
                properties.getChunkOverlap(), maxChunks);
        if (chunking.isTruncated()) {
            warnings.add("Document is long: only the first " + maxChunks + " chunks were sent to the AI.");
        }

        ExtractedOntology merged = new ExtractedOntology();
        merged.setChunkCount(chunking.getChunks().size());
        boolean aiUsed = false;
        try (LlmClient client = clientFactory.create(options)) {
            List<String> chunks = chunking.getChunks();
            for (int index = 0; index < chunks.size(); index++) {
                String chunk = chunks.get(index);
                try {
                    String answer = client.complete(SYSTEM_PROMPT,
                            userPrompt(document.getFileName(), index, chunks.size(), chunk));
                    boolean parsed = jsonParser.parseInto(answer, index + 1, merged, warnings);
                    aiUsed = aiUsed || parsed;
                    if (!parsed && fallback) {
                        warnings.add("chunk " + (index + 1) + ": falling back to module heuristic extraction.");
                        heuristicExtractor.mergeInto(merged,
                                heuristicExtractor.extract(chunk, document.getFileName()));
                    }
                } catch (AiServiceException e) {
                    if (!fallback) {
                        throw e;
                    }
                    warnings.add("chunk " + (index + 1) + ": AI call failed, using heuristic fallback: "
                            + e.getMessage());
                    heuristicExtractor.mergeInto(merged,
                            heuristicExtractor.extract(chunk, document.getFileName()));
                }
            }
        } catch (AiServiceException e) {
            throw e;
        } catch (RuntimeException e) {
            if (!fallback) {
                throw new AiServiceException("AI extraction failed: " + e.getMessage(), e);
            }
            warnings.add("AI backend unavailable (" + e.getMessage() + "); using heuristic fallback.");
            heuristicExtractor.mergeInto(merged,
                    heuristicExtractor.extract(document.getText(), document.getFileName()));
        }

        if (merged.getClasses().isEmpty() && merged.getIndividuals().isEmpty()) {
            warnings.add("AI produced no usable items; using heuristic fallback.");
            heuristicExtractor.mergeInto(merged,
                    heuristicExtractor.extract(document.getText(), document.getFileName()));
        }
        merged.setAiUsed(aiUsed);
        return new ExtractionOutcome(merged, warnings, chunking.isTruncated());
    }

    private static String userPrompt(String fileName, int index, int total, String chunk) {
        return "文档名：" + fileName + "\n"
                + "片段 " + (index + 1) + "/" + total + "：\n"
                + "```text\n" + chunk + "\n```\n"
                + "请抽取本体类和实例，严格按系统提示的 JSON 结构输出。";
    }
}
