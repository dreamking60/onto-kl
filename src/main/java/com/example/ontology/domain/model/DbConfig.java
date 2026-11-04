package com.example.ontology.domain.model;

/** JDBC connection settings used for one ontology model. */
public final class DbConfig {

    private final String profile;
    private final String driver;
    private final String url;
    private final String username;
    private final String password;

    public DbConfig(String profile, String driver, String url, String username, String password) {
        this.profile = profile;
        this.driver = driver;
        this.url = url;
        this.username = username;
        this.password = password;
    }

    public String getProfile() {
        return profile;
    }

    public String getDriver() {
        return driver;
    }

    public String getUrl() {
        return url;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    @Override
    public String toString() {
        return profile + " (" + url + ")";
    }
}
