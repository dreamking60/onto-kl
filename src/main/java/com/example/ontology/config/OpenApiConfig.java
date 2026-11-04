package com.example.ontology.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Swagger UI / OpenAPI metadata: http://localhost:8080/swagger-ui.html */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI ontologyOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Ontology Reasoning API")
                .version("1.0.0")
                .description("Ontology-based data access and reasoning over TDSQL: "
                        + "load a local T-Box + OBDA mapping, reason with Jena 4.8, "
                        + "query the inference closure with SPARQL."));
    }
}
