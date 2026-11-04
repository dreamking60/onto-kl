package com.example.ontology.domain.model;

/** Structural summary of the local T-Box (ontology definition). */
public final class OntologySummary {

    private final long triples;
    private final int classes;
    private final int objectProperties;
    private final int datatypeProperties;
    private final int namedIndividuals;

    public OntologySummary(long triples, int classes, int objectProperties,
                           int datatypeProperties, int namedIndividuals) {
        this.triples = triples;
        this.classes = classes;
        this.objectProperties = objectProperties;
        this.datatypeProperties = datatypeProperties;
        this.namedIndividuals = namedIndividuals;
    }

    public long getTriples() {
        return triples;
    }

    public int getClasses() {
        return classes;
    }

    public int getObjectProperties() {
        return objectProperties;
    }

    public int getDatatypeProperties() {
        return datatypeProperties;
    }

    public int getNamedIndividuals() {
        return namedIndividuals;
    }
}
