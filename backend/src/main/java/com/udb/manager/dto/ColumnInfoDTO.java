package com.udb.manager.dto;

public class ColumnInfoDTO {
    private String columnName;
    private String dataType;
    private Integer columnSize;
    private Boolean nullable;
    private Boolean primaryKey;
    private Boolean autoIncrement;
    private String defaultValue;

    public ColumnInfoDTO() {
    }

    public ColumnInfoDTO(String columnName, String dataType, Integer columnSize, Boolean nullable, 
                        Boolean primaryKey, Boolean autoIncrement, String defaultValue) {
        this.columnName = columnName;
        this.dataType = dataType;
        this.columnSize = columnSize;
        this.nullable = nullable;
        this.primaryKey = primaryKey;
        this.autoIncrement = autoIncrement;
        this.defaultValue = defaultValue;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public Integer getColumnSize() {
        return columnSize;
    }

    public void setColumnSize(Integer columnSize) {
        this.columnSize = columnSize;
    }

    public Boolean getNullable() {
        return nullable;
    }

    public void setNullable(Boolean nullable) {
        this.nullable = nullable;
    }

    public Boolean getPrimaryKey() {
        return primaryKey;
    }

    public void setPrimaryKey(Boolean primaryKey) {
        this.primaryKey = primaryKey;
    }

    public Boolean getAutoIncrement() {
        return autoIncrement;
    }

    public void setAutoIncrement(Boolean autoIncrement) {
        this.autoIncrement = autoIncrement;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }
}
