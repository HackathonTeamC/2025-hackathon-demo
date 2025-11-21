package com.udb.manager.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ColumnInfoDTO {
    private String columnName;
    private String dataType;
    private Integer columnSize;
    private Boolean nullable;
    private Boolean primaryKey;
    private Boolean autoIncrement;
    private String defaultValue;
}
