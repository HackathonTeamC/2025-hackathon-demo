package com.udbmanager.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.udbmanager.dto.DataQueryRequest;
import com.udbmanager.dto.DataQueryResponse;
import com.udbmanager.dto.SqlExecutionRequest;
import com.udbmanager.dto.SqlExecutionResponse;
import com.udbmanager.exception.DatabaseConnectionException;
import com.udbmanager.model.DatabaseConnection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service for querying Salesforce data using SOQL
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SalesforceDataService {

    private final ConnectionService connectionService;
    private final SalesforceConnectionManager salesforceConnectionManager;

    /**
     * Get data from Salesforce object with pagination
     */
    public DataQueryResponse getSalesforceObjectData(String connectionId, String objectName, DataQueryRequest request) {
        DatabaseConnection dbConnection = connectionService.getConnection(connectionId);
        String decryptedPassword = connectionService.getDecryptedPassword(dbConnection);
        
        try {
            SalesforceConnectionManager.SalesforceSession session = 
                    salesforceConnectionManager.getSession(dbConnection, decryptedPassword);
            
            // Build SOQL query - use specific fields instead of FIELDS(ALL)
            StringBuilder soql = new StringBuilder("SELECT Id, Name FROM ");
            soql.append(objectName);
            
            // Add WHERE clause for filters
            if (request.getFilters() != null && !request.getFilters().isEmpty()) {
                soql.append(" WHERE ");
                boolean first = true;
                for (Map.Entry<String, Object> entry : request.getFilters().entrySet()) {
                    if (!first) {
                        soql.append(" AND ");
                    }
                    soql.append(entry.getKey()).append(" = '").append(entry.getValue()).append("'");
                    first = false;
                }
            }
            
            // Add ORDER BY
            if (request.getSortColumn() != null && !request.getSortColumn().isEmpty()) {
                soql.append(" ORDER BY ").append(request.getSortColumn())
                    .append(" ").append(request.getSortDirection());
            }
            
            // Add LIMIT and OFFSET
            int offset = request.getPage() * request.getSize();
            soql.append(" LIMIT ").append(request.getSize());
            soql.append(" OFFSET ").append(offset);
            
            // Execute query
            JsonNode response = salesforceConnectionManager.executeQuery(session, soql.toString());
            
            // Convert to response format
            List<Map<String, Object>> data = convertRecords(response.get("records"));
            long totalRecords = response.get("totalSize").asLong();
            int totalPages = (int) Math.ceil((double) totalRecords / request.getSize());
            
            return new DataQueryResponse(data, totalRecords, request.getPage(), request.getSize(), totalPages);
            
        } catch (Exception e) {
            log.error("Failed to query Salesforce object: {}", objectName, e);
            throw new DatabaseConnectionException("Failed to query Salesforce object", e);
        }
    }

    /**
     * Execute SOQL query
     */
    public SqlExecutionResponse executeSoql(String connectionId, SqlExecutionRequest request) {
        DatabaseConnection dbConnection = connectionService.getConnection(connectionId);
        String decryptedPassword = connectionService.getDecryptedPassword(dbConnection);
        
        String soql = request.getSql().trim();
        long startTime = System.currentTimeMillis();
        
        try {
            SalesforceConnectionManager.SalesforceSession session = 
                    salesforceConnectionManager.getSession(dbConnection, decryptedPassword);
            
            // Execute SOQL query
            JsonNode response = salesforceConnectionManager.executeQuery(session, soql);
            long executionTime = System.currentTimeMillis() - startTime;
            
            // Convert to response format
            List<Map<String, Object>> data = convertRecords(response.get("records"));
            
            // Limit to maxRows
            if (data.size() > request.getMaxRows()) {
                data = data.subList(0, request.getMaxRows());
            }
            
            return SqlExecutionResponse.success("SELECT", data, data.size(), executionTime);
            
        } catch (Exception e) {
            log.error("SOQL execution failed", e);
            return SqlExecutionResponse.error(e.getMessage());
        }
    }

    /**
     * Convert JsonNode records to List of Maps
     */
    private List<Map<String, Object>> convertRecords(JsonNode records) {
        List<Map<String, Object>> data = new ArrayList<>();
        
        if (records != null && records.isArray()) {
            for (JsonNode record : records) {
                Map<String, Object> row = new LinkedHashMap<>();
                Iterator<Map.Entry<String, JsonNode>> fields = record.fields();
                
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> field = fields.next();
                    String key = field.getKey();
                    
                    // Skip 'attributes' field (Salesforce metadata)
                    if (key.equals("attributes")) {
                        continue;
                    }
                    
                    JsonNode value = field.getValue();
                    if (value.isNull()) {
                        row.put(key, null);
                    } else if (value.isTextual()) {
                        row.put(key, value.asText());
                    } else if (value.isNumber()) {
                        row.put(key, value.asDouble());
                    } else if (value.isBoolean()) {
                        row.put(key, value.asBoolean());
                    } else {
                        row.put(key, value.toString());
                    }
                }
                
                data.add(row);
            }
        }
        
        return data;
    }
}
