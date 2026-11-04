package com.example.ontology.modeling.infrastructure.extract;

import com.example.ontology.modeling.domain.ExtractedOntology;
import com.example.ontology.modeling.domain.ExtractedOntology.ExtractedClass;
import com.example.ontology.modeling.domain.ExtractedOntology.ExtractedIndividual;
import com.example.ontology.modeling.domain.ExtractedOntology.ExtractedLiteral;
import com.example.ontology.modeling.domain.ExtractedOntology.ExtractedProperty;
import com.example.ontology.modeling.domain.ExtractedOntology.ExtractedRelation;
import com.example.ontology.modeling.domain.ExtractedOntology.SubClass;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Lenient parser for the JSON answer produced by the AI extractor.
 * Tolerates Markdown code fences, missing fields and alternative key names.
 */
@Component
public class OntologyJsonParser {

    private final ObjectMapper mapper;

    public OntologyJsonParser(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public boolean parseInto(String raw, int chunkIndex, ExtractedOntology target, List<String> warnings) {
        String json = extractJson(raw);
        if (json == null) {
            warnings.add("chunk " + chunkIndex + ": AI answer contains no JSON object, falling back");
            return false;
        }
        JsonNode root;
        try {
            root = mapper.readTree(json);
        } catch (Exception e) {
            warnings.add("chunk " + chunkIndex + ": AI answer is not valid JSON (" + e.getMessage()
                    + "), falling back");
            return false;
        }
        boolean changed = false;
        try {
            changed |= parseClasses(root, target);
            changed |= parseProperties(root, target, true);
            changed |= parseProperties(root, target, false);
            changed |= parseSubclasses(root, target);
            changed |= parseIndividuals(root, target);
            changed |= parseRelations(root, target);
            changed |= parseLiterals(root, target);
        } catch (RuntimeException e) {
            warnings.add("chunk " + chunkIndex + ": could not normalise AI answer ("
                    + e.getMessage() + ")");
        }
        if (!changed) {
            warnings.add("chunk " + chunkIndex + ": AI answer did not contain any usable ontology items");
        }
        return changed;
    }

    private boolean parseClasses(JsonNode root, ExtractedOntology target) {
        boolean changed = false;
        for (JsonNode item : array(root, "classes", "concepts")) {
            String name = text(item, "name", "class", "id");
            String label = text(item, "label", "title", "name");
            String comment = text(item, "comment", "description", "definition");
            if (isEmpty(name)) {
                name = label;
            }
            if (isEmpty(name)) {
                continue;
            }
            String key = name.trim();
            ExtractedClass existing = target.getClasses().get(key);
            if (existing == null) {
                target.getClasses().put(key, new ExtractedClass(key, label, comment));
                changed = true;
            } else {
                if (isEmpty(existing.getLabel())) {
                    existing.setLabel(label);
                }
                if (isEmpty(existing.getComment())) {
                    existing.setComment(comment);
                }
            }
        }
        return changed;
    }

    private boolean parseProperties(JsonNode root, ExtractedOntology target, boolean objectProperty) {
        boolean changed = false;
        String[] names = objectProperty
                ? new String[]{"objectProperties", "object_properties", "relationsProperties"}
                : new String[]{"datatypeProperties", "datatype_properties", "dataProperties", "attributes"};
        for (JsonNode item : array(root, names)) {
            String name = text(item, "name", "property", "id");
            String label = text(item, "label", "title", "name");
            String domain = text(item, "domain", "source");
            String range = text(item, "range", "target");
            String comment = text(item, "comment", "description");
            if (isEmpty(name)) {
                continue;
            }
            String key = name.trim();
            ExtractedProperty property = new ExtractedProperty(key, label, domain, range);
            property.setComment(comment);
            property.setTransitive(Boolean.TRUE.equals(bool(item, "transitive")));
            property.setSymmetric(Boolean.TRUE.equals(bool(item, "symmetric")));
            if (objectProperty) {
                if (!target.getObjectProperties().containsKey(key)) {
                    target.getObjectProperties().put(key, property);
                    changed = true;
                }
            } else if (!target.getDatatypeProperties().containsKey(key)) {
                target.getDatatypeProperties().put(key, property);
                changed = true;
            }
        }
        return changed;
    }

    private boolean parseSubclasses(JsonNode root, ExtractedOntology target) {
        boolean changed = false;
        for (JsonNode item : array(root, "subClassOf", "subclassOf", "subclasses")) {
            String sub = text(item, "sub", "child", "className", "name");
            String superClass = text(item, "superClass", "super", "parent", "parentClass");
            if (item.isTextual()) {
                String value = item.asText();
                int index = value.indexOf('>');
                if (index > 0) {
                    sub = value.substring(0, index).trim();
                    superClass = value.substring(index + 1).trim();
                }
            }
            if (isEmpty(sub) || isEmpty(superClass) || sub.equals(superClass)) {
                continue;
            }
            if (!hasSubclass(target, sub, superClass)) {
                target.getSubClassOf().add(new SubClass(sub.trim(), superClass.trim()));
                changed = true;
            }
        }
        return changed;
    }

    private boolean parseIndividuals(JsonNode root, ExtractedOntology target) {
        boolean changed = false;
        for (JsonNode item : array(root, "individuals", "instances", "entities")) {
            String name = text(item, "name", "id", "label");
            String label = text(item, "label", "name");
            String className = text(item, "class", "className", "type");
            String comment = text(item, "comment", "description");
            if (isEmpty(name)) {
                continue;
            }
            String key = name.trim();
            ExtractedIndividual existing = target.getIndividuals().get(key);
            if (existing == null) {
                ExtractedIndividual individual = new ExtractedIndividual(key, label, className);
                individual.setComment(comment);
                target.getIndividuals().put(key, individual);
                changed = true;
            } else if (isEmpty(existing.getClassName()) && !isEmpty(className)) {
                existing.setClassName(className);
            }
        }
        return changed;
    }

    private boolean parseRelations(JsonNode root, ExtractedOntology target) {
        boolean changed = false;
        for (JsonNode item : array(root, "relations", "objectFacts", "triples")) {
            String subject = text(item, "subject", "s");
            String predicate = text(item, "predicate", "property", "p");
            String object = text(item, "object", "o");
            if (isEmpty(subject) || isEmpty(predicate) || isEmpty(object)) {
                continue;
            }
            if (!hasRelation(target, subject, predicate, object)) {
                target.getRelations().add(new ExtractedRelation(subject.trim(), predicate.trim(), object.trim()));
                changed = true;
            }
        }
        return changed;
    }

    private boolean parseLiterals(JsonNode root, ExtractedOntology target) {
        boolean changed = false;
        for (JsonNode item : array(root, "literals", "dataFacts", "attributes")) {
            String subject = text(item, "subject", "s");
            String predicate = text(item, "predicate", "property", "p");
            String value = text(item, "value", "object", "o");
            String datatype = text(item, "datatype", "type");
            String language = text(item, "language", "lang");
            if (isEmpty(subject) || isEmpty(predicate) || value == null) {
                continue;
            }
            if (!hasLiteral(target, subject, predicate, value)) {
                target.getLiterals().add(new ExtractedLiteral(subject.trim(), predicate.trim(),
                        value, datatype, language));
                changed = true;
            }
        }
        return changed;
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static String extractJson(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        if (value.startsWith("```")) {
            int firstNewline = value.indexOf('\n');
            value = firstNewline >= 0 ? value.substring(firstNewline + 1) : value;
            int fence = value.lastIndexOf("```");
            if (fence >= 0) {
                value = value.substring(0, fence);
            }
        }
        int start = value.indexOf('{');
        int end = value.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return null;
        }
        return value.substring(start, end + 1);
    }

    private static Iterable<JsonNode> array(JsonNode root, String... names) {
        for (String name : names) {
            JsonNode node = root.get(name);
            if (node != null && node.isArray()) {
                return node;
            }
        }
        return java.util.Collections.emptyList();
    }

    private static String text(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode value = node.get(field);
            if (value != null && !value.isNull() && value.isValueNode()) {
                String text = value.asText();
                if (!text.trim().isEmpty()) {
                    return text.trim();
                }
            }
        }
        return null;
    }

    private static Boolean bool(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && value.isBoolean() ? value.asBoolean() : null;
    }

    private static boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static boolean hasSubclass(ExtractedOntology ontology, String sub, String superClass) {
        for (SubClass existing : ontology.getSubClassOf()) {
            if (existing.getSub().equals(sub.trim()) && existing.getSuperClass().equals(superClass.trim())) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasRelation(ExtractedOntology ontology, String subject, String predicate, String object) {
        for (ExtractedRelation existing : ontology.getRelations()) {
            if (existing.getSubject().equals(subject.trim())
                    && existing.getPredicate().equals(predicate.trim())
                    && existing.getObject().equals(object.trim())) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasLiteral(ExtractedOntology ontology, String subject, String predicate, String value) {
        for (ExtractedLiteral existing : ontology.getLiterals()) {
            if (existing.getSubject().equals(subject.trim())
                    && existing.getPredicate().equals(predicate.trim())
                    && existing.getValue().equals(value)) {
                return true;
            }
        }
        return false;
    }
}
