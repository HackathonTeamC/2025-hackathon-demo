package com.udbmanager.controller;

import com.udbmanager.dto.DataQueryRequest;
import com.udbmanager.dto.DataQueryResponse;
import com.udbmanager.service.DataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/connections/{connectionId}/data")
@RequiredArgsConstructor
public class DataController {

    private final DataService dataService;

    @PostMapping("/tables/{tableName}")
    public ResponseEntity<DataQueryResponse> getTableData(
            @PathVariable String connectionId,
            @PathVariable String tableName,
            @RequestParam(required = false) String schemaName,
            @RequestBody DataQueryRequest request) {
        return ResponseEntity.ok(dataService.getTableData(connectionId, schemaName, tableName, request));
    }

    @PostMapping("/tables/{tableName}/insert")
    public ResponseEntity<Map<String, Object>> insertData(
            @PathVariable String connectionId,
            @PathVariable String tableName,
            @RequestParam(required = false) String schemaName,
            @RequestBody Map<String, Object> data) {
        return ResponseEntity.ok(dataService.insertData(connectionId, schemaName, tableName, data));
    }

    @PutMapping("/tables/{tableName}/update")
    public ResponseEntity<Map<String, Object>> updateData(
            @PathVariable String connectionId,
            @PathVariable String tableName,
            @RequestParam(required = false) String schemaName,
            @RequestBody Map<String, Map<String, Object>> payload) {
        Map<String, Object> data = payload.get("data");
        Map<String, Object> whereConditions = payload.get("whereConditions");
        return ResponseEntity.ok(dataService.updateData(connectionId, schemaName, tableName, data, whereConditions));
    }

    @DeleteMapping("/tables/{tableName}/delete")
    public ResponseEntity<Map<String, Object>> deleteData(
            @PathVariable String connectionId,
            @PathVariable String tableName,
            @RequestParam(required = false) String schemaName,
            @RequestBody Map<String, Object> whereConditions) {
        return ResponseEntity.ok(dataService.deleteData(connectionId, schemaName, tableName, whereConditions));
    }
}
