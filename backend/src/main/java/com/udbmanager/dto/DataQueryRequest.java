package com.udbmanager.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DataQueryRequest {
    private Integer page = 0;
    private Integer size = 100;
    private String sortColumn;
    private String sortDirection = "ASC";
    private Map<String, Object> filters;
}
