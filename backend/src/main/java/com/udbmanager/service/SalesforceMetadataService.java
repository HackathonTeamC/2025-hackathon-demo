package com.udbmanager.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.udbmanager.dto.ColumnInfo;
import com.udbmanager.dto.TableInfo;
import com.udbmanager.exception.DatabaseConnectionException;
import com.udbmanager.model.DatabaseConnection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for retrieving Salesforce metadata (objects and fields)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SalesforceMetadataService {

    private final ConnectionService connectionService;
    private final SalesforceConnectionManager salesforceConnectionManager;

    /**
     * Get list of Salesforce objects (equivalent to tables)
     */
    public List<TableInfo> getSalesforceObjects(String connectionId) {
        DatabaseConnection dbConnection = connectionService.getConnection(connectionId);
        String decryptedPassword = connectionService.getDecryptedPassword(dbConnection);
        
        List<TableInfo> tables = new ArrayList<>();
        
        try {
            SalesforceConnectionManager.SalesforceSession session = 
                    salesforceConnectionManager.getSession(dbConnection, decryptedPassword);
            
            JsonNode response = salesforceConnectionManager.describeGlobal(session);
            JsonNode sobjects = response.get("sobjects");
            
            if (sobjects != null && sobjects.isArray()) {
                for (JsonNode sObject : sobjects) {
                    TableInfo table = new TableInfo();
                    table.setTableName(sObject.get("name").asText());
                    table.setTableType(sObject.get("custom").asBoolean() ? "CUSTOM" : "STANDARD");
                    table.setRemarks(sObject.get("label").asText());
                    tables.add(table);
                }
            }
            
            log.info("Retrieved {} Salesforce objects", tables.size());
            
        } catch (Exception e) {
            log.error("Failed to retrieve Salesforce objects", e);
            throw new DatabaseConnectionException("Failed to retrieve Salesforce objects", e);
        }
        
        return tables;
    }

    /**
     * Get field information for a Salesforce object (equivalent to columns)
     */
    public List<ColumnInfo> getSalesforceFields(String connectionId, String objectName) {
        DatabaseConnection dbConnection = connectionService.getConnection(connectionId);
        String decryptedPassword = connectionService.getDecryptedPassword(dbConnection);
        
        List<ColumnInfo> columns = new ArrayList<>();
        
        try {
            SalesforceConnectionManager.SalesforceSession session = 
                    salesforceConnectionManager.getSession(dbConnection, decryptedPassword);
            
            JsonNode response = salesforceConnectionManager.describeSObject(session, objectName);
            JsonNode fields = response.get("fields");
            
            if (fields != null && fields.isArray()) {
                for (JsonNode field : fields) {
                    ColumnInfo column = new ColumnInfo();
                    column.setColumnName(field.get("name").asText());
                    column.setDataType(field.get("type").asText());
                    column.setColumnSize(field.has("length") ? field.get("length").asInt() : null);
                    column.setDecimalDigits(field.has("scale") ? field.get("scale").asInt() : null);
                    column.setNullable(field.get("nillable").asBoolean());
                    column.setDefaultValue(field.has("defaultValueFormula") ? 
                            field.get("defaultValueFormula").asText() : null);
                    column.setPrimaryKey(field.get("name").asText().equals("Id"));
                    column.setAutoIncrement(field.has("autoNumber") && field.get("autoNumber").asBoolean());
                    column.setRemarks(field.get("label").asText());
                    
                    columns.add(column);
                }
            }
            
            log.info("Retrieved {} fields for Salesforce object: {}", columns.size(), objectName);
            
        } catch (Exception e) {
            log.error("Failed to retrieve Salesforce fields for object: {}", objectName, e);
            throw new DatabaseConnectionException("Failed to retrieve Salesforce fields", e);
        }
        
        return columns;
    }
}
