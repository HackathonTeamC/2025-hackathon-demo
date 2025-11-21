package com.udb.manager.controller;

import com.udb.manager.dto.QueryRequestDTO;
import com.udb.manager.dto.QueryResultDTO;
import com.udb.manager.service.QueryExecutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/connections/{connectionId}")
@RequiredArgsConstructor
public class QueryExecutionController {

    private final QueryExecutionService queryService;

    @PostMapping("/query")
    public ResponseEntity<QueryResultDTO> executeQuery(
            @PathVariable String connectionId,
            @RequestBody QueryRequestDTO request) {
        QueryResultDTO result = queryService.executeQuery(connectionId, request);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/data/{tableName}")
    public ResponseEntity<QueryResultDTO> getTableData(
            @PathVariable String connectionId,
            @PathVariable String tableName,
            @RequestParam(required = false) String schema,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "100") Integer pageSize) {
        QueryResultDTO result = queryService.getTableData(connectionId, tableName, schema, page, pageSize);
        return ResponseEntity.ok(result);
    }
}
