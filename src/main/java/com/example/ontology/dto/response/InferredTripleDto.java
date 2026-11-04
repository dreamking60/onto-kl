package com.example.ontology.dto.response;

import com.example.ontology.domain.model.InferredTriple;

/** One inferred triple (IRI / literal object with optional datatype or language). */
public class InferredTripleDto {

    private String subject;
    private String predicate;
    private String object;
    private String objectKind;
    private String datatype;
    private String language;

    public static InferredTripleDto from(InferredTriple triple) {
        InferredTripleDto dto = new InferredTripleDto();
        dto.subject = triple.getSubject();
        dto.predicate = triple.getPredicate();
        dto.object = triple.getObject();
        dto.objectKind = triple.getObjectKind() == null ? null : triple.getObjectKind().name();
        dto.datatype = triple.getDatatype();
        dto.language = triple.getLanguage();
        return dto;
    }

    public String getSubject() {
        return subject;
    }

    public String getPredicate() {
        return predicate;
    }

    public String getObject() {
        return object;
    }

    public String getObjectKind() {
        return objectKind;
    }

    public String getDatatype() {
        return datatype;
    }

    public String getLanguage() {
        return language;
    }
}
