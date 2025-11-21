package com.udb.manager.model;

public enum DatabaseType {
    MYSQL("com.mysql.cj.jdbc.Driver", "jdbc:mysql://%s:%d/%s?characterEncoding=UTF-8&useUnicode=true&serverTimezone=Asia/Tokyo"),
    POSTGRESQL("org.postgresql.Driver", "jdbc:postgresql://%s:%d/%s?characterEncoding=UTF-8&useUnicode=true"),
    SQLITE("org.sqlite.JDBC", "jdbc:sqlite:%s"),
    ORACLE("oracle.jdbc.OracleDriver", "jdbc:oracle:thin:@//%s:%d/%s"),
    SQLSERVER("com.microsoft.sqlserver.jdbc.SQLServerDriver", "jdbc:sqlserver://%s:%d;databaseName=%s;encrypt=true;trustServerCertificate=true"),
    MONGODB("mongodb.jdbc.MongoDriver", "mongodb://%s:%d/%s"),
    SNOWFLAKE("net.snowflake.client.jdbc.SnowflakeDriver", "jdbc:snowflake://%s"),
    SALESFORCE("com.salesforce.jdbc.SalesforceDriver", "jdbc:salesforce:");

    private final String driverClassName;
    private final String urlTemplate;

    DatabaseType(String driverClassName, String urlTemplate) {
        this.driverClassName = driverClassName;
        this.urlTemplate = urlTemplate;
    }

    public String getDriverClassName() {
        return driverClassName;
    }

    public String buildJdbcUrl(String host, int port, String databaseName) {
        return String.format(urlTemplate, host, port, databaseName);
    }

    public Integer getDefaultPort() {
        return switch (this) {
            case MYSQL -> 3306;
            case POSTGRESQL -> 5432;
            case ORACLE -> 1521;
            case SQLSERVER -> 1433;
            case MONGODB -> 27017;
            default -> null;
        };
    }
}
