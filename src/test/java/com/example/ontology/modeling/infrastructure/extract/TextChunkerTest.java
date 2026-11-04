package com.example.ontology.modeling.infrastructure.extract;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextChunkerTest {

    private final TextChunker chunker = new TextChunker();

    @Test
    void splitsLongTextAndKeepsOverlap() {
        String paragraph = "这是一个用于测试分块的句子。".repeat(60);
        String text = paragraph + "\n\n" + paragraph + "\n\n" + paragraph;
        TextChunker.Result result = chunker.chunk(text, 1000, 200, 10);
        assertTrue(result.getChunks().size() > 1);
        assertFalse(result.isTruncated());
        result.getChunks().forEach(chunk -> assertTrue(chunk.length() <= 1400));
    }

    @Test
    void truncatesAtMaxChunks() {
        String text = "段落内容。".repeat(1000);
        TextChunker.Result result = chunker.chunk(text, 500, 50, 2);
        assertTrue(result.isTruncated());
        assertEquals(2, result.getChunks().size());
    }

    @Test
    void keepsShortTextInOneChunk() {
        TextChunker.Result result = chunker.chunk("短文本", 1000, 100, 5);
        assertEquals(1, result.getChunks().size());
        assertFalse(result.isTruncated());
    }
}
