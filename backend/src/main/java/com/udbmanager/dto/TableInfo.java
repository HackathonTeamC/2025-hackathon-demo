package com.udbmanager.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TableInfo {
    private String schemaName;
    private String tableName;
    private String tableType;
    private Long rowCount;
    private String remarks;
}
