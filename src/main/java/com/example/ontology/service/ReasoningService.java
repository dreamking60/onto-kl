package com.example.ontology.service;

import com.example.ontology.config.AppProperties;
import com.example.ontology.domain.model.ExplainResult;
import com.example.ontology.domain.model.InferredTriple;
import com.example.ontology.domain.model.ModelState;
import com.example.ontology.domain.model.ModelStatistics;
import com.example.ontology.domain.model.OntologyModel;
import com.example.ontology.dto.request.ReasonRequest;
import com.example.ontology.dto.response.ExplainResponse;
import com.example.ontology.dto.response.InferredTripleDto;
import com.example.ontology.dto.response.ReasoningResponse;
import com.example.ontology.dto.response.StatisticsDto;
import com.example.ontology.infrastructure.store.ModelContext;
import com.example.ontology.infrastructure.store.ModelRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Use cases behind {@code /reason}: compute the inference closure of a loaded
 * model (business rules + OWL entailment), optionally persist the derived
 * triples into the relational store and explain derivations.
 */
@Service
public class ReasoningService {

    private static final Logger log = LoggerFactory.getLogger(ReasoningService.class);

    private final ModelRegistry registry;
    private final AppProperties properties;

    public ReasoningService(ModelRegistry registry, AppProperties properties) {
        this.registry = registry;
        this.properties = properties;
    }

    public ReasoningResponse reason(ReasonRequest request) {
        ModelContext context = registry.require(request.getModelId());
        // File-based models have no TDSQL table to write to; skip materialisation.
        boolean materializeEnabled = properties.getReasoning().isMaterializeEnabled()
                && context.getDatabase() != null;
        boolean materialize = request.getMaterialize() == null
                ? materializeEnabled
                : request.getMaterialize() && materializeEnabled;
        int inferredLimit = request.getInferredLimit() == null
                ? properties.getMaxInferredSample()
                : request.getInferredLimit();

        long started = System.currentTimeMillis();
        context.getLock().lock();
        try {
            boolean computed = !context.getEngine().isReasoned();
            if (computed) {
                log.info("Computing inference closure for model '{}'", request.getModelId());
            }
            context.getEngine().ensureReasoned();
            refreshStatistics(context);
            OntologyModel model = context.getModel();
            model.setState(ModelState.REASONED);
            model.setReasonedAt(Instant.now());

            int materialized = 0;
            if (materialize) {
                List<InferredTriple> inferred = context.getEngine().inferredTriples();
                materialized = context.getDatabase().replaceInferred(inferred);
                model.setMaterializedTriples(materialized);
                model.setMaterializedAt(Instant.now());
                log.info("Materialised {} inferred triples for model '{}' into table '{}'",
                        materialized, request.getModelId(),
                        properties.getReasoning().getMaterializeTable());
            }

            List<InferredTriple> inferred = context.getEngine().inferredTriples(inferredLimit);
            ReasoningResponse response = new ReasoningResponse();
            response.setModelId(model.getId());
            response.setState(model.getState().name());
            response.setComputed(computed);
            response.setDurationMs(System.currentTimeMillis() - started);
            response.setMaterializeEnabled(materializeEnabled);
            response.setMaterializedTriples(materialized);
            response.setInferredTotal(model.getStatistics().getInferredApplicationTriples());
            response.setInferredReturned(inferred.size());
            response.setInferredTriples(inferred.stream()
                    .map(InferredTripleDto::from)
                    .collect(Collectors.toList()));
            response.setReasonedAt(model.getReasonedAt());
            response.setStatistics(StatisticsDto.from(model.getStatistics()));
            return response;
        } finally {
            context.getLock().unlock();
        }
    }

    /** Computes the closure if necessary; used by query/explain as well. */
    public void ensureReasoned(ModelContext context) {
        context.getLock().lock();
        try {
            if (!context.getEngine().isReasoned()) {
                context.getEngine().computeInference();
                refreshStatistics(context);
                OntologyModel model = context.getModel();
                model.setState(ModelState.REASONED);
                model.setReasonedAt(Instant.now());
            }
        } finally {
            context.getLock().unlock();
        }
    }

    public ExplainResponse explain(String modelId, String subject, int limit) {
        ModelContext context = registry.require(modelId);
        ensureReasoned(context);
        ExplainResult result = context.getEngine().explain(modelId, subject, limit);
        return ExplainResponse.from(result);
    }

    private void refreshStatistics(ModelContext context) {
        ModelStatistics previous = context.getModel().getStatistics();
        ModelStatistics statistics = context.getEngine().statistics();
        if (previous != null) {
            statistics.setOntology(previous.getOntology());
            statistics.setLoadMillis(previous.getLoadMillis());
        }
        statistics.setReasonMillis(context.getEngine().getReasonMillis());
        context.getModel().setStatistics(statistics);
    }
}
