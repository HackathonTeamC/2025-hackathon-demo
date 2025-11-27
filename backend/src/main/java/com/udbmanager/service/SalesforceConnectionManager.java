package com.udbmanager.service;

import com.force.api.ApiConfig;
import com.force.api.ForceApi;
import com.udbmanager.exception.DatabaseConnectionException;
import com.udbmanager.model.DatabaseConnection;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages Salesforce REST API connections
 */
@Component
@Slf4j
public class SalesforceConnectionManager {

    private final Map<String, ForceApi> apiCache = new ConcurrentHashMap<>();

    /**
     * Get or create Salesforce API connection
     */
    public ForceApi getForceApi(DatabaseConnection dbConnection, String decryptedPassword) {
        String connectionId = dbConnection.getId();
        
        // Return cached API if available
        if (apiCache.containsKey(connectionId)) {
            return apiCache.get(connectionId);
        }

        // Create new API connection
        ForceApi api = createForceApi(dbConnection, decryptedPassword);
        apiCache.put(connectionId, api);
        return api;
    }

    /**
     * Create a new Salesforce API connection
     */
    private ForceApi createForceApi(DatabaseConnection dbConnection, String decryptedPassword) {
        try {
            // Instance URL from host (e.g., https://your-instance.salesforce.com)
            String instanceUrl = dbConnection.getHost();
            if (!instanceUrl.startsWith("http")) {
                instanceUrl = "https://" + instanceUrl;
            }
            
            // Username/Password authentication
            ApiConfig config = new ApiConfig()
                    .setUsername(dbConnection.getUsername())
                    .setPassword(decryptedPassword)
                    .setLoginEndpoint(instanceUrl + "/services/oauth2/token");
            
            ForceApi api = new ForceApi(config);
            log.info("Salesforce API connection created for: {}", dbConnection.getConnectionName());
            return api;
            
        } catch (Exception e) {
            log.error("Failed to create Salesforce API connection for: {}", dbConnection.getConnectionName(), e);
            throw new DatabaseConnectionException("Failed to connect to Salesforce: " + e.getMessage(), e);
        }
    }

    /**
     * Test Salesforce connection
     */
    public void testConnection(DatabaseConnection dbConnection, String decryptedPassword) {
        try {
            ForceApi api = createForceApi(dbConnection, decryptedPassword);
            // Test with a simple query
            api.query("SELECT Id FROM User LIMIT 1");
            log.info("Salesforce connection test successful for: {}", dbConnection.getConnectionName());
        } catch (Exception e) {
            log.error("Salesforce connection test failed", e);
            throw new DatabaseConnectionException("Salesforce connection test failed: " + e.getMessage(), e);
        }
    }

    /**
     * Close and remove API from cache
     */
    public void closeConnection(String connectionId) {
        ForceApi api = apiCache.remove(connectionId);
        if (api != null) {
            log.info("Closed Salesforce API connection for ID: {}", connectionId);
        }
    }

    /**
     * Close all connections
     */
    public void closeAllConnections() {
        apiCache.clear();
        log.info("All Salesforce API connections closed");
    }
}
