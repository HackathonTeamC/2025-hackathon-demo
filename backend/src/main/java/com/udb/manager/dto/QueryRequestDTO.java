package com.udb.manager.dto;

import lombok.Data;

@Data
public class QueryRequestDTO {
    private String query;
    private Integer page = 0;
    private Integer pageSize = 100;
}
