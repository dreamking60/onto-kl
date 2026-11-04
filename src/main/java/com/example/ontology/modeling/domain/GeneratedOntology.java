package com.example.ontology.modeling.domain;

/** The two generated documents: T-Box (ontology definition) and A-Box (instances). */
public class GeneratedOntology {

    private final String tboxTurtle;
    private final String aboxTurtle;
    private final long tboxTriples;
    private final long aboxTriples;
    private final int classes;
    private final int objectProperties;
    private final int datatypeProperties;
    private final int individuals;

    public GeneratedOntology(String tboxTurtle, String aboxTurtle, long tboxTriples, long aboxTriples,
                             int classes, int objectProperties, int datatypeProperties, int individuals) {
        this.tboxTurtle = tboxTurtle;
        this.aboxTurtle = aboxTurtle;
        this.tboxTriples = tboxTriples;
        this.aboxTriples = aboxTriples;
        this.classes = classes;
        this.objectProperties = objectProperties;
        this.datatypeProperties = datatypeProperties;
        this.individuals = individuals;
    }

    public String getTboxTurtle() {
        return tboxTurtle;
    }

    public String getAboxTurtle() {
        return aboxTurtle;
    }

    public long getTboxTriples() {
        return tboxTriples;
    }

    public long getAboxTriples() {
        return aboxTriples;
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

    public int getIndividuals() {
        return individuals;
    }
}
