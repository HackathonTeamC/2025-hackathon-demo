package com.udb.manager.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QueryResultDTO {
    private List<String> columns;
    private List<Map<String, Object>> rows;
    private Integer totalRows;
    private Integer page;
    private Integer pageSize;
    private Long executionTime;
}
