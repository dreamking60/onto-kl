package com.example.ontology.modeling.infrastructure.build;

import com.example.ontology.modeling.domain.ExtractedOntology;
import com.example.ontology.modeling.domain.GeneratedOntology;
import com.example.ontology.modeling.infrastructure.extract.HeuristicOntologyExtractor;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.Test;

import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OntologyTurtleBuilderTest {

    private static final String TEXT = String.join("\n",
            "设备操作手册",
            "第一章 安全须知",
            "第一条 操作人员必须经过培训。",
            "第二条 “急停按钮”是指用于紧急停止设备的按钮。",
            "第二章 启动流程",
            "第三条 按下启动按钮，等待设备自检。");

    private final HeuristicOntologyExtractor extractor = new HeuristicOntologyExtractor();
    private final OntologyTurtleBuilder builder = new OntologyTurtleBuilder();

    @Test
    void generatesValidTboxAndAbox() {
        ExtractedOntology extraction = extractor.extract(TEXT, "manual.txt");
        String namespace = "http://example.org/generated#";
        GeneratedOntology generated = builder.build(extraction, namespace, "manual.txt", true);

        assertTrue(generated.getTboxTriples() > 0);
        assertTrue(generated.getAboxTriples() > 0);
        assertNotNull(generated.getTboxTurtle());
        assertTrue(generated.getTboxTurtle().contains("@prefix"));
        assertTrue(generated.getTboxTurtle().contains("owl:Class"));
        assertTrue(generated.getAboxTurtle().contains("owl:NamedIndividual"));

        Model tbox = ModelFactory.createDefaultModel();
        RDFDataMgr.read(tbox, new StringReader(generated.getTboxTurtle()), null, Lang.TURTLE);
        assertFalse(tbox.isEmpty());
        assertTrue(tbox.contains(null, RDF.type, OWL.Ontology));
        assertTrue(tbox.contains(null, RDF.type, OWL.Class));
        assertTrue(tbox.contains(null, RDF.type, OWL.ObjectProperty));

        Model abox = ModelFactory.createDefaultModel();
        RDFDataMgr.read(abox, new StringReader(generated.getAboxTurtle()), null, Lang.TURTLE);
        assertFalse(abox.isEmpty());
        assertTrue(abox.contains(null, RDF.type, OWL.NS == null ? null : org.apache.jena.rdf.model.ResourceFactory.createResource(OWL.NS + "NamedIndividual")));
    }
}
