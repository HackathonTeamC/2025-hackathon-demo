package com.udbmanager.controller;

import com.udbmanager.dto.SqlExecutionRequest;
import com.udbmanager.dto.SqlExecutionResponse;
import com.udbmanager.service.SqlExecutionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/connections/{connectionId}/sql")
@RequiredArgsConstructor
public class SqlController {

    private final SqlExecutionService sqlExecutionService;

    @PostMapping("/execute")
    public ResponseEntity<SqlExecutionResponse> executeSql(
            @PathVariable String connectionId,
            @Valid @RequestBody SqlExecutionRequest request) {
        return ResponseEntity.ok(sqlExecutionService.executeSql(connectionId, request));
    }
}
