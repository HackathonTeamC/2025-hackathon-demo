package com.udbmanager.model;

public enum DatabaseType {
    MYSQL("com.mysql.cj.jdbc.Driver", "jdbc:mysql://"),
    POSTGRESQL("org.postgresql.Driver", "jdbc:postgresql://"),
    SQLITE("org.sqlite.JDBC", "jdbc:sqlite:"),
    H2("org.h2.Driver", "jdbc:h2:");

    private final String driverClassName;
    private final String urlPrefix;

    DatabaseType(String driverClassName, String urlPrefix) {
        this.driverClassName = driverClassName;
        this.urlPrefix = urlPrefix;
    }

    public String getDriverClassName() {
        return driverClassName;
    }

    public String getUrlPrefix() {
        return urlPrefix;
    }
}
