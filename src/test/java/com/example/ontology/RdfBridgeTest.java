package com.example.ontology;

import com.example.ontology.domain.model.InferredTriple;
import com.example.ontology.domain.model.TripleObjectKind;
import com.example.ontology.infrastructure.rdf.RdfBridge;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Unit tests for the RDF4J &harr; Jena bridge. */
class RdfBridgeTest {

    private static final ValueFactory VF = SimpleValueFactory.getInstance();

    @Test
    void convertsPlainLiteral() {
        Model model = ModelFactory.createDefaultModel();
        RdfBridge.addStatement(model, VF.createStatement(
                VF.createIRI("http://example.org/company#emp/1"),
                VF.createIRI("http://example.org/company#name"),
                VF.createLiteral("Alice")));
        Statement statement = model.getResource("http://example.org/company#emp/1")
                .getProperty(model.createProperty("http://example.org/company#name"));
        assertEquals("Alice", statement.getLiteral().getLexicalForm());
        assertEquals(XSD.STRING.stringValue(), statement.getLiteral().getDatatypeURI());
    }

    @Test
    void convertsTypedAndLanguageLiterals() {
        Model model = ModelFactory.createDefaultModel();
        RdfBridge.addStatement(model, VF.createStatement(
                VF.createIRI("http://example.org/company#emp/2"),
                VF.createIRI("http://example.org/company#salary"),
                VF.createLiteral("42000.00", XSD.DECIMAL)));
        RdfBridge.addStatement(model, VF.createStatement(
                VF.createIRI("http://example.org/company#emp/2"),
                VF.createIRI("http://example.org/company#name"),
                VF.createLiteral("Bob", "en")));

        Statement salary = model.getResource("http://example.org/company#emp/2")
                .getProperty(model.createProperty("http://example.org/company#salary"));
        assertEquals(XSD.DECIMAL.stringValue(), salary.getLiteral().getDatatypeURI());

        Statement name = model.getResource("http://example.org/company#emp/2")
                .getProperty(model.createProperty("http://example.org/company#name"));
        assertEquals("en", name.getLiteral().getLanguage());
    }

    @Test
    void convertsIriObjectToDomainTriple() {
        Model model = ModelFactory.createDefaultModel();
        Statement statement = model.createStatement(
                model.createResource("http://example.org/company#emp/5"),
                model.createProperty("http://example.org/company#reportsTo"),
                model.createResource("http://example.org/company#emp/3"));
        InferredTriple triple = RdfBridge.toInferredTriple(statement);
        assertEquals(TripleObjectKind.IRI, triple.getObjectKind());
        assertEquals("http://example.org/company#emp/3", triple.getObject());
        assertEquals("http://example.org/company#reportsTo", triple.getPredicate());
    }

    @Test
    void convertsLiteralToDomainTriple() {
        Model model = ModelFactory.createDefaultModel();
        Statement statement = model.createStatement(
                model.createResource("http://example.org/company#emp/2"),
                model.createProperty("http://example.org/company#salary"),
                model.createTypedLiteral("31000.00", org.apache.jena.vocabulary.XSD.decimal.getURI()));
        InferredTriple triple = RdfBridge.toInferredTriple(statement);
        assertEquals(TripleObjectKind.LITERAL, triple.getObjectKind());
        assertEquals("31000.00", triple.getObject());
        assertTrue(triple.getDatatype().endsWith("#decimal"));
    }
}
