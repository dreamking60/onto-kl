package com.example.ontology.modeling.domain;

import java.util.List;

/** Result of running one extractor over a source document. */
public class ExtractionOutcome {

    private final ExtractedOntology ontology;
    private final List<String> warnings;
    private final boolean truncated;

    public ExtractionOutcome(ExtractedOntology ontology, List<String> warnings, boolean truncated) {
        this.ontology = ontology;
        this.warnings = warnings;
        this.truncated = truncated;
    }

    public ExtractedOntology getOntology() {
        return ontology;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public boolean isTruncated() {
        return truncated;
    }
}
