import React, { useState, useEffect } from 'react';
import {
  Box,
  TextField,
  Button,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  FormControlLabel,
  Switch,
  Alert,
  CircularProgress
} from '@mui/material';
import { ConnectionRequest, ConnectionResponse, DatabaseType } from '../../types';
import { connectionApi } from '../../services/api';

interface ConnectionFormProps {
  connection?: ConnectionResponse | null;
  onSuccess: () => void;
  onCancel: () => void;
}

const ConnectionForm: React.FC<ConnectionFormProps> = ({ connection, onSuccess, onCancel }) => {
  const [formData, setFormData] = useState<ConnectionRequest>({
    connectionName: '',
    databaseType: DatabaseType.MYSQL,
    host: 'localhost',
    port: 3306,
    databaseName: '',
    username: '',
    password: '',
    connectionOptions: '',
    sslEnabled: false,
    timeout: 30
  });

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [testResult, setTestResult] = useState<{ success: boolean; message: string } | null>(null);

  useEffect(() => {
    if (connection) {
      setFormData({
        connectionName: connection.connectionName,
        databaseType: connection.databaseType,
        host: connection.host,
        port: connection.port,
        databaseName: connection.databaseName,
        username: connection.username,
        password: '', // Don't populate password for security
        connectionOptions: connection.connectionOptions || '',
        sslEnabled: connection.sslEnabled,
        timeout: connection.timeout
      });
    }
  }, [connection]);

  useEffect(() => {
    // Update default port based on database type
    const defaultPorts: Record<DatabaseType, number> = {
      [DatabaseType.MYSQL]: 3306,
      [DatabaseType.POSTGRESQL]: 5432,
      [DatabaseType.SQLITE]: 0,
      [DatabaseType.H2]: 9092,
      [DatabaseType.ORACLE]: 1521,
      [DatabaseType.SQL_SERVER]: 1433,
      [DatabaseType.SALESFORCE]: 443
    };
    
    if (!connection) {
      setFormData((prev) => ({ ...prev, port: defaultPorts[prev.databaseType] }));
    }
  }, [formData.databaseType, connection]);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | { name?: string; value: unknown }>) => {
    const { name, value } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name as string]: value
    }));
  };

  const handleSwitchChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, checked } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: checked
    }));
  };

  const handleTest = async () => {
    setLoading(true);
    setError(null);
    setTestResult(null);

    try {
      const response = await connectionApi.testWithCredentials(formData);
      setTestResult(response.data);
    } catch (err: any) {
      console.error('Connection test failed', err);
      setError(err.response?.data?.message || 'Connection test failed');
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);

    try {
      if (connection) {
        await connectionApi.update(connection.id, formData);
      } else {
        await connectionApi.create(formData);
      }
      onSuccess();
    } catch (err: any) {
      console.error('Failed to save connection', err);
      setError(err.response?.data?.message || 'Failed to save connection');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box component="form" onSubmit={handleSubmit} sx={{ mt: 2 }}>
      {error && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}

      {testResult && (
        <Alert severity={testResult.success ? 'success' : 'error'} sx={{ mb: 2 }}>
          {testResult.message}
          {testResult.success === false && testResult && 'errorDetails' in testResult && (
            <Box mt={1} fontSize="0.875rem">
              {(testResult as any).errorDetails}
            </Box>
          )}
        </Alert>
      )}

      <TextField
        fullWidth
        required
        label="Connection Name"
        name="connectionName"
        value={formData.connectionName}
        onChange={handleChange}
        margin="normal"
      />

      <FormControl fullWidth required margin="normal">
        <InputLabel>Database Type</InputLabel>
        <Select
          name="databaseType"
          value={formData.databaseType}
          label="Database Type"
          onChange={handleChange as any}
        >
          <MenuItem value={DatabaseType.MYSQL}>MySQL</MenuItem>
          <MenuItem value={DatabaseType.POSTGRESQL}>PostgreSQL</MenuItem>
          <MenuItem value={DatabaseType.SQLITE}>SQLite</MenuItem>
          <MenuItem value={DatabaseType.H2}>H2</MenuItem>
          <MenuItem value={DatabaseType.ORACLE}>Oracle Database</MenuItem>
          <MenuItem value={DatabaseType.SQL_SERVER}>Microsoft SQL Server</MenuItem>
          <MenuItem value={DatabaseType.SALESFORCE}>Salesforce</MenuItem>
        </Select>
      </FormControl>

      <TextField
        fullWidth
        required
        label={formData.databaseType === DatabaseType.SALESFORCE ? "Instance URL" : "Host"}
        name="host"
        value={formData.host}
        onChange={handleChange}
        margin="normal"
        helperText={formData.databaseType === DatabaseType.SALESFORCE ? "e.g., login.salesforce.com or test.salesforce.com" : ""}
      />

      {formData.databaseType !== DatabaseType.SALESFORCE && (
        <TextField
          fullWidth
          required
          type="number"
          label="Port"
          name="port"
          value={formData.port}
          onChange={handleChange}
          margin="normal"
        />
      )}

      {formData.databaseType !== DatabaseType.SALESFORCE && (
        <TextField
          fullWidth
          required
          label="Database Name"
          name="databaseName"
          value={formData.databaseName}
          onChange={handleChange}
          margin="normal"
        />
      )}

      <TextField
        fullWidth
        required
        label="Username"
        name="username"
        value={formData.username}
        onChange={handleChange}
        margin="normal"
      />

      <TextField
        fullWidth
        required={!connection}
        type="password"
        label="Password"
        name="password"
        value={formData.password}
        onChange={handleChange}
        margin="normal"
        helperText={
          connection 
            ? 'Leave empty to keep current password' 
            : formData.databaseType === DatabaseType.SALESFORCE
              ? 'Enter: YourPassword + SecurityToken (concatenated, no space)'
              : ''
        }
      />

      <TextField
        fullWidth
        label="Connection Options"
        name="connectionOptions"
        value={formData.connectionOptions}
        onChange={handleChange}
        margin="normal"
        helperText={
          formData.databaseType === DatabaseType.SALESFORCE
            ? "Optional: Additional connection parameters (e.g., api_version=v57.0)"
            : "Additional JDBC parameters (e.g., useUnicode=true&characterEncoding=UTF-8)"
        }
      />

      <FormControlLabel
        control={
          <Switch
            checked={formData.sslEnabled}
            onChange={handleSwitchChange}
            name="sslEnabled"
          />
        }
        label="Enable SSL"
      />

      <TextField
        fullWidth
        type="number"
        label="Timeout (seconds)"
        name="timeout"
        value={formData.timeout}
        onChange={handleChange}
        margin="normal"
      />

      <Box display="flex" gap={2} mt={3}>
        <Button
          variant="outlined"
          onClick={handleTest}
          disabled={loading}
          fullWidth
        >
          {loading ? <CircularProgress size={24} /> : 'Test Connection'}
        </Button>
        <Button type="submit" variant="contained" disabled={loading} fullWidth>
          {loading ? <CircularProgress size={24} /> : connection ? 'Update' : 'Create'}
        </Button>
        <Button variant="outlined" onClick={onCancel} disabled={loading}>
          Cancel
        </Button>
      </Box>
    </Box>
  );
};

export default ConnectionForm;
