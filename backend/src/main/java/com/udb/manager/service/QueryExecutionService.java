package com.udb.manager.service;

import com.udb.manager.dto.QueryRequestDTO;
import com.udb.manager.dto.QueryResultDTO;
import com.udb.manager.exception.QueryExecutionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class QueryExecutionService {

    private final DatabaseConnectionService connectionService;

    public QueryResultDTO executeQuery(String connectionId, QueryRequestDTO request) {
        long startTime = System.currentTimeMillis();
        
        try (Connection conn = connectionService.getJdbcConnection(connectionId)) {
            String query = request.getQuery().trim();
            
            if (query.toUpperCase().startsWith("SELECT")) {
                return executeSelectQuery(conn, request, startTime);
            } else {
                return executeUpdateQuery(conn, query, startTime);
            }
        } catch (Exception e) {
            log.error("Query execution failed", e);
            throw new QueryExecutionException("Query execution failed: " + e.getMessage(), e);
        }
    }

    public QueryResultDTO getTableData(String connectionId, String tableName, String schema, Integer page, Integer pageSize) {
        long startTime = System.currentTimeMillis();
        
        try (Connection conn = connectionService.getJdbcConnection(connectionId)) {
            String fullTableName = (schema != null && !schema.isEmpty()) 
                    ? schema + "." + tableName 
                    : tableName;
            
            int offset = page * pageSize;
            String query = String.format("SELECT * FROM %s LIMIT %d OFFSET %d", fullTableName, pageSize, offset);
            
            QueryRequestDTO request = new QueryRequestDTO();
            request.setQuery(query);
            request.setPage(page);
            request.setPageSize(pageSize);
            
            return executeSelectQuery(conn, request, startTime);
        } catch (Exception e) {
            log.error("Failed to get table data", e);
            throw new QueryExecutionException("Failed to get table data: " + e.getMessage(), e);
        }
    }

    private QueryResultDTO executeSelectQuery(Connection conn, QueryRequestDTO request, long startTime) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(request.getQuery())) {
            
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            List<String> columns = new ArrayList<>();
            for (int i = 1; i <= columnCount; i++) {
                columns.add(metaData.getColumnName(i));
            }
            
            List<Map<String, Object>> rows = new ArrayList<>();
            int rowCount = 0;
            while (rs.next() && rowCount < request.getPageSize()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    Object value = rs.getObject(i);
                    row.put(metaData.getColumnName(i), value);
                }
                rows.add(row);
                rowCount++;
            }
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            QueryResultDTO result = new QueryResultDTO();
            result.setColumns(columns);
            result.setRows(rows);
            result.setTotalRows(rows.size());
            result.setPage(request.getPage());
            result.setPageSize(request.getPageSize());
            result.setExecutionTime(executionTime);
            
            return result;
        }
    }

    private QueryResultDTO executeUpdateQuery(Connection conn, String query, long startTime) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            int affectedRows = stmt.executeUpdate(query);
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            QueryResultDTO result = new QueryResultDTO();
            result.setColumns(List.of("Affected Rows"));
            result.setRows(List.of(Map.of("Affected Rows", affectedRows)));
            result.setTotalRows(affectedRows);
            result.setExecutionTime(executionTime);
            
            return result;
        }
    }
}
