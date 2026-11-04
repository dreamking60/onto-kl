package com.example.ontology.domain;

/** Constants of the demo company ontology (shared by services and infrastructure). */
public final class OntologyVocabulary {

    public static final String NS = "http://example.org/company#";

    public static final String PREFIXES =
            "PREFIX :     <" + NS + ">\n"
                    + "PREFIX rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                    + "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
                    + "PREFIX owl:  <http://www.w3.org/2002/07/owl#>\n"
                    + "PREFIX xsd:  <http://www.w3.org/2001/XMLSchema#>\n";

    private OntologyVocabulary() {
    }

    /** True for IRIs that identify A-Box individuals created by the OBDA mapping. */
    public static boolean isInstanceIri(String iri) {
        return iri != null && (iri.startsWith(NS + "emp/")
                || iri.startsWith(NS + "dept/")
                || iri.startsWith(NS + "proj/"));
    }
}
