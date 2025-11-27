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
        config.setConnectionTestQuery("SELECT 1");
        
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
        
        if (dbType == DatabaseType.SQLITE) {
            url.append(dbConnection.getDatabaseName());
        } else {
            url.append(dbConnection.getHost())
               .append(":")
               .append(dbConnection.getPort())
               .append("/")
               .append(dbConnection.getDatabaseName());
            
            if (dbConnection.getSslEnabled()) {
                url.append("?useSSL=true");
            }
            
            if (dbConnection.getConnectionOptions() != null && !dbConnection.getConnectionOptions().isEmpty()) {
                String separator = dbConnection.getSslEnabled() ? "&" : "?";
                url.append(separator).append(dbConnection.getConnectionOptions());
            }
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
}
