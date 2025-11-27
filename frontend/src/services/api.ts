import axios, { AxiosResponse } from 'axios';
import {
  ConnectionRequest,
  ConnectionResponse,
  ConnectionTestResult,
  TableInfo,
  ColumnInfo,
  DataQueryRequest,
  DataQueryResponse,
  SqlExecutionRequest,
  SqlExecutionResponse
} from '../types';

const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json'
  }
});

// Connection API
export const connectionApi = {
  getAll: (): Promise<AxiosResponse<ConnectionResponse[]>> => {
    return apiClient.get('/connections');
  },

  getById: (id: string): Promise<AxiosResponse<ConnectionResponse>> => {
    return apiClient.get(`/connections/${id}`);
  },

  create: (request: ConnectionRequest): Promise<AxiosResponse<ConnectionResponse>> => {
    return apiClient.post('/connections', request);
  },

  update: (id: string, request: ConnectionRequest): Promise<AxiosResponse<ConnectionResponse>> => {
    return apiClient.put(`/connections/${id}`, request);
  },

  delete: (id: string): Promise<AxiosResponse<void>> => {
    return apiClient.delete(`/connections/${id}`);
  },

  test: (id: string): Promise<AxiosResponse<ConnectionTestResult>> => {
    return apiClient.post(`/connections/${id}/test`);
  },

  testWithCredentials: (request: ConnectionRequest): Promise<AxiosResponse<ConnectionTestResult>> => {
    return apiClient.post('/connections/test', request);
  }
};

// Metadata API
export const metadataApi = {
  getSchemas: (connectionId: string): Promise<AxiosResponse<string[]>> => {
    return apiClient.get(`/connections/${connectionId}/metadata/schemas`);
  },

  getAllTables: (connectionId: string): Promise<AxiosResponse<TableInfo[]>> => {
    return apiClient.get(`/connections/${connectionId}/metadata/tables`);
  },

  getTables: (connectionId: string, schemaName: string): Promise<AxiosResponse<TableInfo[]>> => {
    return apiClient.get(`/connections/${connectionId}/metadata/schemas/${schemaName}/tables`);
  },

  getColumns: (connectionId: string, tableName: string, schemaName?: string): Promise<AxiosResponse<ColumnInfo[]>> => {
    const params = schemaName ? { schemaName } : {};
    return apiClient.get(`/connections/${connectionId}/metadata/tables/${tableName}/columns`, { params });
  }
};

// Data API
export const dataApi = {
  getTableData: (
    connectionId: string,
    tableName: string,
    request: DataQueryRequest,
    schemaName?: string
  ): Promise<AxiosResponse<DataQueryResponse>> => {
    const params = schemaName ? { schemaName } : {};
    return apiClient.post(`/connections/${connectionId}/data/tables/${tableName}`, request, { params });
  },

  insertData: (
    connectionId: string,
    tableName: string,
    data: Record<string, any>,
    schemaName?: string
  ): Promise<AxiosResponse<{ success: boolean; rowsAffected: number }>> => {
    const params = schemaName ? { schemaName } : {};
    return apiClient.post(`/connections/${connectionId}/data/tables/${tableName}/insert`, data, { params });
  },

  updateData: (
    connectionId: string,
    tableName: string,
    data: Record<string, any>,
    whereConditions: Record<string, any>,
    schemaName?: string
  ): Promise<AxiosResponse<{ success: boolean; rowsAffected: number }>> => {
    const params = schemaName ? { schemaName } : {};
    const payload = { data, whereConditions };
    return apiClient.put(`/connections/${connectionId}/data/tables/${tableName}/update`, payload, { params });
  },

  deleteData: (
    connectionId: string,
    tableName: string,
    whereConditions: Record<string, any>,
    schemaName?: string
  ): Promise<AxiosResponse<{ success: boolean; rowsAffected: number }>> => {
    const params = schemaName ? { schemaName } : {};
    return apiClient.delete(`/connections/${connectionId}/data/tables/${tableName}/delete`, {
      data: whereConditions,
      params
    });
  }
};

// SQL Execution API
export const sqlApi = {
  execute: (connectionId: string, request: SqlExecutionRequest): Promise<AxiosResponse<SqlExecutionResponse>> => {
    return apiClient.post(`/connections/${connectionId}/sql/execute`, request);
  }
};

export default apiClient;
