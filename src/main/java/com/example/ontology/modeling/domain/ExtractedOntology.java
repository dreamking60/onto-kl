package com.example.ontology.modeling.domain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Intermediate extraction result: concepts, properties, individuals and facts
 * discovered in the source document.  This is the contract between the AI /
 * heuristic extractors and the Turtle generator.
 */
public class ExtractedOntology {

    private Map<String, ExtractedClass> classes = new LinkedHashMap<>();
    private Map<String, ExtractedProperty> objectProperties = new LinkedHashMap<>();
    private Map<String, ExtractedProperty> datatypeProperties = new LinkedHashMap<>();
    private Map<String, ExtractedIndividual> individuals = new LinkedHashMap<>();
    private List<SubClass> subClassOf = new ArrayList<>();
    private List<ExtractedRelation> relations = new ArrayList<>();
    private List<ExtractedLiteral> literals = new ArrayList<>();
    private int chunkCount;
    private boolean aiUsed;

    public Map<String, ExtractedClass> getClasses() {
        return classes;
    }

    public void setClasses(Map<String, ExtractedClass> classes) {
        this.classes = classes;
    }

    public Map<String, ExtractedProperty> getObjectProperties() {
        return objectProperties;
    }

    public void setObjectProperties(Map<String, ExtractedProperty> objectProperties) {
        this.objectProperties = objectProperties;
    }

    public Map<String, ExtractedProperty> getDatatypeProperties() {
        return datatypeProperties;
    }

    public void setDatatypeProperties(Map<String, ExtractedProperty> datatypeProperties) {
        this.datatypeProperties = datatypeProperties;
    }

    public Map<String, ExtractedIndividual> getIndividuals() {
        return individuals;
    }

    public void setIndividuals(Map<String, ExtractedIndividual> individuals) {
        this.individuals = individuals;
    }

    public List<SubClass> getSubClassOf() {
        return subClassOf;
    }

    public void setSubClassOf(List<SubClass> subClassOf) {
        this.subClassOf = subClassOf;
    }

    public List<ExtractedRelation> getRelations() {
        return relations;
    }

    public void setRelations(List<ExtractedRelation> relations) {
        this.relations = relations;
    }

    public List<ExtractedLiteral> getLiterals() {
        return literals;
    }

    public void setLiterals(List<ExtractedLiteral> literals) {
        this.literals = literals;
    }

    public int getChunkCount() {
        return chunkCount;
    }

    public void setChunkCount(int chunkCount) {
        this.chunkCount = chunkCount;
    }

    public boolean isAiUsed() {
        return aiUsed;
    }

    public void setAiUsed(boolean aiUsed) {
        this.aiUsed = aiUsed;
    }

    // ------------------------------------------------------------------
    // Nested data holders
    // ------------------------------------------------------------------

    public static class ExtractedClass {
        private String name;
        private String label;
        private String comment;

        public ExtractedClass() {
        }

        public ExtractedClass(String name, String label, String comment) {
            this.name = name;
            this.label = label;
            this.comment = comment;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getComment() {
            return comment;
        }

        public void setComment(String comment) {
            this.comment = comment;
        }
    }

    public static class ExtractedProperty {
        private String name;
        private String label;
        private String comment;
        private String domain;
        private String range;
        private boolean transitive;
        private boolean symmetric;

        public ExtractedProperty() {
        }

        public ExtractedProperty(String name, String label, String domain, String range) {
            this.name = name;
            this.label = label;
            this.domain = domain;
            this.range = range;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getComment() {
            return comment;
        }

        public void setComment(String comment) {
            this.comment = comment;
        }

        public String getDomain() {
            return domain;
        }

        public void setDomain(String domain) {
            this.domain = domain;
        }

        public String getRange() {
            return range;
        }

        public void setRange(String range) {
            this.range = range;
        }

        public boolean isTransitive() {
            return transitive;
        }

        public void setTransitive(boolean transitive) {
            this.transitive = transitive;
        }

        public boolean isSymmetric() {
            return symmetric;
        }

        public void setSymmetric(boolean symmetric) {
            this.symmetric = symmetric;
        }
    }

    public static class ExtractedIndividual {
        private String name;
        private String label;
        private String className;
        private String comment;

        public ExtractedIndividual() {
        }

        public ExtractedIndividual(String name, String label, String className) {
            this.name = name;
            this.label = label;
            this.className = className;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getClassName() {
            return className;
        }

        public void setClassName(String className) {
            this.className = className;
        }

        public String getComment() {
            return comment;
        }

        public void setComment(String comment) {
            this.comment = comment;
        }
    }

    public static class SubClass {
        private String sub;
        private String superClass;

        public SubClass() {
        }

        public SubClass(String sub, String superClass) {
            this.sub = sub;
            this.superClass = superClass;
        }

        public String getSub() {
            return sub;
        }

        public void setSub(String sub) {
            this.sub = sub;
        }

        public String getSuperClass() {
            return superClass;
        }

        public void setSuperClass(String superClass) {
            this.superClass = superClass;
        }
    }

    public static class ExtractedRelation {
        private String subject;
        private String predicate;
        private String object;

        public ExtractedRelation() {
        }

        public ExtractedRelation(String subject, String predicate, String object) {
            this.subject = subject;
            this.predicate = predicate;
            this.object = object;
        }

        public String getSubject() {
            return subject;
        }

        public void setSubject(String subject) {
            this.subject = subject;
        }

        public String getPredicate() {
            return predicate;
        }

        public void setPredicate(String predicate) {
            this.predicate = predicate;
        }

        public String getObject() {
            return object;
        }

        public void setObject(String object) {
            this.object = object;
        }
    }

    public static class ExtractedLiteral {
        private String subject;
        private String predicate;
        private String value;
        private String datatype;
        private String language;

        public ExtractedLiteral() {
        }

        public ExtractedLiteral(String subject, String predicate, String value, String datatype, String language) {
            this.subject = subject;
            this.predicate = predicate;
            this.value = value;
            this.datatype = datatype;
            this.language = language;
        }

        public String getSubject() {
            return subject;
        }

        public void setSubject(String subject) {
            this.subject = subject;
        }

        public String getPredicate() {
            return predicate;
        }

        public void setPredicate(String predicate) {
            this.predicate = predicate;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getDatatype() {
            return datatype;
        }

        public void setDatatype(String datatype) {
            this.datatype = datatype;
        }

        public String getLanguage() {
            return language;
        }

        public void setLanguage(String language) {
            this.language = language;
        }
    }
}
