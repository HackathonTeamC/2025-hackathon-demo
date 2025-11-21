package com.udb.manager.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionTestResultDTO {
    private Boolean success;
    private String message;
    private String databaseVersion;
}
