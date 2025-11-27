package com.udbmanager.controller;

import com.udbmanager.dto.ConnectionRequest;
import com.udbmanager.dto.ConnectionResponse;
import com.udbmanager.dto.ConnectionTestResult;
import com.udbmanager.service.ConnectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/connections")
@RequiredArgsConstructor
public class ConnectionController {

    private final ConnectionService connectionService;

    @GetMapping
    public ResponseEntity<List<ConnectionResponse>> getAllConnections() {
        return ResponseEntity.ok(connectionService.getAllConnections());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConnectionResponse> getConnection(@PathVariable String id) {
        return ResponseEntity.ok(connectionService.getConnectionById(id));
    }

    @PostMapping
    public ResponseEntity<ConnectionResponse> createConnection(@Valid @RequestBody ConnectionRequest request) {
        ConnectionResponse response = connectionService.createConnection(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ConnectionResponse> updateConnection(
            @PathVariable String id,
            @Valid @RequestBody ConnectionRequest request) {
        return ResponseEntity.ok(connectionService.updateConnection(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteConnection(@PathVariable String id) {
        connectionService.deleteConnection(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/test")
    public ResponseEntity<ConnectionTestResult> testConnection(@PathVariable String id) {
        return ResponseEntity.ok(connectionService.testConnection(id));
    }

    @PostMapping("/test")
    public ResponseEntity<ConnectionTestResult> testConnectionWithCredentials(
            @Valid @RequestBody ConnectionRequest request) {
        return ResponseEntity.ok(connectionService.testConnectionWithCredentials(request));
    }
}
