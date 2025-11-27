package com.udbmanager.service;

import com.udbmanager.dto.ConnectionRequest;
import com.udbmanager.dto.ConnectionResponse;
import com.udbmanager.dto.ConnectionTestResult;
import com.udbmanager.exception.DatabaseConnectionException;
import com.udbmanager.exception.ResourceNotFoundException;
import com.udbmanager.model.DatabaseConnection;
import com.udbmanager.repository.DatabaseConnectionRepository;
import com.udbmanager.util.ConnectionMapper;
import com.udbmanager.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConnectionService {

    private final DatabaseConnectionRepository connectionRepository;
    private final DatabaseConnectionManager connectionManager;
    private final SalesforceConnectionManager salesforceConnectionManager;
    private final EncryptionUtil encryptionUtil;
    private final ConnectionMapper connectionMapper;

    public List<ConnectionResponse> getAllConnections() {
        return connectionRepository.findAll().stream()
                .map(connectionMapper::toResponse)
                .collect(Collectors.toList());
    }

    public ConnectionResponse getConnectionById(String id) {
        DatabaseConnection connection = connectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Connection not found with id: " + id));
        return connectionMapper.toResponse(connection);
    }

    @Transactional
    public ConnectionResponse createConnection(ConnectionRequest request) {
        if (connectionRepository.existsByConnectionName(request.getConnectionName())) {
            throw new DatabaseConnectionException("Connection with name '" + request.getConnectionName() + "' already exists");
        }

        String encryptedPassword = encryptionUtil.encrypt(request.getPassword());

        DatabaseConnection connection = new DatabaseConnection();
        connection.setConnectionName(request.getConnectionName());
        connection.setDatabaseType(request.getDatabaseType());
        connection.setHost(request.getHost());
        connection.setPort(request.getPort());
        connection.setDatabaseName(request.getDatabaseName());
        connection.setUsername(request.getUsername());
        connection.setEncryptedPassword(encryptedPassword);
        connection.setConnectionOptions(request.getConnectionOptions());
        connection.setSslEnabled(request.getSslEnabled());
        connection.setTimeout(request.getTimeout());

        DatabaseConnection saved = connectionRepository.save(connection);
        log.info("Created new connection: {}", saved.getConnectionName());

        return connectionMapper.toResponse(saved);
    }

    @Transactional
    public ConnectionResponse updateConnection(String id, ConnectionRequest request) {
        DatabaseConnection connection = connectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Connection not found with id: " + id));

        if (!connection.getConnectionName().equals(request.getConnectionName()) &&
            connectionRepository.existsByConnectionName(request.getConnectionName())) {
            throw new DatabaseConnectionException("Connection with name '" + request.getConnectionName() + "' already exists");
        }

        connection.setConnectionName(request.getConnectionName());
        connection.setDatabaseType(request.getDatabaseType());
        connection.setHost(request.getHost());
        connection.setPort(request.getPort());
        connection.setDatabaseName(request.getDatabaseName());
        connection.setUsername(request.getUsername());
        connection.setConnectionOptions(request.getConnectionOptions());
        connection.setSslEnabled(request.getSslEnabled());
        connection.setTimeout(request.getTimeout());

        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            String encryptedPassword = encryptionUtil.encrypt(request.getPassword());
            connection.setEncryptedPassword(encryptedPassword);
        }

        connectionManager.closeConnection(id);

        DatabaseConnection updated = connectionRepository.save(connection);
        log.info("Updated connection: {}", updated.getConnectionName());

        return connectionMapper.toResponse(updated);
    }

    @Transactional
    public void deleteConnection(String id) {
        if (!connectionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Connection not found with id: " + id);
        }

        connectionManager.closeConnection(id);
        connectionRepository.deleteById(id);
        log.info("Deleted connection with id: {}", id);
    }

    public ConnectionTestResult testConnection(String id) {
        DatabaseConnection connection = connectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Connection not found with id: " + id));
        return testConnectionInternal(connection);
    }

    public ConnectionTestResult testConnectionWithCredentials(ConnectionRequest request) {
        DatabaseConnection tempConnection = new DatabaseConnection();
        tempConnection.setDatabaseType(request.getDatabaseType());
        tempConnection.setHost(request.getHost());
        tempConnection.setPort(request.getPort());
        tempConnection.setDatabaseName(request.getDatabaseName());
        tempConnection.setUsername(request.getUsername());
        tempConnection.setEncryptedPassword(request.getPassword());
        tempConnection.setSslEnabled(request.getSslEnabled());
        tempConnection.setTimeout(request.getTimeout());
        tempConnection.setConnectionOptions(request.getConnectionOptions());

        return testConnectionInternal(tempConnection, request.getPassword());
    }

    private ConnectionTestResult testConnectionInternal(DatabaseConnection connection) {
        String decryptedPassword = encryptionUtil.decrypt(connection.getEncryptedPassword());
        return testConnectionInternal(connection, decryptedPassword);
    }

    private ConnectionTestResult testConnectionInternal(DatabaseConnection connection, String password) {
        try {
            // Check if Salesforce
            if (connection.getDatabaseType() == com.udbmanager.model.DatabaseType.SALESFORCE) {
                salesforceConnectionManager.testConnection(connection, password);
            } else {
                connectionManager.testConnection(connection, password);
            }
            return ConnectionTestResult.success();
        } catch (SQLException e) {
            log.error("Connection test failed", e);
            return ConnectionTestResult.failure("Connection failed", e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during connection test", e);
            return ConnectionTestResult.failure("Unexpected error", e.getMessage());
        }
    }

    public DatabaseConnection getConnection(String id) {
        return connectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Connection not found with id: " + id));
    }

    public String getDecryptedPassword(DatabaseConnection connection) {
        return encryptionUtil.decrypt(connection.getEncryptedPassword());
    }
}
