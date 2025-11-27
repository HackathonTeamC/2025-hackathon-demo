package com.udbmanager.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionTestResult {
    private boolean success;
    private String message;
    private String errorDetails;

    public static ConnectionTestResult success() {
        return new ConnectionTestResult(true, "Connection successful", null);
    }

    public static ConnectionTestResult failure(String message, String errorDetails) {
        return new ConnectionTestResult(false, message, errorDetails);
    }
}
