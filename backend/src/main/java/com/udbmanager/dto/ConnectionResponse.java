package com.udbmanager.dto;

import com.udbmanager.model.DatabaseType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionResponse {
    private String id;
    private String connectionName;
    private DatabaseType databaseType;
    private String host;
    private Integer port;
    private String databaseName;
    private String username;
    private String connectionOptions;
    private Boolean sslEnabled;
    private Integer timeout;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
