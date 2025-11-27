package com.udbmanager.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SqlExecutionResponse {
    private boolean success;
    private String queryType;
    private List<Map<String, Object>> data;
    private Integer rowsAffected;
    private Long executionTimeMs;
    private String message;
    private String errorMessage;
    
    public static SqlExecutionResponse success(String queryType, List<Map<String, Object>> data, 
                                                int rowsAffected, long executionTimeMs) {
        SqlExecutionResponse response = new SqlExecutionResponse();
        response.setSuccess(true);
        response.setQueryType(queryType);
        response.setData(data);
        response.setRowsAffected(rowsAffected);
        response.setExecutionTimeMs(executionTimeMs);
        response.setMessage("Query executed successfully");
        return response;
    }
    
    public static SqlExecutionResponse error(String errorMessage) {
        SqlExecutionResponse response = new SqlExecutionResponse();
        response.setSuccess(false);
        response.setErrorMessage(errorMessage);
        return response;
    }
}
