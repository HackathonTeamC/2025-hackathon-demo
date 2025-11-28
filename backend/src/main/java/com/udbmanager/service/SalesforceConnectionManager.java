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
     * Authenticate with Salesforce using SOAP Login API (no OAuth Connected App required)
     */
    private SalesforceSession authenticate(DatabaseConnection dbConnection, String decryptedPassword) {
        try {
            String loginUrl = dbConnection.getHost();
            if (!loginUrl.startsWith("http")) {
                loginUrl = "https://" + loginUrl;
            }
            
            log.info("Attempting Salesforce SOAP login - URL: {}, Username: {}", loginUrl, dbConnection.getUsername());
            
            // Use SOAP login API - no Connected App required
            String soapEndpoint = loginUrl + "/services/Soap/u/57.0";
            
            // Build SOAP login request
            String soapRequest = buildSoapLoginRequest(dbConnection.getUsername(), decryptedPassword);
            
            String soapResponse = webClient.post()
                    .uri(soapEndpoint)
                    .contentType(MediaType.TEXT_XML)
                    .header("SOAPAction", "login")
                    .bodyValue(soapRequest)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (soapResponse == null) {
                throw new DatabaseConnectionException("Failed to authenticate with Salesforce - no response");
            }

            // Parse SOAP response to extract sessionId and serverUrl
            String sessionId = extractXmlValue(soapResponse, "sessionId");
            String serverUrl = extractXmlValue(soapResponse, "serverUrl");
            
            if (sessionId == null || serverUrl == null) {
                // Check for fault
                String faultCode = extractXmlValue(soapResponse, "faultcode");
                String faultString = extractXmlValue(soapResponse, "faultstring");
                
                if (faultString != null) {
                    throw new DatabaseConnectionException("Salesforce authentication failed: " + faultString);
                }
                
                log.error("Failed to parse Salesforce SOAP response. Response: {}", soapResponse);
                throw new DatabaseConnectionException("Failed to parse Salesforce login response");
            }

            log.debug("Extracted serverUrl from SOAP response: {}", serverUrl);
            
            // Extract instance URL from serverUrl
            String instanceUrl = extractInstanceUrl(serverUrl);
            
            log.info("Extracted instanceUrl: {}", instanceUrl);
            
            SalesforceSession session = new SalesforceSession();
            session.setAccessToken(sessionId);
            session.setInstanceUrl(instanceUrl);

            log.info("Salesforce SOAP authentication successful for: {}", dbConnection.getConnectionName());
            return session;

        } catch (DatabaseConnectionException e) {
            throw e;
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
            String errorDetails = e.getResponseBodyAsString();
            log.error("Failed to authenticate with Salesforce. Status: {}, Response: {}", e.getStatusCode(), errorDetails);
            throw new DatabaseConnectionException(
                "Failed to authenticate with Salesforce (HTTP " + e.getStatusCode() + "): " + errorDetails, e);
        } catch (Exception e) {
            log.error("Failed to authenticate with Salesforce", e);
            throw new DatabaseConnectionException("Failed to authenticate with Salesforce: " + e.getMessage(), e);
        }
    }
    
    /**
     * Build SOAP login request XML
     */
    private String buildSoapLoginRequest(String username, String password) {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                "xmlns:urn=\"urn:partner.soap.sforce.com\">" +
                "<soapenv:Body>" +
                "<urn:login>" +
                "<urn:username>" + escapeXml(username) + "</urn:username>" +
                "<urn:password>" + escapeXml(password) + "</urn:password>" +
                "</urn:login>" +
                "</soapenv:Body>" +
                "</soapenv:Envelope>";
    }
    
    /**
     * Extract value from XML response (simple parser)
     */
    private String extractXmlValue(String xml, String tagName) {
        String startTag = "<" + tagName + ">";
        String endTag = "</" + tagName + ">";
        
        int startIndex = xml.indexOf(startTag);
        if (startIndex == -1) {
            // Try with namespace
            startTag = ":" + tagName + ">";
            startIndex = xml.indexOf(startTag);
            if (startIndex == -1) {
                return null;
            }
            startIndex = startIndex + startTag.length();
        } else {
            startIndex = startIndex + startTag.length();
        }
        
        int endIndex = xml.indexOf(endTag, startIndex);
        if (endIndex == -1) {
            return null;
        }
        
        String value = xml.substring(startIndex, endIndex);
        return value != null ? value.trim() : null;
    }
    
    /**
     * Extract instance URL from server URL
     */
    private String extractInstanceUrl(String serverUrl) {
        // serverUrl format: https://na1.salesforce.com/services/Soap/u/57.0/00D...
        // We need: https://na1.salesforce.com
        try {
            if (serverUrl == null) {
                return null;
            }
            
            String trimmedUrl = serverUrl.trim();
            log.debug("Extracting instance URL from: '{}'", trimmedUrl);
            
            int idx = trimmedUrl.indexOf("/services/");
            if (idx > 0) {
                String instanceUrl = trimmedUrl.substring(0, idx);
                log.debug("Extracted instance URL: '{}'", instanceUrl);
                return instanceUrl;
            }
            
            log.debug("No '/services/' found, returning original URL: '{}'", trimmedUrl);
            return trimmedUrl;
        } catch (Exception e) {
            log.warn("Failed to extract instance URL from: {}", serverUrl, e);
            return serverUrl != null ? serverUrl.trim() : null;
        }
    }
    
    /**
     * Escape XML special characters
     */
    private String escapeXml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    /**
     * Test Salesforce connection
     */
    public void testConnection(DatabaseConnection dbConnection, String decryptedPassword) {
        try {
            SalesforceSession session = authenticate(dbConnection, decryptedPassword);
            
            log.info("Testing connection with instanceUrl: {}", session.getInstanceUrl());
            
            // Test with a simple query
            String queryUrl = session.getInstanceUrl() + "/services/data/" + session.getApiVersion() + "/query?q=SELECT+Id+FROM+User+LIMIT+1";
            
            log.debug("Test query URL: {}", queryUrl);
            
            webClient.get()
                    .uri(queryUrl)
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
            String baseUrl = session.getInstanceUrl() + "/services/data/" + session.getApiVersion() + "/query";
            
            return webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host(extractHost(session.getInstanceUrl()))
                            .path("/services/data/" + session.getApiVersion() + "/query")
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
     * Extract host from full URL
     */
    private String extractHost(String url) {
        try {
            if (url.startsWith("https://")) {
                return url.substring(8);
            } else if (url.startsWith("http://")) {
                return url.substring(7);
            }
            return url;
        } catch (Exception e) {
            return url;
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

    /**
     * Extract option value from connection options string
     * Format: key1=value1;key2=value2
     */
    private String extractOption(String connectionOptions, String key) {
        if (connectionOptions == null || connectionOptions.isEmpty()) {
            return null;
        }
        
        String[] options = connectionOptions.split(";");
        for (String option : options) {
            String[] keyValue = option.split("=", 2);
            if (keyValue.length == 2 && keyValue[0].trim().equals(key)) {
                return keyValue[1].trim();
            }
        }
        return null;
    }
}
