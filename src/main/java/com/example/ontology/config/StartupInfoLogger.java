package com.example.ontology.config;

import com.example.ontology.infrastructure.store.ModelFileStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.List;

/** Logs the useful entry points once the application is ready. */
@Component
public class StartupInfoLogger implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StartupInfoLogger.class);

    private final AppProperties properties;
    private final ModelFileStore fileStore;
    private final Environment environment;

    public StartupInfoLogger(AppProperties properties, ModelFileStore fileStore, Environment environment) {
        this.properties = properties;
        this.fileStore = fileStore;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        String port = environment.getProperty("local.server.port",
                environment.getProperty("server.port", "8080"));
        log.info("");
        log.info("Ontology Reasoning Server is ready");
        log.info("  REST base       : http://localhost:{}", port);
        log.info("  Web UI          : http://localhost:{}/", port);
        log.info("  Swagger UI      : http://localhost:{}/swagger-ui.html", port);
        log.info("  data directory  : {}", properties.dataDirPath());
        log.info("  default profile : {}", properties.getDefaultProfile());
        log.info("  stored models   : {}", storedModels());
        log.info("  POST /load {{\"modelId\":\"company\"}}  ->  POST /reason  ->  POST /query");
        log.info("");
    }

    private String storedModels() {
        List<String> ids = fileStore.listStoredModelIds();
        if (ids.isEmpty()) {
            return "none (call POST /load)";
        }
        return ids.size() + " on disk " + ids + " (POST /load to register)";
    }
}
