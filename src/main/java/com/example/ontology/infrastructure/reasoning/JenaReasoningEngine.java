package com.example.ontology.infrastructure.reasoning;

import com.example.ontology.domain.OntologyVocabulary;
import com.example.ontology.domain.model.ExplainResult;
import com.example.ontology.domain.model.InferredTriple;
import com.example.ontology.domain.model.ModelStatistics;
import com.example.ontology.infrastructure.rdf.RdfBridge;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.reasoner.Derivation;
import org.apache.jena.reasoner.rulesys.GenericRuleReasoner;
import org.apache.jena.reasoner.rulesys.Rule;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Jena 4.8 reasoning engine for one ontology model.
 *
 * <p>Inference pipeline:</p>
 * <ol>
 *   <li>business rules ({@link GenericRuleReasoner}) are run forward to a fixpoint;</li>
 *   <li>an {@link OntModel} with {@link OntModelSpec#OWL_MEM_MINI_RULE_INF} computes
 *       the RDFS/OWL closure (sub-class, domain/range, inverse, symmetric,
 *       transitive, equivalent class);</li>
 *   <li>the resulting {@link InfModel} answers SPARQL queries and explains
 *       derivations.</li>
 * </ol>
 */
public final class JenaReasoningEngine implements AutoCloseable {

    private final Path ontologyFile;
    private final Path rulesFile;
    private final Model virtualAbox;
    private final Model tbox;
    private final Model asserted;
    /** Namespace of the model vocabulary (derived from the T-Box). */
    private final String namespace;
    /** IRIs that occur as subjects of the (file- or TDSQL-based) A-Box. */
    private final Set<String> individualIris = new HashSet<>();

    private Model ruleClosure;
    private InfModel ruleInfModel;
    private OntModel infModel;
    private List<Rule> rules = Collections.emptyList();
    private volatile boolean reasoned;
    private volatile long reasonMillis;

    public JenaReasoningEngine(Path ontologyFile, Path rulesFile, Model virtualAbox) {
        this.ontologyFile = ontologyFile;
        this.rulesFile = rulesFile;
        this.virtualAbox = virtualAbox;
        this.tbox = RDFDataMgr.loadModel(ontologyFile.toUri().toString());
        this.asserted = ModelFactory.createDefaultModel();
        this.asserted.setNsPrefixes(tbox.getNsPrefixMap());
        this.asserted.add(tbox);
        this.asserted.add(virtualAbox);
        this.ruleClosure = asserted;

        String detectedNamespace = tbox.getNsPrefixURI("");
        if (detectedNamespace == null) {
            detectedNamespace = tbox.getNsPrefixURI(":");
        }
        if (detectedNamespace == null) {
            StmtIterator ontologyIterator = tbox.listStatements(null, RDF.type, OWL.Ontology);
            try {
                if (ontologyIterator.hasNext()) {
                    String uri = ontologyIterator.next().getSubject().getURI();
                    if (uri != null) {
                        detectedNamespace = uri.endsWith("#") ? uri : uri + "#";
                    }
                }
            } finally {
                ontologyIterator.close();
            }
        }
        this.namespace = detectedNamespace == null ? OntologyVocabulary.NS : detectedNamespace;
        StmtIterator subjects = virtualAbox.listStatements();
        try {
            while (subjects.hasNext()) {
                individualIris.add(RdfBridge.termIri(subjects.next().getSubject()));
            }
        } finally {
            subjects.close();
        }
    }

    // ------------------------------------------------------------------
    // Inference pipeline
    // ------------------------------------------------------------------

    public synchronized void computeInference() {
        if (reasoned) {
            return;
        }
        long started = System.currentTimeMillis();
        Model closure = asserted;
        if (rulesFile != null && Files.isRegularFile(rulesFile)) {
            rules = Rule.rulesFromURL(rulesFile.toUri().toString());
            if (!rules.isEmpty()) {
                GenericRuleReasoner reasoner = new GenericRuleReasoner(rules);
                reasoner.setMode(GenericRuleReasoner.FORWARD_RETE);
                ruleInfModel = ModelFactory.createInfModel(reasoner, asserted);
                ruleInfModel.setDerivationLogging(true);
                ruleInfModel.prepare();
                closure = ModelFactory.createDefaultModel();
                closure.setNsPrefixes(asserted.getNsPrefixMap());
                closure.add(ruleInfModel);
            }
        }
        OntModel owlModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MINI_RULE_INF, closure);
        owlModel.setDerivationLogging(true);
        owlModel.prepare();
        this.ruleClosure = closure;
        this.infModel = owlModel;
        this.reasonMillis = System.currentTimeMillis() - started;
        this.reasoned = true;
    }

    public void ensureReasoned() {
        if (!reasoned) {
            computeInference();
        }
    }

    public boolean isReasoned() {
        return reasoned;
    }

    /** SPARQL prologue with the {@code :} prefix bound to this model's namespace. */
    public String prefixes() {
        return "PREFIX :     <" + namespace + ">\n"
                + "PREFIX rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                + "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
                + "PREFIX owl:  <http://www.w3.org/2002/07/owl#>\n"
                + "PREFIX xsd:  <http://www.w3.org/2001/XMLSchema#>\n";
    }

    public String namespace() {
        return namespace;
    }

    public long getReasonMillis() {
        return reasonMillis;
    }

    public int getRuleCount() {
        return rules.size();
    }

    public Path getOntologyFile() {
        return ontologyFile;
    }

    public Path getRulesFile() {
        return rulesFile;
    }

    public Model getVirtualAbox() {
        return virtualAbox;
    }

    // ------------------------------------------------------------------
    // Querying
    // ------------------------------------------------------------------

    public ResultSet select(String sparql) {
        ensureReasoned();
        Query query = QueryFactory.create(prefixes() + sparql);
        try (QueryExecution execution = QueryExecutionFactory.create(query, infModel)) {
            return execution.execSelect().materialise();
        }
    }

    public boolean ask(String sparql) {
        ensureReasoned();
        Query query = QueryFactory.create(prefixes() + sparql);
        try (QueryExecution execution = QueryExecutionFactory.create(query, infModel)) {
            return execution.execAsk();
        }
    }

    public Model construct(String sparql) {
        ensureReasoned();
        Query query = QueryFactory.create(prefixes() + sparql);
        try (QueryExecution execution = QueryExecutionFactory.create(query, infModel)) {
            return execution.execConstruct();
        }
    }

    // ------------------------------------------------------------------
    // Inference results
    // ------------------------------------------------------------------

    /** Application-vocabulary triples implied by the inference model. */
    public List<InferredTriple> inferredTriples() {
        return inferredTriples(Integer.MAX_VALUE);
    }

    public List<InferredTriple> inferredTriples(int limit) {
        ensureReasoned();
        List<InferredTriple> triples = new ArrayList<>();
        StmtIterator iterator = infModel.listStatements();
        try {
            while (iterator.hasNext() && triples.size() < limit) {
                Statement statement = iterator.next();
                if (!isApplicationStatement(statement)) {
                    continue;
                }
                if (!individualIris.contains(RdfBridge.termIri(statement.getSubject()))) {
                    continue;
                }
                if (asserted.contains(statement)) {
                    continue;
                }
                triples.add(RdfBridge.toInferredTriple(statement));
            }
        } finally {
            iterator.close();
        }
        return triples;
    }

    /** Keeps properties of the model vocabulary and rdf:type of its classes. */
    private boolean isApplicationStatement(Statement statement) {
        String predicate = statement.getPredicate().getURI();
        if (RDF.type.getURI().equals(predicate)) {
            RDFNode object = statement.getObject();
            return object.isURIResource() && object.asResource().getURI().startsWith(namespace);
        }
        return predicate.startsWith(namespace);
    }

    /** Sorted ontology types (asserted + inferred) of one individual. */
    public Set<String> types(String subjectIri) {
        ensureReasoned();
        Set<String> types = new TreeSet<>();
        Resource resource = infModel.createResource(subjectIri);
        StmtIterator iterator = infModel.listStatements(resource, RDF.type, (RDFNode) null);
        try {
            while (iterator.hasNext()) {
                RDFNode object = iterator.next().getObject();
                if (object.isURIResource() && object.asResource().getURI().startsWith(namespace)) {
                    types.add(object.toString());
                }
            }
        } finally {
            iterator.close();
        }
        return types;
    }

    // ------------------------------------------------------------------
    // Explanation
    // ------------------------------------------------------------------

    public ExplainResult explain(String modelId, String subjectIri, int limit) {
        ensureReasoned();
        String subject = subjectIri.startsWith(":") ? namespace + subjectIri.substring(1)
                : subjectIri;
        Resource resource = infModel.createResource(subject);
        StringWriter buffer = new StringWriter();
        PrintWriter out = new PrintWriter(buffer);
        int explained = 0;
        StmtIterator iterator = infModel.listStatements(resource, null, (RDFNode) null);
        try {
            while (iterator.hasNext()) {
                Statement statement = iterator.next();
                if (asserted.contains(statement) || !isApplicationStatement(statement)) {
                    continue;
                }
                if (explained >= limit) {
                    out.println("... (stopped after " + limit + " inferred statements)");
                    break;
                }
                out.println(statement);
                boolean printed = printDerivations(ruleInfModel, statement, out);
                if (!printed) {
                    printed = printDerivations(infModel, statement, out);
                }
                if (!printed) {
                    out.println("    (no derivation trace available for this statement)");
                }
                explained++;
            }
        } finally {
            iterator.close();
        }
        if (explained == 0) {
            out.println("(nothing is inferred about " + subject + ")");
        }
        out.flush();
        return new ExplainResult(modelId, subject, types(subject), buffer.toString(), explained);
    }

    private static boolean printDerivations(InfModel model, Statement statement, PrintWriter out) {
        if (model == null) {
            return false;
        }
        Iterator<Derivation> derivations;
        try {
            derivations = model.getDerivation(statement);
        } catch (RuntimeException e) {
            return false;
        }
        if (derivations == null || !derivations.hasNext()) {
            return false;
        }
        boolean printed = false;
        while (derivations.hasNext()) {
            Derivation derivation = derivations.next();
            PrintWriter writer = new PrintWriter(out);
            derivation.printTrace(writer, true);
            writer.flush();
            printed = true;
        }
        return printed;
    }

    // ------------------------------------------------------------------
    // Statistics
    // ------------------------------------------------------------------

    public ModelStatistics statistics() {
        ModelStatistics statistics = new ModelStatistics();
        statistics.setTboxTriples(tbox.size());
        statistics.setVirtualAboxTriples(virtualAbox.size());
        statistics.setAssertedTriples(asserted.size());
        Model closure = reasoned && ruleClosure != null ? ruleClosure : asserted;
        statistics.setAfterBusinessRulesTriples(closure.size());
        statistics.setInferenceModelTriples(reasoned && infModel != null ? infModel.size() : asserted.size());
        statistics.setReasonMillis(reasonMillis);
        if (reasoned) {
            List<InferredTriple> inferred = inferredTriples();
            statistics.setInferredApplicationTriples(inferred.size());
            long allDerived = Math.max(0L, infModel.size() - closure.size());
            statistics.setInferredTriples(allDerived);
        }
        return statistics;
    }

    @Override
    public void close() {
        try {
            if (infModel != null) {
                infModel.close();
            }
        } catch (RuntimeException ignored) {
            // best effort
        }
        try {
            asserted.close();
        } catch (RuntimeException ignored) {
            // best effort
        }
    }
}
