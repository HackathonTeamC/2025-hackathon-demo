package com.udb.manager.controller;

import com.udb.manager.dto.ConnectionTestResultDTO;
import com.udb.manager.dto.DatabaseConnectionDTO;
import com.udb.manager.service.DatabaseConnectionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/connections")
public class DatabaseConnectionController {

    private final DatabaseConnectionService connectionService;

    public DatabaseConnectionController(DatabaseConnectionService connectionService) {
        this.connectionService = connectionService;
    }

    @PostMapping
    public ResponseEntity<DatabaseConnectionDTO> createConnection(@RequestBody DatabaseConnectionDTO dto) {
        DatabaseConnectionDTO created = connectionService.createConnection(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<DatabaseConnectionDTO>> getAllConnections() {
        List<DatabaseConnectionDTO> connections = connectionService.getAllConnections();
        return ResponseEntity.ok(connections);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DatabaseConnectionDTO> getConnection(@PathVariable String id) {
        return connectionService.getConnection(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<DatabaseConnectionDTO> updateConnection(
            @PathVariable String id,
            @RequestBody DatabaseConnectionDTO dto) {
        DatabaseConnectionDTO updated = connectionService.updateConnection(id, dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteConnection(@PathVariable String id) {
        connectionService.deleteConnection(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/test")
    public ResponseEntity<ConnectionTestResultDTO> testConnection(@RequestBody DatabaseConnectionDTO dto) {
        ConnectionTestResultDTO result = connectionService.testConnection(dto);
        return ResponseEntity.ok(result);
    }
}
