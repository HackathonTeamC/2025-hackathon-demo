package com.udbmanager.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ColumnInfo {
    private String columnName;
    private String dataType;
    private Integer columnSize;
    private Integer decimalDigits;
    private Boolean nullable;
    private String defaultValue;
    private Boolean primaryKey;
    private Boolean autoIncrement;
    private String remarks;
}
