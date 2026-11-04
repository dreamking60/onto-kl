package com.example.ontology.infrastructure.loader;

import com.example.ontology.config.AppProperties;
import com.example.ontology.domain.exception.InvalidModelException;
import com.example.ontology.domain.model.DbConfig;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Builds the JDBC configuration of one model from the application defaults and
 * the per-request overrides.
 *
 * <ul>
 *   <li>{@code tdsql}: the production profile, connects to the configured TDSQL;</li>
 *   <li>{@code h2}: an in-memory H2 database in MySQL compatibility mode, a new
 *       one per model load (zero-install demos and tests).</li>
 * </ul>
 */
@Component
public class DbConfigResolver {

    private final AppProperties properties;

    public DbConfigResolver(AppProperties properties) {
        this.properties = properties;
    }

    public DbConfig resolve(String modelId, String profile, String urlOverride,
                            String usernameOverride, String passwordOverride, String driverOverride) {
        String selected = (profile == null || profile.trim().isEmpty())
                ? properties.getDefaultProfile()
                : profile.trim().toLowerCase();
        if (!"tdsql".equals(selected) && !"h2".equals(selected)) {
            throw new InvalidModelException("Unknown data source profile '" + profile
                    + "' (expected tdsql or h2)");
        }
        if ("h2".equals(selected)) {
            AppProperties.H2 defaults = properties.getDatasource().getH2();
            String url = isBlank(urlOverride) ? defaults.getUrlTemplate()
                    .replace("{modelId}", sanitize(modelId) + "_" + randomSuffix()) : urlOverride;
            return new DbConfig("h2", valueOrDefault(driverOverride, defaults.getDriver()), url,
                    valueOrDefault(usernameOverride, defaults.getUsername()),
                    valueOrDefault(passwordOverride, defaults.getPassword()));
        }
        AppProperties.Tdsql defaults = properties.getDatasource().getTdsql();
        String url = valueOrDefault(urlOverride, defaults.getUrl());
        if (isBlank(url)) {
            throw new InvalidModelException("No TDSQL JDBC URL configured (app.datasource.tdsql.url)");
        }
        return new DbConfig("tdsql", valueOrDefault(driverOverride, defaults.getDriver()), url,
                valueOrDefault(usernameOverride, defaults.getUsername()),
                valueOrDefault(passwordOverride, defaults.getPassword()));
    }

    private static String sanitize(String modelId) {
        return modelId.replaceAll("[^A-Za-z0-9]", "_");
    }

    private static String randomSuffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String valueOrDefault(String value, String defaultValue) {
        return isBlank(value) ? defaultValue : value;
    }
}
