package com.udbmanager.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SqlExecutionRequest {
    @NotBlank(message = "SQL query is required")
    private String sql;
    private Integer maxRows = 1000;
}
