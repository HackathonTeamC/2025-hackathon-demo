package com.udbmanager.controller;

import com.udbmanager.dto.SqlExecutionRequest;
import com.udbmanager.dto.SqlExecutionResponse;
import com.udbmanager.service.ConnectionService;
import com.udbmanager.service.SalesforceDataService;
import com.udbmanager.service.SqlExecutionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for SQL/SOQL execution
 */
@RestController
@RequestMapping("/api/connections/{connectionId}/query")
@RequiredArgsConstructor
public class SqlExecutionController {

    private final SqlExecutionService sqlExecutionService;
    private final SalesforceDataService salesforceDataService;
    private final ConnectionService connectionService;

    @PostMapping
    public ResponseEntity<SqlExecutionResponse> executeQuery(
            @PathVariable String connectionId,
            @Valid @RequestBody SqlExecutionRequest request) {
        
        com.udbmanager.model.DatabaseConnection connection = connectionService.getConnection(connectionId);
        
        // Check if Salesforce (uses SOQL instead of SQL)
        if (connection.getDatabaseType() == com.udbmanager.model.DatabaseType.SALESFORCE) {
            return ResponseEntity.ok(salesforceDataService.executeSoql(connectionId, request));
        }
        
        return ResponseEntity.ok(sqlExecutionService.executeSql(connectionId, request));
    }
}
