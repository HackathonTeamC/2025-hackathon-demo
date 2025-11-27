package com.udbmanager.service;

import com.force.api.ForceApi;
import com.force.api.QueryResult;
import com.udbmanager.dto.DataQueryRequest;
import com.udbmanager.dto.DataQueryResponse;
import com.udbmanager.dto.SqlExecutionRequest;
import com.udbmanager.dto.SqlExecutionResponse;
import com.udbmanager.exception.DatabaseConnectionException;
import com.udbmanager.model.DatabaseConnection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
            ForceApi api = salesforceConnectionManager.getForceApi(dbConnection, decryptedPassword);
            
            // Build SOQL query
            StringBuilder soql = new StringBuilder("SELECT FIELDS(ALL) FROM ");
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
            QueryResult<Map> queryResult = api.query(soql.toString(), Map.class);
            
            // Convert to response format
            List<Map<String, Object>> data = new ArrayList<>();
            for (Map record : queryResult.getRecords()) {
                Map<String, Object> row = new LinkedHashMap<>(record);
                data.add(row);
            }
            
            // Get total count (Salesforce doesn't provide it in query result)
            Long totalRecords = (long) queryResult.getTotalSize();
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
            ForceApi api = salesforceConnectionManager.getForceApi(dbConnection, decryptedPassword);
            
            // Execute SOQL query
            QueryResult<Map> queryResult = api.query(soql, Map.class);
            long executionTime = System.currentTimeMillis() - startTime;
            
            // Convert to response format
            List<Map<String, Object>> data = new ArrayList<>();
            int count = 0;
            for (Map record : queryResult.getRecords()) {
                if (count >= request.getMaxRows()) {
                    break;
                }
                Map<String, Object> row = new LinkedHashMap<>(record);
                data.add(row);
                count++;
            }
            
            return SqlExecutionResponse.success("SELECT", data, data.size(), executionTime);
            
        } catch (Exception e) {
            log.error("SOQL execution failed", e);
            return SqlExecutionResponse.error(e.getMessage());
        }
    }
}
