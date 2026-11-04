package com.example.ontology.infrastructure.store;

import com.example.ontology.domain.exception.DuplicateModelException;
import com.example.ontology.domain.exception.ModelNotFoundException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** In-memory registry of the ontology models currently loaded in this process. */
@Component
public class ModelRegistry {

    private final Map<String, ModelContext> models = new ConcurrentHashMap<>();

    public void register(ModelContext context) {
        String id = context.getModel().getId();
        ModelContext previous = models.putIfAbsent(id, context);
        if (previous != null) {
            context.close();
            throw new DuplicateModelException(id);
        }
    }

    public ModelContext require(String modelId) {
        ModelContext context = models.get(modelId);
        if (context == null) {
            throw new ModelNotFoundException(modelId);
        }
        return context;
    }

    public boolean exists(String modelId) {
        return models.containsKey(modelId);
    }

    public List<ModelContext> list() {
        List<ModelContext> contexts = new ArrayList<>(models.values());
        contexts.sort(Comparator.comparing(context -> context.getModel().getCreatedAt()));
        return contexts;
    }

    public void remove(String modelId) {
        ModelContext context = models.remove(modelId);
        if (context == null) {
            throw new ModelNotFoundException(modelId);
        }
        context.close();
    }

    public int size() {
        return models.size();
    }
}
