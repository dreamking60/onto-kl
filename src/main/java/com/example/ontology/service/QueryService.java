package com.example.ontology.service;

import com.example.ontology.config.AppProperties;
import com.example.ontology.domain.exception.InvalidQueryException;
import com.example.ontology.dto.response.QueryResponse;
import com.example.ontology.infrastructure.store.ModelContext;
import com.example.ontology.infrastructure.store.ModelRegistry;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.springframework.stereotype.Service;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Use cases behind {@code /query}: execute SPARQL over the inference closure of
 * a loaded model.
 */
@Service
public class QueryService {

    private final ModelRegistry registry;
    private final ReasoningService reasoningService;
    private final AppProperties properties;

    public QueryService(ModelRegistry registry, ReasoningService reasoningService, AppProperties properties) {
        this.registry = registry;
        this.reasoningService = reasoningService;
        this.properties = properties;
    }

    public QueryResponse query(String modelId, String sparql, Integer limit) {
        ModelContext context = registry.require(modelId);
        reasoningService.ensureReasoned(context);

        int effectiveLimit = limit == null ? properties.getMaxQueryRows() : limit;
        Query query;
        try {
            query = QueryFactory.create(context.getEngine().prefixes() + sparql);
        } catch (QueryParseException e) {
            throw new InvalidQueryException("Invalid SPARQL query: " + e.getMessage(), e);
        }

        long started = System.currentTimeMillis();
        QueryResponse response = new QueryResponse();
        response.setModelId(modelId);
        try {
            if (query.isSelectType()) {
                executeSelect(context, sparql, effectiveLimit, response);
            } else if (query.isAskType()) {
                response.setQueryType("ASK");
                response.setBooleanResult(context.getEngine().ask(sparql));
                response.setRowCount(1);
                response.setColumns(List.of("boolean"));
            } else if (query.isConstructType() || query.isDescribeType()) {
                response.setQueryType(query.isDescribeType() ? "DESCRIBE" : "CONSTRUCT");
                executeConstruct(context, sparql, effectiveLimit, response);
            } else {
                throw new InvalidQueryException("Unsupported query type: only SELECT, ASK, CONSTRUCT "
                        + "and DESCRIBE are supported");
            }
        } finally {
            response.setDurationMs(System.currentTimeMillis() - started);
        }
        return response;
    }

    private void executeSelect(ModelContext context, String sparql, int limit, QueryResponse response) {
        ResultSet resultSet = context.getEngine().select(sparql);
        List<String> columns = resultSet.getResultVars();
        List<List<String>> rows = new ArrayList<>();
        boolean truncated = false;
        while (resultSet.hasNext()) {
            if (rows.size() >= limit) {
                truncated = true;
                break;
            }
            org.apache.jena.query.QuerySolution solution = resultSet.next();
            List<String> row = new ArrayList<>(columns.size());
            for (String column : columns) {
                row.add(format(solution.get(column)));
            }
            rows.add(row);
        }
        response.setQueryType("SELECT");
        response.setColumns(columns);
        response.setRows(rows);
        response.setRowCount(rows.size());
        response.setTruncated(truncated);
    }

    private void executeConstruct(ModelContext context, String sparql, int limit, QueryResponse response) {
        Model result = context.getEngine().construct(sparql);
        Model limited = ModelFactory.createDefaultModel();
        long total = result.size();
        StmtIterator iterator = result.listStatements();
        int copied = 0;
        try {
            while (iterator.hasNext() && copied < limit) {
                Statement statement = iterator.next();
                limited.add(statement);
                copied++;
            }
        } finally {
            iterator.close();
        }
        StringWriter writer = new StringWriter();
        RDFDataMgr.write(writer, limited, Lang.TURTLE);
        response.setConstructTurtle(writer.toString());
        response.setConstructTriples(total);
        response.setRowCount(copied);
        response.setTruncated(total > copied);
    }

    private static String format(RDFNode node) {
        if (node == null) {
            return "";
        }
        if (node.isURIResource()) {
            return node.asResource().getURI();
        }
        if (node.isAnon()) {
            return "_:" + node.asResource().getId().getLabelString();
        }
        Literal literal = node.asLiteral();
        String lexical = literal.getLexicalForm();
        if (literal.getLanguage() != null && !literal.getLanguage().isEmpty()) {
            return "\"" + lexical + "\"@" + literal.getLanguage();
        }
        String datatype = literal.getDatatypeURI();
        if (datatype == null || "http://www.w3.org/2001/XMLSchema#string".equals(datatype)) {
            return lexical;
        }
        return lexical + "^^<" + datatype + ">";
    }
}
