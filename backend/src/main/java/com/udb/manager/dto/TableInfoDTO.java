package com.udb.manager.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TableInfoDTO {
    private String tableName;
    private String schema;
    private String tableType;
    private Long rowCount;
    private List<ColumnInfoDTO> columns;
}
