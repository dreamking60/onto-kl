package com.example.ontology.modeling.infrastructure.extract;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** Splits a document into chunks with a small overlap, respecting paragraph boundaries. */
@Component
public class TextChunker {

    public Result chunk(String text, int chunkSize, int overlap, int maxChunks) {
        int size = Math.max(500, chunkSize);
        int overlapChars = Math.max(0, Math.min(overlap, size / 3));
        int max = Math.max(1, maxChunks);

        List<String> blocks = blocks(text, size);
        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String block : blocks) {
            if (current.length() > 0 && current.length() + block.length() + 2 > size) {
                chunks.add(current.toString().trim());
                String previous = chunks.get(chunks.size() - 1);
                String tail = overlapChars == 0 ? ""
                        : previous.substring(Math.max(0, previous.length() - overlapChars));
                current = new StringBuilder(tail);
                if (current.length() > 0) {
                    current.append("\n\n");
                }
            }
            if (current.length() > 0) {
                current.append("\n\n");
            }
            current.append(block.trim());
        }
        if (current.length() > 0) {
            chunks.add(current.toString().trim());
        }
        if (chunks.isEmpty()) {
            chunks.add(text.trim());
        }
        boolean truncated = chunks.size() > max;
        if (truncated) {
            chunks = new ArrayList<>(chunks.subList(0, max));
        }
        return new Result(chunks, truncated);
    }

    private static List<String> blocks(String text, int size) {
        List<String> blocks = new ArrayList<>();
        for (String paragraph : text.split("\\n\\s*\\n")) {
            String value = paragraph.trim();
            if (value.isEmpty()) {
                continue;
            }
            if (value.length() <= size) {
                blocks.add(value);
                continue;
            }
            for (String piece : splitLongBlock(value, size)) {
                blocks.add(piece);
            }
        }
        return blocks;
    }

    private static List<String> splitLongBlock(String block, int size) {
        List<String> pieces = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String sentence : block.split("(?<=[。！？.!?；;])\\s*")) {
            if (sentence.isEmpty()) {
                continue;
            }
            if (current.length() > 0 && current.length() + sentence.length() > size) {
                pieces.add(current.toString().trim());
                current = new StringBuilder();
            }
            if (sentence.length() > size) {
                if (current.length() > 0) {
                    pieces.add(current.toString().trim());
                    current = new StringBuilder();
                }
                for (int start = 0; start < sentence.length(); start += size) {
                    pieces.add(sentence.substring(start, Math.min(sentence.length(), start + size)));
                }
            } else {
                current.append(sentence);
            }
        }
        if (current.length() > 0) {
            pieces.add(current.toString().trim());
        }
        return pieces;
    }

    /** Chunking result. */
    public static final class Result {
        private final List<String> chunks;
        private final boolean truncated;

        public Result(List<String> chunks, boolean truncated) {
            this.chunks = chunks;
            this.truncated = truncated;
        }

        public List<String> getChunks() {
            return chunks;
        }

        public boolean isTruncated() {
            return truncated;
        }
    }
}
