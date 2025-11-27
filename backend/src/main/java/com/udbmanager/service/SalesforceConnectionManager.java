package com.udbmanager.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.udbmanager.exception.DatabaseConnectionException;
import com.udbmanager.model.DatabaseConnection;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages Salesforce REST API connections using WebClient
 */
@Component
@Slf4j
public class SalesforceConnectionManager {

    private final Map<String, SalesforceSession> sessionCache = new ConcurrentHashMap<>();
    private final WebClient webClient = WebClient.builder().build();

    @Data
    public static class SalesforceSession {
        private String accessToken;
        private String instanceUrl;
        private String apiVersion = "v57.0"; // Salesforce API version
    }

    /**
     * Get or create Salesforce session
     */
    public SalesforceSession getSession(DatabaseConnection dbConnection, String decryptedPassword) {
        String connectionId = dbConnection.getId();
        
        // Return cached session if available
        if (sessionCache.containsKey(connectionId)) {
            return sessionCache.get(connectionId);
        }

        // Create new session
        SalesforceSession session = authenticate(dbConnection, decryptedPassword);
        sessionCache.put(connectionId, session);
        return session;
    }

    /**
     * Authenticate with Salesforce using Username/Password flow
     */
    private SalesforceSession authenticate(DatabaseConnection dbConnection, String decryptedPassword) {
        try {
            String loginUrl = dbConnection.getHost();
            if (!loginUrl.startsWith("http")) {
                loginUrl = "https://" + loginUrl;
            }
            
            // OAuth 2.0 Username-Password flow
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("grant_type", "password");
            formData.add("client_id", "3MVG9_XwsqeYoue_your_consumer_key"); // Placeholder
            formData.add("client_secret", "your_consumer_secret"); // Placeholder
            formData.add("username", dbConnection.getUsername());
            formData.add("password", decryptedPassword);

            // For now, use SOAP login endpoint as fallback
            String authEndpoint = loginUrl + "/services/oauth2/token";
            
            JsonNode response = webClient.post()
                    .uri(authEndpoint)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData(formData))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (response == null) {
                throw new DatabaseConnectionException("Failed to authenticate with Salesforce");
            }

            SalesforceSession session = new SalesforceSession();
            session.setAccessToken(response.get("access_token").asText());
            session.setInstanceUrl(response.get("instance_url").asText());

            log.info("Salesforce authentication successful for: {}", dbConnection.getConnectionName());
            return session;

        } catch (Exception e) {
            log.error("Failed to authenticate with Salesforce", e);
            throw new DatabaseConnectionException("Failed to authenticate with Salesforce: " + e.getMessage(), e);
        }
    }

    /**
     * Test Salesforce connection
     */
    public void testConnection(DatabaseConnection dbConnection, String decryptedPassword) {
        try {
            SalesforceSession session = authenticate(dbConnection, decryptedPassword);
            
            // Test with a simple query
            String queryUrl = session.getInstanceUrl() + "/services/data/" + session.getApiVersion() + "/query";
            
            webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(queryUrl)
                            .queryParam("q", "SELECT Id FROM User LIMIT 1")
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + session.getAccessToken())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("Salesforce connection test successful");
        } catch (Exception e) {
            log.error("Salesforce connection test failed", e);
            throw new DatabaseConnectionException("Salesforce connection test failed: " + e.getMessage(), e);
        }
    }

    /**
     * Execute SOQL query
     */
    public JsonNode executeQuery(SalesforceSession session, String soql) {
        try {
            String queryUrl = session.getInstanceUrl() + "/services/data/" + session.getApiVersion() + "/query";
            
            return webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(queryUrl)
                            .queryParam("q", soql)
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + session.getAccessToken())
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
        } catch (Exception e) {
            log.error("Failed to execute SOQL query", e);
            throw new DatabaseConnectionException("Failed to execute SOQL query: " + e.getMessage(), e);
        }
    }

    /**
     * Get Salesforce metadata (describe global)
     */
    public JsonNode describeGlobal(SalesforceSession session) {
        try {
            String url = session.getInstanceUrl() + "/services/data/" + session.getApiVersion() + "/sobjects";
            
            return webClient.get()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + session.getAccessToken())
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
        } catch (Exception e) {
            log.error("Failed to describe global", e);
            throw new DatabaseConnectionException("Failed to retrieve Salesforce objects: " + e.getMessage(), e);
        }
    }

    /**
     * Describe specific Salesforce object
     */
    public JsonNode describeSObject(SalesforceSession session, String objectName) {
        try {
            String url = session.getInstanceUrl() + "/services/data/" + session.getApiVersion() 
                    + "/sobjects/" + objectName + "/describe";
            
            return webClient.get()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + session.getAccessToken())
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
        } catch (Exception e) {
            log.error("Failed to describe object: {}", objectName, e);
            throw new DatabaseConnectionException("Failed to describe object: " + e.getMessage(), e);
        }
    }

    /**
     * Close and remove session from cache
     */
    public void closeConnection(String connectionId) {
        sessionCache.remove(connectionId);
        log.info("Closed Salesforce session for ID: {}", connectionId);
    }

    /**
     * Close all connections
     */
    public void closeAllConnections() {
        sessionCache.clear();
        log.info("All Salesforce sessions closed");
    }
}
