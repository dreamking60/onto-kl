package com.example.ontology.dto.request;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * Request body of {@code POST /load}.
 *
 * <p>The ontology, mapping and rules can be supplied either as a local file
 * path ({@code *Path}) or inline content ({@code *Content}).  When none of them
 * is given, the demo files configured in {@code app.defaults.*} are used.</p>
 */
public class LoadModelRequest {

    @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9_-]{0,63}$",
            message = "modelId must start with a letter/digit and contain only letters, digits, '_' or '-'")
    private String modelId;

    @Size(max = 200)
    private String name;

    @Size(max = 2000)
    private String description;

    private String ontologyPath;
    private String ontologyContent;
    private String mappingPath;
    private String mappingContent;
    private String rulesPath;
    private String rulesContent;

    /** Optional local A-Box instances file (Turtle). Used when no OBDA mapping is given. */
    private String instancesPath;
    private String instancesContent;

    @Pattern(regexp = "^(tdsql|h2)$", message = "profile must be 'tdsql' or 'h2'")
    private String profile;

    private String jdbcUrl;
    private String jdbcUsername;
    private String jdbcPassword;
    private String jdbcDriver;

    /** Create/replace the A-Box tables (and seed demo data) before loading. */
    private Boolean initDatabase;

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getOntologyPath() {
        return ontologyPath;
    }

    public void setOntologyPath(String ontologyPath) {
        this.ontologyPath = ontologyPath;
    }

    public String getOntologyContent() {
        return ontologyContent;
    }

    public void setOntologyContent(String ontologyContent) {
        this.ontologyContent = ontologyContent;
    }

    public String getMappingPath() {
        return mappingPath;
    }

    public void setMappingPath(String mappingPath) {
        this.mappingPath = mappingPath;
    }

    public String getMappingContent() {
        return mappingContent;
    }

    public void setMappingContent(String mappingContent) {
        this.mappingContent = mappingContent;
    }

    public String getRulesPath() {
        return rulesPath;
    }

    public void setRulesPath(String rulesPath) {
        this.rulesPath = rulesPath;
    }

    public String getRulesContent() {
        return rulesContent;
    }

    public void setRulesContent(String rulesContent) {
        this.rulesContent = rulesContent;
    }

    public String getInstancesPath() {
        return instancesPath;
    }

    public void setInstancesPath(String instancesPath) {
        this.instancesPath = instancesPath;
    }

    public String getInstancesContent() {
        return instancesContent;
    }

    public void setInstancesContent(String instancesContent) {
        this.instancesContent = instancesContent;
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public void setJdbcUrl(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    public String getJdbcUsername() {
        return jdbcUsername;
    }

    public void setJdbcUsername(String jdbcUsername) {
        this.jdbcUsername = jdbcUsername;
    }

    public String getJdbcPassword() {
        return jdbcPassword;
    }

    public void setJdbcPassword(String jdbcPassword) {
        this.jdbcPassword = jdbcPassword;
    }

    public String getJdbcDriver() {
        return jdbcDriver;
    }

    public void setJdbcDriver(String jdbcDriver) {
        this.jdbcDriver = jdbcDriver;
    }

    public Boolean getInitDatabase() {
        return initDatabase;
    }

    public void setInitDatabase(Boolean initDatabase) {
        this.initDatabase = initDatabase;
    }
}
