package com.udbmanager.service;

import com.udbmanager.dto.ColumnInfo;
import com.udbmanager.dto.TableInfo;
import com.udbmanager.exception.DatabaseConnectionException;
import com.udbmanager.model.DatabaseConnection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class MetadataService {

    private final ConnectionService connectionService;
    private final DatabaseConnectionManager connectionManager;

    public List<String> getSchemas(String connectionId) {
        DatabaseConnection dbConnection = connectionService.getConnection(connectionId);
        String decryptedPassword = connectionService.getDecryptedPassword(dbConnection);
        
        List<String> schemas = new ArrayList<>();
        
        try (Connection connection = connectionManager.getConnection(dbConnection, decryptedPassword)) {
            DatabaseMetaData metaData = connection.getMetaData();
            
            try (ResultSet rs = metaData.getSchemas()) {
                while (rs.next()) {
                    String schema = rs.getString("TABLE_SCHEM");
                    if (schema != null && !isSystemSchema(schema)) {
                        schemas.add(schema);
                    }
                }
            }
            
            if (schemas.isEmpty()) {
                try (ResultSet rs = metaData.getCatalogs()) {
                    while (rs.next()) {
                        String catalog = rs.getString("TABLE_CAT");
                        if (catalog != null && !isSystemSchema(catalog)) {
                            schemas.add(catalog);
                        }
                    }
                }
            }
            
        } catch (SQLException e) {
            log.error("Failed to retrieve schemas", e);
            throw new DatabaseConnectionException("Failed to retrieve schemas", e);
        }
        
        return schemas;
    }

    public List<TableInfo> getTables(String connectionId, String schemaName) {
        DatabaseConnection dbConnection = connectionService.getConnection(connectionId);
        String decryptedPassword = connectionService.getDecryptedPassword(dbConnection);
        
        List<TableInfo> tables = new ArrayList<>();
        
        try (Connection connection = connectionManager.getConnection(dbConnection, decryptedPassword)) {
            DatabaseMetaData metaData = connection.getMetaData();
            
            String[] types = {"TABLE", "VIEW"};
            try (ResultSet rs = metaData.getTables(null, schemaName, "%", types)) {
                while (rs.next()) {
                    TableInfo table = new TableInfo();
                    table.setSchemaName(rs.getString("TABLE_SCHEM"));
                    table.setTableName(rs.getString("TABLE_NAME"));
                    table.setTableType(rs.getString("TABLE_TYPE"));
                    table.setRemarks(rs.getString("REMARKS"));
                    table.setRowCount(getTableRowCount(connection, table.getSchemaName(), table.getTableName(), dbConnection.getDatabaseType()));
                    tables.add(table);
                }
            }
            
        } catch (SQLException e) {
            log.error("Failed to retrieve tables", e);
            throw new DatabaseConnectionException("Failed to retrieve tables", e);
        }
        
        return tables;
    }

    public List<TableInfo> getAllTables(String connectionId) {
        DatabaseConnection dbConnection = connectionService.getConnection(connectionId);
        String decryptedPassword = connectionService.getDecryptedPassword(dbConnection);
        
        List<TableInfo> tables = new ArrayList<>();
        
        try (Connection connection = connectionManager.getConnection(dbConnection, decryptedPassword)) {
            DatabaseMetaData metaData = connection.getMetaData();
            
            String[] types = {"TABLE"};
            try (ResultSet rs = metaData.getTables(null, null, "%", types)) {
                while (rs.next()) {
                    String schemaName = rs.getString("TABLE_SCHEM");
                    String tableName = rs.getString("TABLE_NAME");
                    
                    if (schemaName != null && isSystemSchema(schemaName)) {
                        continue;
                    }
                    
                    TableInfo table = new TableInfo();
                    table.setSchemaName(schemaName);
                    table.setTableName(tableName);
                    table.setTableType(rs.getString("TABLE_TYPE"));
                    table.setRemarks(rs.getString("REMARKS"));
                    table.setRowCount(getTableRowCount(connection, schemaName, tableName, dbConnection.getDatabaseType()));
                    tables.add(table);
                }
            }
            
        } catch (SQLException e) {
            log.error("Failed to retrieve tables", e);
            throw new DatabaseConnectionException("Failed to retrieve tables", e);
        }
        
        return tables;
    }

    public List<ColumnInfo> getColumns(String connectionId, String schemaName, String tableName) {
        DatabaseConnection dbConnection = connectionService.getConnection(connectionId);
        String decryptedPassword = connectionService.getDecryptedPassword(dbConnection);
        
        List<ColumnInfo> columns = new ArrayList<>();
        Set<String> primaryKeys = new HashSet<>();
        
        try (Connection connection = connectionManager.getConnection(dbConnection, decryptedPassword)) {
            DatabaseMetaData metaData = connection.getMetaData();
            
            try (ResultSet rs = metaData.getPrimaryKeys(null, schemaName, tableName)) {
                while (rs.next()) {
                    primaryKeys.add(rs.getString("COLUMN_NAME"));
                }
            }
            
            try (ResultSet rs = metaData.getColumns(null, schemaName, tableName, "%")) {
                while (rs.next()) {
                    ColumnInfo column = new ColumnInfo();
                    String columnName = rs.getString("COLUMN_NAME");
                    
                    column.setColumnName(columnName);
                    column.setDataType(rs.getString("TYPE_NAME"));
                    column.setColumnSize(rs.getInt("COLUMN_SIZE"));
                    column.setDecimalDigits(rs.getInt("DECIMAL_DIGITS"));
                    column.setNullable("YES".equals(rs.getString("IS_NULLABLE")));
                    column.setDefaultValue(rs.getString("COLUMN_DEF"));
                    column.setPrimaryKey(primaryKeys.contains(columnName));
                    column.setAutoIncrement("YES".equals(rs.getString("IS_AUTOINCREMENT")));
                    column.setRemarks(rs.getString("REMARKS"));
                    
                    columns.add(column);
                }
            }
            
        } catch (SQLException e) {
            log.error("Failed to retrieve columns", e);
            throw new DatabaseConnectionException("Failed to retrieve columns", e);
        }
        
        return columns;
    }

    private Long getTableRowCount(Connection connection, String schemaName, String tableName, com.udbmanager.model.DatabaseType dbType) {
        String fullTableName = buildFullTableName(schemaName, tableName, dbType);
        
        String query = "SELECT COUNT(*) FROM " + fullTableName;
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            log.warn("Could not get row count for table: {}", fullTableName);
        }
        
        return null;
    }

    private String buildFullTableName(String schemaName, String tableName, com.udbmanager.model.DatabaseType dbType) {
        String quotedTable = quoteIdentifier(tableName, dbType);
        
        if (schemaName != null && !schemaName.isEmpty()) {
            String quotedSchema = quoteIdentifier(schemaName, dbType);
            return quotedSchema + "." + quotedTable;
        }
        return quotedTable;
    }

    private String quoteIdentifier(String identifier, com.udbmanager.model.DatabaseType dbType) {
        if (identifier == null || identifier.isEmpty()) {
            return identifier;
        }
        
        switch (dbType) {
            case MYSQL:
                return "`" + identifier.replace("`", "``") + "`";
            case SQL_SERVER:
                return "[" + identifier.replace("]", "]]") + "]";
            case POSTGRESQL:
            case ORACLE:
            case SQLITE:
            case H2:
            default:
                return "\"" + identifier.replace("\"", "\"\"") + "\"";
        }
    }

    private boolean isSystemSchema(String schema) {
        if (schema == null) return false;
        
        String lower = schema.toLowerCase();
        return lower.equals("information_schema") ||
               lower.equals("pg_catalog") ||
               lower.equals("pg_toast") ||
               lower.equals("sys") ||
               lower.equals("mysql") ||
               lower.equals("performance_schema");
    }
}
