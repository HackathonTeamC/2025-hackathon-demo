package com.udb.manager.service;

import com.udb.manager.dto.ColumnInfoDTO;
import com.udb.manager.dto.TableInfoDTO;
import com.udb.manager.exception.DatabaseConnectionException;
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
public class DatabaseMetadataService {

    private final DatabaseConnectionService connectionService;

    public List<String> getSchemas(String connectionId) {
        List<String> schemas = new ArrayList<>();
        try (Connection conn = connectionService.getJdbcConnection(connectionId)) {
            DatabaseMetaData metaData = conn.getMetaData();
            try (ResultSet rs = metaData.getSchemas()) {
                while (rs.next()) {
                    schemas.add(rs.getString("TABLE_SCHEM"));
                }
            }
        } catch (Exception e) {
            log.error("Failed to get schemas", e);
            throw new DatabaseConnectionException("Failed to get schemas: " + e.getMessage(), e);
        }
        return schemas;
    }

    public List<TableInfoDTO> getTables(String connectionId, String schema) {
        List<TableInfoDTO> tables = new ArrayList<>();
        try (Connection conn = connectionService.getJdbcConnection(connectionId)) {
            DatabaseMetaData metaData = conn.getMetaData();
            try (ResultSet rs = metaData.getTables(null, schema, "%", new String[]{"TABLE", "VIEW"})) {
                while (rs.next()) {
                    TableInfoDTO table = new TableInfoDTO();
                    table.setTableName(rs.getString("TABLE_NAME"));
                    table.setSchema(rs.getString("TABLE_SCHEM"));
                    table.setTableType(rs.getString("TABLE_TYPE"));
                    
                    try {
                        Long rowCount = getRowCount(conn, table.getSchema(), table.getTableName());
                        table.setRowCount(rowCount);
                    } catch (Exception e) {
                        log.warn("Failed to get row count for table: " + table.getTableName(), e);
                        table.setRowCount(0L);
                    }
                    
                    tables.add(table);
                }
            }
        } catch (Exception e) {
            log.error("Failed to get tables", e);
            throw new DatabaseConnectionException("Failed to get tables: " + e.getMessage(), e);
        }
        return tables;
    }

    public TableInfoDTO getTableDetails(String connectionId, String schema, String tableName) {
        try (Connection conn = connectionService.getJdbcConnection(connectionId)) {
            TableInfoDTO table = new TableInfoDTO();
            table.setTableName(tableName);
            table.setSchema(schema);
            
            DatabaseMetaData metaData = conn.getMetaData();
            
            try (ResultSet rs = metaData.getTables(null, schema, tableName, new String[]{"TABLE", "VIEW"})) {
                if (rs.next()) {
                    table.setTableType(rs.getString("TABLE_TYPE"));
                }
            }
            
            Long rowCount = getRowCount(conn, schema, tableName);
            table.setRowCount(rowCount);
            
            List<ColumnInfoDTO> columns = getColumns(conn, metaData, schema, tableName);
            table.setColumns(columns);
            
            return table;
        } catch (Exception e) {
            log.error("Failed to get table details", e);
            throw new DatabaseConnectionException("Failed to get table details: " + e.getMessage(), e);
        }
    }

    private List<ColumnInfoDTO> getColumns(Connection conn, DatabaseMetaData metaData, String schema, String tableName) throws SQLException {
        List<ColumnInfoDTO> columns = new ArrayList<>();
        
        Set<String> primaryKeys = new HashSet<>();
        try (ResultSet pkRs = metaData.getPrimaryKeys(null, schema, tableName)) {
            while (pkRs.next()) {
                primaryKeys.add(pkRs.getString("COLUMN_NAME"));
            }
        }
        
        try (ResultSet rs = metaData.getColumns(null, schema, tableName, "%")) {
            while (rs.next()) {
                ColumnInfoDTO column = new ColumnInfoDTO();
                column.setColumnName(rs.getString("COLUMN_NAME"));
                column.setDataType(rs.getString("TYPE_NAME"));
                column.setColumnSize(rs.getInt("COLUMN_SIZE"));
                column.setNullable("YES".equalsIgnoreCase(rs.getString("IS_NULLABLE")));
                column.setPrimaryKey(primaryKeys.contains(rs.getString("COLUMN_NAME")));
                column.setAutoIncrement("YES".equalsIgnoreCase(rs.getString("IS_AUTOINCREMENT")));
                column.setDefaultValue(rs.getString("COLUMN_DEF"));
                
                columns.add(column);
            }
        }
        
        return columns;
    }

    private Long getRowCount(Connection conn, String schema, String tableName) throws SQLException {
        String fullTableName = (schema != null && !schema.isEmpty()) 
                ? schema + "." + tableName 
                : tableName;
        
        String query = "SELECT COUNT(*) FROM " + fullTableName;
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        }
        
        return 0L;
    }
}
