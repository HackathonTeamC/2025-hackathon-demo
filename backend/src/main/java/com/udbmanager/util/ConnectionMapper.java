package com.udbmanager.util;

import com.udbmanager.dto.ConnectionResponse;
import com.udbmanager.model.DatabaseConnection;
import org.springframework.stereotype.Component;

@Component
public class ConnectionMapper {

    public ConnectionResponse toResponse(DatabaseConnection connection) {
        ConnectionResponse response = new ConnectionResponse();
        response.setId(connection.getId());
        response.setConnectionName(connection.getConnectionName());
        response.setDatabaseType(connection.getDatabaseType());
        response.setHost(connection.getHost());
        response.setPort(connection.getPort());
        response.setDatabaseName(connection.getDatabaseName());
        response.setUsername(connection.getUsername());
        response.setConnectionOptions(connection.getConnectionOptions());
        response.setSslEnabled(connection.getSslEnabled());
        response.setTimeout(connection.getTimeout());
        response.setCreatedAt(connection.getCreatedAt());
        response.setUpdatedAt(connection.getUpdatedAt());
        return response;
    }
}
