package com.example.ontology.modeling.infrastructure.extract;

import com.example.ontology.modeling.domain.ExtractedOntology;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OntologyJsonParserTest {

    private final OntologyJsonParser parser = new OntologyJsonParser(new ObjectMapper());

    @Test
    void parsesFencedJsonAndAlternativeKeys() {
        String raw = "```json\n"
                + "{\"classes\":[{\"name\":\"Contract\",\"label\":\"合同\"},{\"label\":\"仅标签\"}],"
                + " \"object_properties\":[{\"name\":\"partOf\",\"domain\":\"Contract\",\"range\":\"Contract\"}],"
                + " \"dataProperties\":[{\"name\":\"hasTitle\",\"domain\":\"Contract\",\"range\":\"xsd:string\"}],"
                + " \"subclasses\":[{\"child\":\"SalesContract\",\"parent\":\"Contract\"}],"
                + " \"instances\":[{\"name\":\"c1\",\"class\":\"Contract\"}],"
                + " \"relations\":[{\"subject\":\"c1\",\"predicate\":\"partOf\",\"object\":\"c1\"}],"
                + " \"literals\":[{\"subject\":\"c1\",\"predicate\":\"hasTitle\",\"value\":\"合同一\"}]}\n```";
        ExtractedOntology ontology = new ExtractedOntology();
        List<String> warnings = new ArrayList<>();
        assertTrue(parser.parseInto(raw, 1, ontology, warnings));
        assertEquals(2, ontology.getClasses().size());
        assertTrue(ontology.getObjectProperties().containsKey("partOf"));
        assertTrue(ontology.getDatatypeProperties().containsKey("hasTitle"));
        assertEquals(1, ontology.getSubClassOf().size());
        assertEquals("Contract", ontology.getSubClassOf().get(0).getSuperClass());
        assertTrue(ontology.getIndividuals().containsKey("c1"));
        assertEquals(1, ontology.getRelations().size());
        assertEquals(1, ontology.getLiterals().size());
        assertTrue(warnings.isEmpty());
    }

    @Test
    void reportsInvalidJson() {
        ExtractedOntology ontology = new ExtractedOntology();
        List<String> warnings = new ArrayList<>();
        assertFalse(parser.parseInto("I cannot answer that.", 1, ontology, warnings));
        assertFalse(warnings.isEmpty());
        assertTrue(ontology.getClasses().isEmpty());
    }
}
