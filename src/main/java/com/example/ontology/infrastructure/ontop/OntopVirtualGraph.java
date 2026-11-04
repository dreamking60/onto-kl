package com.example.ontology.infrastructure.ontop;

import com.example.ontology.domain.exception.ModelLoadException;
import com.example.ontology.domain.model.DbConfig;
import com.example.ontology.infrastructure.rdf.RdfBridge;
import it.unibz.inf.ontop.injection.OntopSQLOWLAPIConfiguration;
import it.unibz.inf.ontop.rdf4j.repository.OntopRepository;
import it.unibz.inf.ontop.rdf4j.repository.OntopRepositoryConnection;
import it.unibz.inf.ontop.rdf4j.repository.impl.OntopVirtualRepository;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;

import java.nio.file.Path;

/**
 * Ontop virtual knowledge graph over TDSQL.
 *
 * <p>Ontop is embedded in the process through its RDF4J binding.  The ontology
 * (local Turtle file) and the OBDA mapping (local .obda file) are read from the
 * filesystem; the instances stay in the relational database and are queried
 * through SQL.  Nothing is copied into a triple store by this class.</p>
 */
public final class OntopVirtualGraph implements AutoCloseable {

    private final OntopVirtualRepository repository;

    private OntopVirtualGraph(OntopVirtualRepository repository) {
        this.repository = repository;
    }

    public static OntopVirtualGraph open(DbConfig dbConfig, Path ontologyFile, Path mappingFile) {
        OntopSQLOWLAPIConfiguration.Builder<?> builder = OntopSQLOWLAPIConfiguration.defaultBuilder();
        builder = builder.ontologyFile(ontologyFile.toFile());
        builder = builder.nativeOntopMappingFile(mappingFile.toFile());
        builder = builder.jdbcUrl(dbConfig.getUrl());
        builder = builder.jdbcUser(dbConfig.getUsername());
        builder = builder.jdbcPassword(dbConfig.getPassword());
        builder = builder.jdbcDriver(dbConfig.getDriver());
        builder = builder.keepPermanentDBConnection(true);
        builder = builder.enableOntologyAnnotationQuerying(false);
        OntopSQLOWLAPIConfiguration configuration = builder.build();

        OntopVirtualRepository repository = OntopRepository.defaultRepository(configuration);
        try {
            repository.init();
        } catch (RuntimeException e) {
            throw new ModelLoadException("Failed to initialise the Ontop virtual graph for database "
                    + dbConfig.getUrl() + ": " + rootMessage(e), e);
        }
        return new OntopVirtualGraph(repository);
    }

    private static String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return message == null ? current.getClass().getSimpleName() : message;
    }

    /**
     * Materialises the complete virtual A-Box (everything implied by the OBDA
     * mapping, including Ontop's OWL 2 QL mapping saturation) into a Jena model.
     * The data is read live from TDSQL and is not stored permanently.
     */
    public Model loadAllTriples() {
        try {
            Model model = constructAllTriples();
            if (!model.isEmpty()) {
                return model;
            }
        } catch (RuntimeException e) {
            // fall through to the SELECT-based dump below
        }
        return selectAllTriples();
    }

    private Model constructAllTriples() {
        String sparql = "CONSTRUCT { ?s ?p ?o } WHERE { ?s ?p ?o }";
        Model model = ModelFactory.createDefaultModel();
        try (OntopRepositoryConnection connection = repository.getConnection()) {
            GraphQuery query = connection.prepareGraphQuery(QueryLanguage.SPARQL, sparql);
            try (GraphQueryResult result = query.evaluate()) {
                while (result.hasNext()) {
                    RdfBridge.addStatement(model, result.next());
                }
            }
        }
        return model;
    }

    private Model selectAllTriples() {
        String sparql = "SELECT ?s ?p ?o WHERE { ?s ?p ?o }";
        Model model = ModelFactory.createDefaultModel();
        try (OntopRepositoryConnection connection = repository.getConnection()) {
            TupleQuery query = connection.prepareTupleQuery(QueryLanguage.SPARQL, sparql);
            try (TupleQueryResult result = query.evaluate()) {
                while (result.hasNext()) {
                    BindingSet bindings = result.next();
                    Value subject = bindings.getValue("s");
                    Value predicate = bindings.getValue("p");
                    Value object = bindings.getValue("o");
                    if (subject == null || predicate == null || object == null) {
                        continue;
                    }
                    model.add(RdfBridge.toResource(model, (org.eclipse.rdf4j.model.Resource) subject),
                            model.createProperty(predicate.stringValue()),
                            RdfBridge.toRdfNode(model, object));
                }
            }
        }
        return model;
    }

    @Override
    public void close() {
        repository.close();
    }
}
