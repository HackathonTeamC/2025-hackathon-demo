package com.udb.manager.dto;

import com.udb.manager.model.DatabaseType;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

@Data
public class DatabaseConnectionDTO {
    private String id;
    private String connectionName;
    private DatabaseType databaseType;
    private String host;
    private Integer port;
    private String databaseName;
    private String username;
    private String password;
    private Map<String, String> options;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
