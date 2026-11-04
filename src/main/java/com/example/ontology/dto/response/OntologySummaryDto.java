package com.example.ontology.dto.response;

import com.example.ontology.domain.model.OntologySummary;

/** Structural summary of the local ontology definition. */
public class OntologySummaryDto {

    private long triples;
    private int classes;
    private int objectProperties;
    private int datatypeProperties;
    private int namedIndividuals;

    public static OntologySummaryDto from(OntologySummary summary) {
        if (summary == null) {
            return null;
        }
        OntologySummaryDto dto = new OntologySummaryDto();
        dto.triples = summary.getTriples();
        dto.classes = summary.getClasses();
        dto.objectProperties = summary.getObjectProperties();
        dto.datatypeProperties = summary.getDatatypeProperties();
        dto.namedIndividuals = summary.getNamedIndividuals();
        return dto;
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
