package com.udb.manager.controller;

import com.udb.manager.dto.TableInfoDTO;
import com.udb.manager.service.DatabaseMetadataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/connections/{connectionId}/metadata")
@RequiredArgsConstructor
public class DatabaseMetadataController {

    private final DatabaseMetadataService metadataService;

    @GetMapping("/schemas")
    public ResponseEntity<List<String>> getSchemas(@PathVariable String connectionId) {
        List<String> schemas = metadataService.getSchemas(connectionId);
        return ResponseEntity.ok(schemas);
    }

    @GetMapping("/tables")
    public ResponseEntity<List<TableInfoDTO>> getTables(
            @PathVariable String connectionId,
            @RequestParam(required = false) String schema) {
        List<TableInfoDTO> tables = metadataService.getTables(connectionId, schema);
        return ResponseEntity.ok(tables);
    }

    @GetMapping("/tables/{tableName}")
    public ResponseEntity<TableInfoDTO> getTableDetails(
            @PathVariable String connectionId,
            @PathVariable String tableName,
            @RequestParam(required = false) String schema) {
        TableInfoDTO table = metadataService.getTableDetails(connectionId, schema, tableName);
        return ResponseEntity.ok(table);
    }
}
