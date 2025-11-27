// Database Types
export enum DatabaseType {
  MYSQL = 'MYSQL',
  POSTGRESQL = 'POSTGRESQL',
  SQLITE = 'SQLITE',
  H2 = 'H2',
  ORACLE = 'ORACLE',
  SQL_SERVER = 'SQL_SERVER',
  SALESFORCE = 'SALESFORCE'
}

// Connection Types
export interface ConnectionRequest {
  connectionName: string;
  databaseType: DatabaseType;
  host: string;
  port: number;
  databaseName: string;
  username: string;
  password: string;
  connectionOptions?: string;
  sslEnabled?: boolean;
  timeout?: number;
}

export interface ConnectionResponse {
  id: string;
  connectionName: string;
  databaseType: DatabaseType;
  host: string;
  port: number;
  databaseName: string;
  username: string;
  connectionOptions?: string;
  sslEnabled: boolean;
  timeout: number;
  createdAt: string;
  updatedAt: string;
}

export interface ConnectionTestResult {
  success: boolean;
  message: string;
  errorDetails?: string;
}

// Metadata Types
export interface TableInfo {
  schemaName?: string;
  tableName: string;
  tableType: string;
  rowCount?: number;
  remarks?: string;
}

export interface ColumnInfo {
  columnName: string;
  dataType: string;
  columnSize?: number;
  decimalDigits?: number;
  nullable: boolean;
  defaultValue?: string;
  primaryKey: boolean;
  autoIncrement: boolean;
  remarks?: string;
}

// Data Query Types
export interface DataQueryRequest {
  page?: number;
  size?: number;
  sortColumn?: string;
  sortDirection?: 'ASC' | 'DESC';
  filters?: Record<string, any>;
}

export interface DataQueryResponse {
  data: Record<string, any>[];
  totalRecords: number;
  page: number;
  size: number;
  totalPages: number;
}

// SQL Execution Types
export interface SqlExecutionRequest {
  sql: string;
  maxRows?: number;
}

export interface SqlExecutionResponse {
  success: boolean;
  queryType?: string;
  data?: Record<string, any>[];
  rowsAffected?: number;
  executionTimeMs?: number;
  message?: string;
  errorMessage?: string;
}

// UI State Types
export interface AppState {
  selectedConnection?: ConnectionResponse;
  selectedTable?: TableInfo;
}
