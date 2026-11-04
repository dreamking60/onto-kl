package com.example.ontology.domain.model;

import java.util.Objects;

/** One triple derived by the Jena reasoner. */
public final class InferredTriple {

    private final String subject;
    private final String predicate;
    private final String object;
    private final TripleObjectKind objectKind;
    private final String datatype;
    private final String language;

    public InferredTriple(String subject, String predicate, String object,
                          TripleObjectKind objectKind, String datatype, String language) {
        this.subject = subject;
        this.predicate = predicate;
        this.object = object;
        this.objectKind = objectKind;
        this.datatype = datatype;
        this.language = language;
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

    public TripleObjectKind getObjectKind() {
        return objectKind;
    }

    public String getDatatype() {
        return datatype;
    }

    public String getLanguage() {
        return language;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof InferredTriple)) {
            return false;
        }
        InferredTriple that = (InferredTriple) o;
        return Objects.equals(subject, that.subject)
                && Objects.equals(predicate, that.predicate)
                && Objects.equals(object, that.object);
    }

    @Override
    public int hashCode() {
        return Objects.hash(subject, predicate, object);
    }

    @Override
    public String toString() {
        return subject + " " + predicate + " " + object;
    }
}
