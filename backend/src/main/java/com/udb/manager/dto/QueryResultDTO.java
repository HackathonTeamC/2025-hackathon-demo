package com.udb.manager.dto;

import java.util.List;
import java.util.Map;

public class QueryResultDTO {
    private List<String> columns;
    private List<Map<String, Object>> rows;
    private Integer totalRows;
    private Integer page;
    private Integer pageSize;
    private Long executionTime;

    public QueryResultDTO() {
    }

    public QueryResultDTO(List<String> columns, List<Map<String, Object>> rows, Integer totalRows, 
                         Integer page, Integer pageSize, Long executionTime) {
        this.columns = columns;
        this.rows = rows;
        this.totalRows = totalRows;
        this.page = page;
        this.pageSize = pageSize;
        this.executionTime = executionTime;
    }

    public List<String> getColumns() {
        return columns;
    }

    public void setColumns(List<String> columns) {
        this.columns = columns;
    }

    public List<Map<String, Object>> getRows() {
        return rows;
    }

    public void setRows(List<Map<String, Object>> rows) {
        this.rows = rows;
    }

    public Integer getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(Integer totalRows) {
        this.totalRows = totalRows;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public Long getExecutionTime() {
        return executionTime;
    }

    public void setExecutionTime(Long executionTime) {
        this.executionTime = executionTime;
    }
}
