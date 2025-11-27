package com.udbmanager.service;

import com.force.api.DescribeGlobal;
import com.force.api.DescribeSObject;
import com.force.api.Field;
import com.force.api.ForceApi;
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
            ForceApi api = salesforceConnectionManager.getForceApi(dbConnection, decryptedPassword);
            DescribeGlobal describeGlobal = api.describeGlobal();
            
            for (DescribeGlobal.SObject sObject : describeGlobal.sobjects) {
                TableInfo table = new TableInfo();
                table.setTableName(sObject.name);
                table.setTableType(sObject.custom ? "CUSTOM" : "STANDARD");
                table.setRemarks(sObject.label);
                // Row count not available in global describe
                tables.add(table);
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
            ForceApi api = salesforceConnectionManager.getForceApi(dbConnection, decryptedPassword);
            DescribeSObject describeSObject = api.describeSObject(objectName);
            
            for (Field field : describeSObject.fields) {
                ColumnInfo column = new ColumnInfo();
                column.setColumnName(field.name);
                column.setDataType(field.type);
                column.setColumnSize(field.length);
                column.setDecimalDigits(field.scale);
                column.setNullable(field.nillable);
                column.setDefaultValue(field.defaultValueFormula);
                column.setPrimaryKey(field.name.equals("Id"));
                column.setAutoIncrement(field.autoNumber);
                column.setRemarks(field.label);
                
                columns.add(column);
            }
            
            log.info("Retrieved {} fields for Salesforce object: {}", columns.size(), objectName);
            
        } catch (Exception e) {
            log.error("Failed to retrieve Salesforce fields for object: {}", objectName, e);
            throw new DatabaseConnectionException("Failed to retrieve Salesforce fields", e);
        }
        
        return columns;
    }
}
