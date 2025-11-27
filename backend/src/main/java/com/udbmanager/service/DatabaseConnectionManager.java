package com.udbmanager.service;

import com.udbmanager.exception.DatabaseConnectionException;
import com.udbmanager.model.DatabaseConnection;
import com.udbmanager.model.DatabaseType;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class DatabaseConnectionManager {

    private final Map<String, HikariDataSource> dataSourceCache = new ConcurrentHashMap<>();

    public DataSource getDataSource(DatabaseConnection dbConnection, String decryptedPassword) {
        String connectionId = dbConnection.getId();
        
        if (dataSourceCache.containsKey(connectionId)) {
            HikariDataSource dataSource = dataSourceCache.get(connectionId);
            if (!dataSource.isClosed()) {
                return dataSource;
            } else {
                dataSourceCache.remove(connectionId);
            }
        }

        HikariDataSource dataSource = createDataSource(dbConnection, decryptedPassword);
        dataSourceCache.put(connectionId, dataSource);
        return dataSource;
    }

    private HikariDataSource createDataSource(DatabaseConnection dbConnection, String decryptedPassword) {
        HikariConfig config = new HikariConfig();
        
        DatabaseType dbType = dbConnection.getDatabaseType();
        config.setDriverClassName(dbType.getDriverClassName());
        config.setJdbcUrl(buildJdbcUrl(dbConnection));
        config.setUsername(dbConnection.getUsername());
        config.setPassword(decryptedPassword);
        
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(dbConnection.getTimeout() * 1000L);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setPoolName("UDB-Pool-" + dbConnection.getConnectionName());
        
        // Set database-specific connection test query
        String testQuery = getConnectionTestQuery(dbType);
        if (testQuery != null) {
            config.setConnectionTestQuery(testQuery);
        }
        
        try {
            return new HikariDataSource(config);
        } catch (Exception e) {
            log.error("Failed to create data source for connection: {}", dbConnection.getConnectionName(), e);
            throw new DatabaseConnectionException("Failed to create database connection", e);
        }
    }

    private String buildJdbcUrl(DatabaseConnection dbConnection) {
        DatabaseType dbType = dbConnection.getDatabaseType();
        String urlPrefix = dbType.getUrlPrefix();
        
        StringBuilder url = new StringBuilder(urlPrefix);
        
        switch (dbType) {
            case SQLITE:
                // jdbc:sqlite:path/to/database.db
                url.append(dbConnection.getDatabaseName());
                break;
                
            case H2:
                // jdbc:h2:~/test or jdbc:h2:mem:test
                url.append(dbConnection.getDatabaseName());
                break;
                
            case ORACLE:
                // jdbc:oracle:thin:@host:port:sid or jdbc:oracle:thin:@host:port/service_name
                // If databaseName starts with '/', use Service Name format, otherwise use SID format
                Integer oraclePort = dbConnection.getPort();
                if (oraclePort == null || oraclePort == 0) {
                    oraclePort = 1521; // Default Oracle port
                }
                
                url.append(dbConnection.getHost())
                   .append(":")
                   .append(oraclePort);
                
                String dbName = dbConnection.getDatabaseName();
                if (dbName.startsWith("/")) {
                    // Service Name format: jdbc:oracle:thin:@host:port/service_name
                    url.append(dbName);
                } else {
                    // SID format: jdbc:oracle:thin:@host:port:sid
                    url.append(":").append(dbName);
                }
                
                if (dbConnection.getConnectionOptions() != null && !dbConnection.getConnectionOptions().isEmpty()) {
                    url.append("?").append(dbConnection.getConnectionOptions());
                }
                break;
                
            case SQL_SERVER:
                // jdbc:sqlserver://host:port;databaseName=dbname
                Integer sqlServerPort = dbConnection.getPort();
                if (sqlServerPort == null || sqlServerPort == 0) {
                    sqlServerPort = 1433; // Default SQL Server port
                }
                
                url.append(dbConnection.getHost())
                   .append(":")
                   .append(sqlServerPort)
                   .append(";databaseName=")
                   .append(dbConnection.getDatabaseName());
                
                if (dbConnection.getSslEnabled()) {
                    // For production with valid SSL certificates, set trustServerCertificate=false
                    // For development/self-signed certificates, use trustServerCertificate=true
                    url.append(";encrypt=true;trustServerCertificate=true");
                } else {
                    // For non-SSL connections
                    url.append(";encrypt=false");
                }
                
                if (dbConnection.getConnectionOptions() != null && !dbConnection.getConnectionOptions().isEmpty()) {
                    url.append(";").append(dbConnection.getConnectionOptions());
                }
                break;
                
            default:
                // MySQL, PostgreSQL and other standard databases
                // jdbc:mysql://host:port/database or jdbc:postgresql://host:port/database
                Integer defaultPort = dbConnection.getPort();
                if (defaultPort == null || defaultPort == 0) {
                    // Set default port based on database type
                    if (dbType == DatabaseType.MYSQL) {
                        defaultPort = 3306;
                    } else if (dbType == DatabaseType.POSTGRESQL) {
                        defaultPort = 5432;
                    } else {
                        defaultPort = 5432; // Fallback to PostgreSQL default
                    }
                }
                
                url.append(dbConnection.getHost())
                   .append(":")
                   .append(defaultPort)
                   .append("/")
                   .append(dbConnection.getDatabaseName());
                
                if (dbConnection.getSslEnabled()) {
                    url.append("?useSSL=true");
                }
                
                if (dbConnection.getConnectionOptions() != null && !dbConnection.getConnectionOptions().isEmpty()) {
                    String separator = dbConnection.getSslEnabled() ? "&" : "?";
                    url.append(separator).append(dbConnection.getConnectionOptions());
                }
                break;
        }
        
        return url.toString();
    }

    public void testConnection(DatabaseConnection dbConnection, String decryptedPassword) throws SQLException {
        HikariConfig config = new HikariConfig();
        
        DatabaseType dbType = dbConnection.getDatabaseType();
        config.setDriverClassName(dbType.getDriverClassName());
        config.setJdbcUrl(buildJdbcUrl(dbConnection));
        config.setUsername(dbConnection.getUsername());
        config.setPassword(decryptedPassword);
        config.setConnectionTimeout(10000);
        
        try (HikariDataSource testDataSource = new HikariDataSource(config);
             Connection connection = testDataSource.getConnection()) {
            log.info("Connection test successful for: {}", dbConnection.getConnectionName());
        }
    }

    public void closeConnection(String connectionId) {
        HikariDataSource dataSource = dataSourceCache.remove(connectionId);
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            log.info("Closed data source for connection ID: {}", connectionId);
        }
    }

    public void closeAllConnections() {
        dataSourceCache.forEach((id, dataSource) -> {
            if (!dataSource.isClosed()) {
                dataSource.close();
            }
        });
        dataSourceCache.clear();
        log.info("All database connections closed");
    }

    public Connection getConnection(DatabaseConnection dbConnection, String decryptedPassword) throws SQLException {
        DataSource dataSource = getDataSource(dbConnection, decryptedPassword);
        return dataSource.getConnection();
    }

    /**
     * Get database-specific connection test query
     * Oracle requires "FROM DUAL", while other databases work with "SELECT 1"
     */
    private String getConnectionTestQuery(DatabaseType dbType) {
        switch (dbType) {
            case ORACLE:
                return "SELECT 1 FROM DUAL";
            case MYSQL:
            case POSTGRESQL:
            case SQLITE:
            case H2:
            case SQL_SERVER:
                return "SELECT 1";
            default:
                return null; // Let HikariCP use its default
        }
    }
}
