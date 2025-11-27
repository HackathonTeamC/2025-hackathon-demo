package com.udbmanager.controller;

import com.udbmanager.dto.ColumnInfo;
import com.udbmanager.dto.TableInfo;
import com.udbmanager.service.MetadataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/connections/{connectionId}/metadata")
@RequiredArgsConstructor
public class MetadataController {

    private final MetadataService metadataService;
    private final com.udbmanager.service.SalesforceMetadataService salesforceMetadataService;
    private final com.udbmanager.service.ConnectionService connectionService;

    @GetMapping("/schemas")
    public ResponseEntity<List<String>> getSchemas(@PathVariable String connectionId) {
        return ResponseEntity.ok(metadataService.getSchemas(connectionId));
    }

    @GetMapping("/tables")
    public ResponseEntity<List<TableInfo>> getAllTables(@PathVariable String connectionId) {
        com.udbmanager.model.DatabaseConnection connection = connectionService.getConnection(connectionId);
        if (connection.getDatabaseType() == com.udbmanager.model.DatabaseType.SALESFORCE) {
            return ResponseEntity.ok(salesforceMetadataService.getSalesforceObjects(connectionId));
        }
        return ResponseEntity.ok(metadataService.getAllTables(connectionId));
    }

    @GetMapping("/schemas/{schemaName}/tables")
    public ResponseEntity<List<TableInfo>> getTables(
            @PathVariable String connectionId,
            @PathVariable String schemaName) {
        return ResponseEntity.ok(metadataService.getTables(connectionId, schemaName));
    }

    @GetMapping("/tables/{tableName}/columns")
    public ResponseEntity<List<ColumnInfo>> getColumns(
            @PathVariable String connectionId,
            @PathVariable String tableName,
            @RequestParam(required = false) String schemaName) {
        com.udbmanager.model.DatabaseConnection connection = connectionService.getConnection(connectionId);
        if (connection.getDatabaseType() == com.udbmanager.model.DatabaseType.SALESFORCE) {
            return ResponseEntity.ok(salesforceMetadataService.getSalesforceFields(connectionId, tableName));
        }
        return ResponseEntity.ok(metadataService.getColumns(connectionId, schemaName, tableName));
    }
}
