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
            
            // Get object metadata to determine available fields
            JsonNode objectDescribe = salesforceConnectionManager.describeSObject(session, objectName);
            
            // Check if object is queryable
            if (!objectDescribe.get("queryable").asBoolean()) {
                throw new DatabaseConnectionException("Object " + objectName + " is not queryable");
            }
            
            // Build field list from describe (limit to first 20 fields for performance)
            List<String> fields = new ArrayList<>();
            fields.add("Id"); // Always include Id
            
            JsonNode fieldsArray = objectDescribe.get("fields");
            if (fieldsArray != null && fieldsArray.isArray()) {
                for (JsonNode field : fieldsArray) {
                    if (fields.size() >= 20) break; // Limit fields
                    
                    String fieldName = field.get("name").asText();
                    boolean isAccessible = field.get("accessible").asBoolean();
                    boolean isCompound = field.has("compoundFieldName") && !field.get("compoundFieldName").isNull();
                    
                    // Include accessible, non-compound fields
                    if (isAccessible && !isCompound && !fieldName.equals("Id")) {
                        fields.add(fieldName);
                    }
                }
            }
            
            // Build SOQL query
            StringBuilder soql = new StringBuilder("SELECT ");
            soql.append(String.join(", ", fields));
            soql.append(" FROM ");
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
