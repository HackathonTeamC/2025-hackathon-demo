package com.udbmanager.service;

import com.udbmanager.dto.DataQueryRequest;
import com.udbmanager.dto.DataQueryResponse;
import com.udbmanager.exception.DatabaseConnectionException;
import com.udbmanager.model.DatabaseConnection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataService {

    private final ConnectionService connectionService;
    private final DatabaseConnectionManager connectionManager;

    public DataQueryResponse getTableData(String connectionId, String schemaName, String tableName, DataQueryRequest request) {
        DatabaseConnection dbConnection = connectionService.getConnection(connectionId);
        String decryptedPassword = connectionService.getDecryptedPassword(dbConnection);
        
        String fullTableName = buildFullTableName(schemaName, tableName);
        
        try (Connection connection = connectionManager.getConnection(dbConnection, decryptedPassword)) {
            StringBuilder queryBuilder = new StringBuilder("SELECT * FROM ").append(fullTableName);
            List<Object> parameters = new ArrayList<>();
            
            if (request.getFilters() != null && !request.getFilters().isEmpty()) {
                queryBuilder.append(" WHERE ");
                boolean first = true;
                for (Map.Entry<String, Object> entry : request.getFilters().entrySet()) {
                    if (!first) {
                        queryBuilder.append(" AND ");
                    }
                    queryBuilder.append(entry.getKey()).append(" = ?");
                    parameters.add(entry.getValue());
                    first = false;
                }
            }
            
            if (request.getSortColumn() != null && !request.getSortColumn().isEmpty()) {
                queryBuilder.append(" ORDER BY ").append(request.getSortColumn())
                           .append(" ").append(request.getSortDirection());
            } else {
                // SQL Server and Oracle require ORDER BY for OFFSET/FETCH
                queryBuilder.append(" ORDER BY (SELECT NULL)");
            }
            
            int offset = request.getPage() * request.getSize();
            
            // Add pagination based on database type
            appendPaginationClause(queryBuilder, dbConnection.getDatabaseType(), request.getSize(), offset);
            
            Long totalRecords = getTotalCount(connection, fullTableName, request.getFilters());
            
            List<Map<String, Object>> data = new ArrayList<>();
            try (PreparedStatement stmt = connection.prepareStatement(queryBuilder.toString())) {
                int paramIndex = 1;
                for (Object param : parameters) {
                    stmt.setObject(paramIndex++, param);
                }
                
                try (ResultSet rs = stmt.executeQuery()) {
                    ResultSetMetaData metaData = rs.getMetaData();
                    int columnCount = metaData.getColumnCount();
                    
                    while (rs.next()) {
                        Map<String, Object> row = new LinkedHashMap<>();
                        for (int i = 1; i <= columnCount; i++) {
                            String columnName = metaData.getColumnName(i);
                            Object value = rs.getObject(i);
                            row.put(columnName, value);
                        }
                        data.add(row);
                    }
                }
            }
            
            int totalPages = (int) Math.ceil((double) totalRecords / request.getSize());
            
            return new DataQueryResponse(data, totalRecords, request.getPage(), request.getSize(), totalPages);
            
        } catch (SQLException e) {
            log.error("Failed to query table data", e);
            throw new DatabaseConnectionException("Failed to query table data", e);
        }
    }

    private Long getTotalCount(Connection connection, String fullTableName, Map<String, Object> filters) throws SQLException {
        StringBuilder countQuery = new StringBuilder("SELECT COUNT(*) FROM ").append(fullTableName);
        List<Object> parameters = new ArrayList<>();
        
        if (filters != null && !filters.isEmpty()) {
            countQuery.append(" WHERE ");
            boolean first = true;
            for (Map.Entry<String, Object> entry : filters.entrySet()) {
                if (!first) {
                    countQuery.append(" AND ");
                }
                countQuery.append(entry.getKey()).append(" = ?");
                parameters.add(entry.getValue());
                first = false;
            }
        }
        
        try (PreparedStatement stmt = connection.prepareStatement(countQuery.toString())) {
            int paramIndex = 1;
            for (Object param : parameters) {
                stmt.setObject(paramIndex++, param);
            }
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        
        return 0L;
    }

    public Map<String, Object> insertData(String connectionId, String schemaName, String tableName, Map<String, Object> data) {
        DatabaseConnection dbConnection = connectionService.getConnection(connectionId);
        String decryptedPassword = connectionService.getDecryptedPassword(dbConnection);
        
        String fullTableName = buildFullTableName(schemaName, tableName);
        
        try (Connection connection = connectionManager.getConnection(dbConnection, decryptedPassword)) {
            StringBuilder query = new StringBuilder("INSERT INTO ").append(fullTableName).append(" (");
            StringBuilder values = new StringBuilder("VALUES (");
            
            List<String> columns = new ArrayList<>(data.keySet());
            for (int i = 0; i < columns.size(); i++) {
                if (i > 0) {
                    query.append(", ");
                    values.append(", ");
                }
                query.append(columns.get(i));
                values.append("?");
            }
            query.append(") ").append(values).append(")");
            
            try (PreparedStatement stmt = connection.prepareStatement(query.toString())) {
                int paramIndex = 1;
                for (String column : columns) {
                    stmt.setObject(paramIndex++, data.get(column));
                }
                
                int rowsAffected = stmt.executeUpdate();
                log.info("Inserted {} row(s) into {}", rowsAffected, fullTableName);
                
                return Map.of("success", true, "rowsAffected", rowsAffected);
            }
            
        } catch (SQLException e) {
            log.error("Failed to insert data", e);
            throw new DatabaseConnectionException("Failed to insert data: " + e.getMessage(), e);
        }
    }

    public Map<String, Object> updateData(String connectionId, String schemaName, String tableName, 
                                          Map<String, Object> data, Map<String, Object> whereConditions) {
        DatabaseConnection dbConnection = connectionService.getConnection(connectionId);
        String decryptedPassword = connectionService.getDecryptedPassword(dbConnection);
        
        String fullTableName = buildFullTableName(schemaName, tableName);
        
        try (Connection connection = connectionManager.getConnection(dbConnection, decryptedPassword)) {
            StringBuilder query = new StringBuilder("UPDATE ").append(fullTableName).append(" SET ");
            
            List<String> columns = new ArrayList<>(data.keySet());
            for (int i = 0; i < columns.size(); i++) {
                if (i > 0) {
                    query.append(", ");
                }
                query.append(columns.get(i)).append(" = ?");
            }
            
            query.append(" WHERE ");
            List<String> whereColumns = new ArrayList<>(whereConditions.keySet());
            for (int i = 0; i < whereColumns.size(); i++) {
                if (i > 0) {
                    query.append(" AND ");
                }
                query.append(whereColumns.get(i)).append(" = ?");
            }
            
            try (PreparedStatement stmt = connection.prepareStatement(query.toString())) {
                int paramIndex = 1;
                for (String column : columns) {
                    stmt.setObject(paramIndex++, data.get(column));
                }
                for (String column : whereColumns) {
                    stmt.setObject(paramIndex++, whereConditions.get(column));
                }
                
                int rowsAffected = stmt.executeUpdate();
                log.info("Updated {} row(s) in {}", rowsAffected, fullTableName);
                
                return Map.of("success", true, "rowsAffected", rowsAffected);
            }
            
        } catch (SQLException e) {
            log.error("Failed to update data", e);
            throw new DatabaseConnectionException("Failed to update data: " + e.getMessage(), e);
        }
    }

    public Map<String, Object> deleteData(String connectionId, String schemaName, String tableName, 
                                          Map<String, Object> whereConditions) {
        DatabaseConnection dbConnection = connectionService.getConnection(connectionId);
        String decryptedPassword = connectionService.getDecryptedPassword(dbConnection);
        
        String fullTableName = buildFullTableName(schemaName, tableName);
        
        try (Connection connection = connectionManager.getConnection(dbConnection, decryptedPassword)) {
            StringBuilder query = new StringBuilder("DELETE FROM ").append(fullTableName).append(" WHERE ");
            
            List<String> whereColumns = new ArrayList<>(whereConditions.keySet());
            for (int i = 0; i < whereColumns.size(); i++) {
                if (i > 0) {
                    query.append(" AND ");
                }
                query.append(whereColumns.get(i)).append(" = ?");
            }
            
            try (PreparedStatement stmt = connection.prepareStatement(query.toString())) {
                int paramIndex = 1;
                for (String column : whereColumns) {
                    stmt.setObject(paramIndex++, whereConditions.get(column));
                }
                
                int rowsAffected = stmt.executeUpdate();
                log.info("Deleted {} row(s) from {}", rowsAffected, fullTableName);
                
                return Map.of("success", true, "rowsAffected", rowsAffected);
            }
            
        } catch (SQLException e) {
            log.error("Failed to delete data", e);
            throw new DatabaseConnectionException("Failed to delete data: " + e.getMessage(), e);
        }
    }

    private String buildFullTableName(String schemaName, String tableName) {
        if (schemaName != null && !schemaName.isEmpty()) {
            return schemaName + "." + tableName;
        }
        return tableName;
    }

    /**
     * Append database-specific pagination clause
     */
    private void appendPaginationClause(StringBuilder queryBuilder, com.udbmanager.model.DatabaseType dbType, 
                                        int limit, int offset) {
        switch (dbType) {
            case SQL_SERVER:
            case ORACLE:
                // SQL Server and Oracle use OFFSET ... ROWS FETCH NEXT ... ROWS ONLY
                queryBuilder.append(" OFFSET ").append(offset).append(" ROWS")
                           .append(" FETCH NEXT ").append(limit).append(" ROWS ONLY");
                break;
            case MYSQL:
            case POSTGRESQL:
            case SQLITE:
            case H2:
            default:
                // MySQL, PostgreSQL, SQLite, H2 use LIMIT ... OFFSET ...
                queryBuilder.append(" LIMIT ").append(limit).append(" OFFSET ").append(offset);
                break;
        }
    }
}
