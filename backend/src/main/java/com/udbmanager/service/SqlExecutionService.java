package com.udbmanager.service;

import com.udbmanager.dto.SqlExecutionRequest;
import com.udbmanager.dto.SqlExecutionResponse;
import com.udbmanager.model.DatabaseConnection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class SqlExecutionService {

    private final ConnectionService connectionService;
    private final DatabaseConnectionManager connectionManager;

    public SqlExecutionResponse executeSql(String connectionId, SqlExecutionRequest request) {
        DatabaseConnection dbConnection = connectionService.getConnection(connectionId);
        String decryptedPassword = connectionService.getDecryptedPassword(dbConnection);
        
        String sql = request.getSql().trim();
        long startTime = System.currentTimeMillis();
        
        try (Connection connection = connectionManager.getConnection(dbConnection, decryptedPassword);
             Statement stmt = connection.createStatement()) {
            
            String queryType = determineQueryType(sql);
            
            boolean hasResultSet = stmt.execute(sql);
            long executionTime = System.currentTimeMillis() - startTime;
            
            if (hasResultSet) {
                List<Map<String, Object>> data = new ArrayList<>();
                try (ResultSet rs = stmt.getResultSet()) {
                    ResultSetMetaData metaData = rs.getMetaData();
                    int columnCount = metaData.getColumnCount();
                    
                    int rowCount = 0;
                    while (rs.next() && rowCount < request.getMaxRows()) {
                        Map<String, Object> row = new LinkedHashMap<>();
                        for (int i = 1; i <= columnCount; i++) {
                            String columnName = metaData.getColumnName(i);
                            Object value = rs.getObject(i);
                            row.put(columnName, value);
                        }
                        data.add(row);
                        rowCount++;
                    }
                }
                
                return SqlExecutionResponse.success(queryType, data, data.size(), executionTime);
                
            } else {
                int rowsAffected = stmt.getUpdateCount();
                return SqlExecutionResponse.success(queryType, null, rowsAffected, executionTime);
            }
            
        } catch (SQLException e) {
            log.error("SQL execution failed", e);
            return SqlExecutionResponse.error(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during SQL execution", e);
            return SqlExecutionResponse.error("Unexpected error: " + e.getMessage());
        }
    }

    private String determineQueryType(String sql) {
        String sqlUpper = sql.toUpperCase().trim();
        
        if (sqlUpper.startsWith("SELECT") || sqlUpper.startsWith("SHOW") || sqlUpper.startsWith("DESCRIBE")) {
            return "SELECT";
        } else if (sqlUpper.startsWith("INSERT")) {
            return "INSERT";
        } else if (sqlUpper.startsWith("UPDATE")) {
            return "UPDATE";
        } else if (sqlUpper.startsWith("DELETE")) {
            return "DELETE";
        } else if (sqlUpper.startsWith("CREATE") || sqlUpper.startsWith("ALTER") || 
                   sqlUpper.startsWith("DROP") || sqlUpper.startsWith("TRUNCATE")) {
            return "DDL";
        } else {
            return "OTHER";
        }
    }
}
