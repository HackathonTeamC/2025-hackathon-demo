package com.udbmanager.model;

/**
 * Enum for supported database types
 */
public enum DatabaseType {
    MYSQL("com.mysql.cj.jdbc.Driver", "jdbc:mysql://"),
    POSTGRESQL("org.postgresql.Driver", "jdbc:postgresql://"),
    SQLITE("org.sqlite.JDBC", "jdbc:sqlite:"),
    H2("org.h2.Driver", "jdbc:h2:"),
    ORACLE("oracle.jdbc.OracleDriver", "jdbc:oracle:thin:@"),
    SQL_SERVER("com.microsoft.sqlserver.jdbc.SQLServerDriver", "jdbc:sqlserver://"),
    SALESFORCE(null, null); // Salesforce uses REST API, not JDBC

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
    
    public boolean isJdbc() {
        return this != SALESFORCE;
    }
}
