package com.example.ontology;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Ontology Reasoning Server (Jena 4.8 + Ontop 5.5 + TDSQL).
 *
 * <p>Run this class from IntelliJ IDEA or with
 * {@code java -jar target/ontology-reasoning-server.jar}.</p>
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class OntologyApplication {

    public static void main(String[] args) {
        SpringApplication.run(OntologyApplication.class, args);
    }
}
