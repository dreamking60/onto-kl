package com.example.ontology.infrastructure.rdf;

import com.example.ontology.domain.model.InferredTriple;
import com.example.ontology.domain.model.TripleObjectKind;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.rdf.model.AnonId;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.XSD;

import java.util.Optional;

/**
 * Bridges Ontop's RDF4J world and Jena's RDF model world.
 *
 * <p>Ontop exposes TDSQL rows as RDF4J values; Jena only understands its own
 * {@link RDFNode} types, therefore every triple crossing the boundary is
 * converted here.</p>
 */
public final class RdfBridge {

    private RdfBridge() {
    }

    // ------------------------------------------------------------------
    // RDF4J -> Jena
    // ------------------------------------------------------------------

    public static void addStatement(Model model, org.eclipse.rdf4j.model.Statement statement) {
        model.add(toResource(model, statement.getSubject()),
                model.createProperty(statement.getPredicate().stringValue()),
                toRdfNode(model, statement.getObject()));
    }

    public static Resource toResource(Model model, org.eclipse.rdf4j.model.Resource resource) {
        if (resource instanceof org.eclipse.rdf4j.model.BNode) {
            return model.createResource(new AnonId(((org.eclipse.rdf4j.model.BNode) resource).getID()));
        }
        return model.createResource(resource.stringValue());
    }

    public static RDFNode toRdfNode(Model model, org.eclipse.rdf4j.model.Value value) {
        if (value instanceof org.eclipse.rdf4j.model.IRI) {
            return model.createResource(value.stringValue());
        }
        if (value instanceof org.eclipse.rdf4j.model.BNode) {
            return model.createResource(new AnonId(((org.eclipse.rdf4j.model.BNode) value).getID()));
        }
        org.eclipse.rdf4j.model.Literal literal = (org.eclipse.rdf4j.model.Literal) value;
        String label = literal.getLabel();
        Optional<String> language = literal.getLanguage();
        if (language.isPresent() && !language.get().isEmpty()) {
            return model.createLiteral(label, language.get());
        }
        String datatype = literal.getDatatype().stringValue();
        if (XSD.xstring.getURI().equals(datatype)) {
            return model.createLiteral(label);
        }
        RDFDatatype rdfDatatype = TypeMapper.getInstance().getSafeTypeByName(datatype);
        return model.createTypedLiteral(label, rdfDatatype);
    }

    // ------------------------------------------------------------------
    // Jena -> domain
    // ------------------------------------------------------------------

    public static InferredTriple toInferredTriple(Statement statement) {
        String subject = termIri(statement.getSubject());
        String predicate = statement.getPredicate().getURI();
        RDFNode object = statement.getObject();
        if (object.isURIResource()) {
            return new InferredTriple(subject, predicate, object.asResource().getURI(),
                    TripleObjectKind.IRI, null, null);
        }
        if (object.isAnon()) {
            return new InferredTriple(subject, predicate,
                    "_:" + object.asResource().getId().getLabelString(),
                    TripleObjectKind.BLANK, null, null);
        }
        org.apache.jena.rdf.model.Literal literal = object.asLiteral();
        return new InferredTriple(subject, predicate, literal.getLexicalForm(),
                TripleObjectKind.LITERAL, literal.getDatatypeURI(), literal.getLanguage());
    }

    public static String termIri(Resource resource) {
        return resource.isURIResource()
                ? resource.getURI()
                : "_:" + resource.getId().getLabelString();
    }
}
