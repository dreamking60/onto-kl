package com.example.ontology.infrastructure.store;

import com.example.ontology.domain.model.OntologyModel;
import com.example.ontology.infrastructure.db.InstanceDatabase;
import com.example.ontology.infrastructure.ontop.OntopVirtualGraph;
import com.example.ontology.infrastructure.reasoning.JenaReasoningEngine;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Runtime handles of one loaded model (Ontop repository, Jena engine, JDBC
 * control connection) plus the domain aggregate.
 */
public final class ModelContext implements AutoCloseable {

    private final OntologyModel model;
    /** null when the A-Box is file-based (no TDSQL/Ontop involved). */
    private final InstanceDatabase database;
    /** null when the A-Box is file-based. */
    private final OntopVirtualGraph ontop;
    private final JenaReasoningEngine engine;
    private final ReentrantLock lock = new ReentrantLock();

    public ModelContext(OntologyModel model, InstanceDatabase database,
                        OntopVirtualGraph ontop, JenaReasoningEngine engine) {
        this.model = model;
        this.database = database;
        this.ontop = ontop;
        this.engine = engine;
    }

    public OntologyModel getModel() {
        return model;
    }

    public InstanceDatabase getDatabase() {
        return database;
    }

    public OntopVirtualGraph getOntop() {
        return ontop;
    }

    public JenaReasoningEngine getEngine() {
        return engine;
    }

    public ReentrantLock getLock() {
        return lock;
    }

    /** True when the A-Box comes from TDSQL through Ontop. */
    public boolean isTdsqlBacked() {
        return database != null && ontop != null;
    }

    @Override
    public void close() {
        try {
            engine.close();
        } catch (RuntimeException ignored) {
            // best effort
        }
        if (ontop != null) {
            try {
                ontop.close();
            } catch (RuntimeException ignored) {
                // best effort
            }
        }
        if (database != null) {
            database.close();
        }
    }
}
