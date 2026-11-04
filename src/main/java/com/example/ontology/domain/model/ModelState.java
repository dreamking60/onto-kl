package com.example.ontology.domain.model;

/** Lifecycle of a loaded ontology model. */
public enum ModelState {
    /** Ontology/mapping files are stored, runtime not initialised yet. */
    REGISTERED,
    /** Ontop virtual graph is connected and the virtual A-Box has been loaded. */
    LOADED,
    /** The Jena inference closure has been computed. */
    REASONED,
    /** Loading or reasoning failed. */
    FAILED
}
