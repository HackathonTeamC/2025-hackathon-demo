package com.udb.manager.dto;

public class ConnectionTestResultDTO {
    private Boolean success;
    private String message;
    private String databaseVersion;

    public ConnectionTestResultDTO() {
    }

    public ConnectionTestResultDTO(Boolean success, String message, String databaseVersion) {
        this.success = success;
        this.message = message;
        this.databaseVersion = databaseVersion;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDatabaseVersion() {
        return databaseVersion;
    }

    public void setDatabaseVersion(String databaseVersion) {
        this.databaseVersion = databaseVersion;
    }
}
