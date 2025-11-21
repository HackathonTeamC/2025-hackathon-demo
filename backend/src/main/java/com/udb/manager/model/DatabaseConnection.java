package com.udb.manager.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "database_connections")
public class DatabaseConnection {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String connectionName;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private DatabaseType databaseType;

    @Column(nullable = false)
    private String host;

    @Column(nullable = false)
    private Integer port;

    private String databaseName;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(length = 1000)
    private String options;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public DatabaseConnection() {
    }

    public DatabaseConnection(String id, String connectionName, DatabaseType databaseType, String host, 
                            Integer port, String databaseName, String username, String password, 
                            String options, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.connectionName = connectionName;
        this.databaseType = databaseType;
        this.host = host;
        this.port = port;
        this.databaseName = databaseName;
        this.username = username;
        this.password = password;
        this.options = options;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getConnectionName() {
        return connectionName;
    }

    public void setConnectionName(String connectionName) {
        this.connectionName = connectionName;
    }

    public DatabaseType getDatabaseType() {
        return databaseType;
    }

    public void setDatabaseType(DatabaseType databaseType) {
        this.databaseType = databaseType;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getOptions() {
        return options;
    }

    public void setOptions(String options) {
        this.options = options;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
