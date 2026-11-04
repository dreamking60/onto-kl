package com.example.ontology.infrastructure.loader;

import com.example.ontology.domain.exception.InvalidModelException;
import com.example.ontology.domain.model.OntologySummary;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDFS;

import java.nio.file.Path;

/**
 * Parses and validates a local ontology file (T-Box) with Jena and computes a
 * structural summary.
 */
public final class OntologyAnalyzer {

    private OntologyAnalyzer() {
    }

    public static OntologySummary analyze(Path ontologyFile) {
        Model model = ModelFactory.createDefaultModel();
        try {
            RDFDataMgr.read(model, ontologyFile.toUri().toString());
        } catch (RuntimeException e) {
            throw new InvalidModelException("Cannot parse ontology file " + ontologyFile
                    + ": " + e.getMessage(), e);
        }
        if (model.isEmpty()) {
            throw new InvalidModelException("Ontology file is empty: " + ontologyFile);
        }
        OntModel ontology = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM, model);
        int classes = countNamedClasses(ontology);
        int objectProperties = countProperties(ontology.listObjectProperties());
        int datatypeProperties = countProperties(ontology.listDatatypeProperties());
        int individuals = countIndividuals(ontology);
        return new OntologySummary(model.size(), classes, objectProperties, datatypeProperties, individuals);
    }

    private static int countNamedClasses(OntModel ontology) {
        int count = 0;
        ExtendedIterator<OntClass> iterator = ontology.listNamedClasses();
        try {
            while (iterator.hasNext()) {
                OntClass cls = iterator.next();
                String uri = cls.getURI();
                if (uri != null && !OWL.Thing.getURI().equals(uri) && !RDFS.Resource.getURI().equals(uri)) {
                    count++;
                }
            }
        } finally {
            iterator.close();
        }
        return count;
    }

    private static int countProperties(ExtendedIterator<? extends OntProperty> iterator) {
        int count = 0;
        try {
            while (iterator.hasNext()) {
                OntProperty property = iterator.next();
                if (property.getURI() != null) {
                    count++;
                }
            }
        } finally {
            iterator.close();
        }
        return count;
    }

    private static int countIndividuals(OntModel ontology) {
        int count = 0;
        ExtendedIterator<?> iterator = ontology.listIndividuals();
        try {
            while (iterator.hasNext()) {
                iterator.next();
                count++;
            }
        } finally {
            iterator.close();
        }
        return count;
    }
}
