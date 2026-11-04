package com.example.ontology.modeling.domain;

/** Statistics of one modeling project. */
public class ModelingStatistics {

    private long textChars;
    private int chunkCount;
    private int classes;
    private int objectProperties;
    private int datatypeProperties;
    private int individuals;
    private int relations;
    private int literals;
    private long tboxTriples;
    private long aboxTriples;
    private long extractionMillis;
    private long generationMillis;

    public long getTextChars() {
        return textChars;
    }

    public void setTextChars(long textChars) {
        this.textChars = textChars;
    }

    public int getChunkCount() {
        return chunkCount;
    }

    public void setChunkCount(int chunkCount) {
        this.chunkCount = chunkCount;
    }

    public int getClasses() {
        return classes;
    }

    public void setClasses(int classes) {
        this.classes = classes;
    }

    public int getObjectProperties() {
        return objectProperties;
    }

    public void setObjectProperties(int objectProperties) {
        this.objectProperties = objectProperties;
    }

    public int getDatatypeProperties() {
        return datatypeProperties;
    }

    public void setDatatypeProperties(int datatypeProperties) {
        this.datatypeProperties = datatypeProperties;
    }

    public int getIndividuals() {
        return individuals;
    }

    public void setIndividuals(int individuals) {
        this.individuals = individuals;
    }

    public int getRelations() {
        return relations;
    }

    public void setRelations(int relations) {
        this.relations = relations;
    }

    public int getLiterals() {
        return literals;
    }

    public void setLiterals(int literals) {
        this.literals = literals;
    }

    public long getTboxTriples() {
        return tboxTriples;
    }

    public void setTboxTriples(long tboxTriples) {
        this.tboxTriples = tboxTriples;
    }

    public long getAboxTriples() {
        return aboxTriples;
    }

    public void setAboxTriples(long aboxTriples) {
        this.aboxTriples = aboxTriples;
    }

    public long getExtractionMillis() {
        return extractionMillis;
    }

    public void setExtractionMillis(long extractionMillis) {
        this.extractionMillis = extractionMillis;
    }

    public long getGenerationMillis() {
        return generationMillis;
    }

    public void setGenerationMillis(long generationMillis) {
        this.generationMillis = generationMillis;
    }
}
