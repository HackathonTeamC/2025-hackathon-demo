package com.udbmanager.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DataQueryResponse {
    private List<Map<String, Object>> data;
    private Long totalRecords;
    private Integer page;
    private Integer size;
    private Integer totalPages;
}
