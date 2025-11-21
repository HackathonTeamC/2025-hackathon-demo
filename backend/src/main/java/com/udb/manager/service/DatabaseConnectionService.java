package com.udb.manager.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.udb.manager.dto.ConnectionTestResultDTO;
import com.udb.manager.dto.DatabaseConnectionDTO;
import com.udb.manager.exception.DatabaseConnectionException;
import com.udb.manager.model.DatabaseConnection;
import com.udb.manager.model.DatabaseType;
import com.udb.manager.repository.DatabaseConnectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jasypt.encryption.StringEncryptor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DatabaseConnectionService {

    private final DatabaseConnectionRepository repository;
    private final StringEncryptor encryptor;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public DatabaseConnectionDTO createConnection(DatabaseConnectionDTO dto) {
        DatabaseConnection entity = convertToEntity(dto);
        entity.setPassword(encryptor.encrypt(dto.getPassword()));
        
        DatabaseConnection saved = repository.save(entity);
        return convertToDTO(saved);
    }

    public List<DatabaseConnectionDTO> getAllConnections() {
        return repository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public Optional<DatabaseConnectionDTO> getConnection(String id) {
        return repository.findById(id).map(this::convertToDTO);
    }

    @Transactional
    public DatabaseConnectionDTO updateConnection(String id, DatabaseConnectionDTO dto) {
        DatabaseConnection entity = repository.findById(id)
                .orElseThrow(() -> new DatabaseConnectionException("Connection not found: " + id));

        entity.setConnectionName(dto.getConnectionName());
        entity.setDatabaseType(dto.getDatabaseType());
        entity.setHost(dto.getHost());
        entity.setPort(dto.getPort());
        entity.setDatabaseName(dto.getDatabaseName());
        entity.setUsername(dto.getUsername());
        
        if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
            entity.setPassword(encryptor.encrypt(dto.getPassword()));
        }
        
        if (dto.getOptions() != null) {
            try {
                entity.setOptions(objectMapper.writeValueAsString(dto.getOptions()));
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize options", e);
            }
        }

        DatabaseConnection updated = repository.save(entity);
        return convertToDTO(updated);
    }

    @Transactional
    public void deleteConnection(String id) {
        repository.deleteById(id);
    }

    public ConnectionTestResultDTO testConnection(DatabaseConnectionDTO dto) {
        try {
            String jdbcUrl = dto.getDatabaseType().buildJdbcUrl(
                    dto.getHost(), dto.getPort(), dto.getDatabaseName());
            
            Class.forName(dto.getDatabaseType().getDriverClassName());
            
            try (Connection conn = DriverManager.getConnection(
                    jdbcUrl, dto.getUsername(), dto.getPassword())) {
                
                DatabaseMetaData metaData = conn.getMetaData();
                String version = metaData.getDatabaseProductName() + " " + metaData.getDatabaseProductVersion();
                
                return new ConnectionTestResultDTO(true, "Connection successful", version);
            }
        } catch (Exception e) {
            log.error("Connection test failed", e);
            return new ConnectionTestResultDTO(false, "Connection failed: " + e.getMessage(), null);
        }
    }

    public Connection getJdbcConnection(String connectionId) throws Exception {
        DatabaseConnection entity = repository.findById(connectionId)
                .orElseThrow(() -> new DatabaseConnectionException("Connection not found: " + connectionId));

        String jdbcUrl = entity.getDatabaseType().buildJdbcUrl(
                entity.getHost(), entity.getPort(), entity.getDatabaseName());
        
        Class.forName(entity.getDatabaseType().getDriverClassName());
        
        String decryptedPassword = encryptor.decrypt(entity.getPassword());
        
        return DriverManager.getConnection(jdbcUrl, entity.getUsername(), decryptedPassword);
    }

    private DatabaseConnectionDTO convertToDTO(DatabaseConnection entity) {
        DatabaseConnectionDTO dto = new DatabaseConnectionDTO();
        dto.setId(entity.getId());
        dto.setConnectionName(entity.getConnectionName());
        dto.setDatabaseType(entity.getDatabaseType());
        dto.setHost(entity.getHost());
        dto.setPort(entity.getPort());
        dto.setDatabaseName(entity.getDatabaseName());
        dto.setUsername(entity.getUsername());
        dto.setPassword("********");
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        
        if (entity.getOptions() != null) {
            try {
                dto.setOptions(objectMapper.readValue(entity.getOptions(), Map.class));
            } catch (JsonProcessingException e) {
                log.error("Failed to deserialize options", e);
            }
        }
        
        return dto;
    }

    private DatabaseConnection convertToEntity(DatabaseConnectionDTO dto) {
        DatabaseConnection entity = new DatabaseConnection();
        entity.setConnectionName(dto.getConnectionName());
        entity.setDatabaseType(dto.getDatabaseType());
        entity.setHost(dto.getHost());
        entity.setPort(dto.getPort());
        entity.setDatabaseName(dto.getDatabaseName());
        entity.setUsername(dto.getUsername());
        entity.setPassword(dto.getPassword());
        
        if (dto.getOptions() != null) {
            try {
                entity.setOptions(objectMapper.writeValueAsString(dto.getOptions()));
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize options", e);
            }
        }
        
        return entity;
    }
}
