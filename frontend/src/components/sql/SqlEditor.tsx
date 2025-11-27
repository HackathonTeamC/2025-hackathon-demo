import React, { useState } from 'react';
import CodeMirror from '@uiw/react-codemirror';
import { sql } from '@codemirror/lang-sql';
import {
  Box,
  Paper,
  Button,
  Typography,
  Alert,
  CircularProgress,
  Chip
} from '@mui/material';
import { PlayArrow } from '@mui/icons-material';
import { AgGridReact } from 'ag-grid-react';
import 'ag-grid-community/styles/ag-grid.css';
import 'ag-grid-community/styles/ag-theme-material.css';
import { sqlApi } from '../../services/api';
import { SqlExecutionRequest, SqlExecutionResponse } from '../../types';
import { ColDef } from 'ag-grid-community';

interface SqlEditorProps {
  connectionId: string;
}

const SqlEditor: React.FC<SqlEditorProps> = ({ connectionId }) => {
  const [sqlQuery, setSqlQuery] = useState('SELECT * FROM your_table LIMIT 10;');
  const [executing, setExecuting] = useState(false);
  const [result, setResult] = useState<SqlExecutionResponse | null>(null);
  const [columnDefs, setColumnDefs] = useState<ColDef[]>([]);

  const handleExecute = async () => {
    if (!sqlQuery.trim()) {
      return;
    }

    setExecuting(true);
    setResult(null);

    try {
      const request: SqlExecutionRequest = {
        sql: sqlQuery,
        maxRows: 1000
      };

      const response = await sqlApi.execute(connectionId, request);
      setResult(response.data);

      // If query returned data, set up grid columns
      if (response.data.success && response.data.data && response.data.data.length > 0) {
        const columns: ColDef[] = Object.keys(response.data.data[0]).map((key) => ({
          field: key,
          headerName: key,
          sortable: true,
          filter: true,
          resizable: true,
          minWidth: 150
        }));
        setColumnDefs(columns);
      }
    } catch (error: any) {
      console.error('SQL execution failed', error);
      setResult({
        success: false,
        errorMessage: error.response?.data?.message || 'SQL execution failed'
      });
    } finally {
      setExecuting(false);
    }
  };

  return (
    <Box>
      <Paper elevation={3} sx={{ p: 2, mb: 2 }}>
        <Box mb={2} display="flex" justifyContent="space-between" alignItems="center">
          <Typography variant="h6">SQL Editor</Typography>
          <Button
            variant="contained"
            startIcon={executing ? <CircularProgress size={20} /> : <PlayArrow />}
            onClick={handleExecute}
            disabled={executing}
          >
            Execute
          </Button>
        </Box>

        <Box sx={{ border: '1px solid #ddd', borderRadius: 1, overflow: 'hidden' }}>
          <CodeMirror
            value={sqlQuery}
            height="200px"
            extensions={[sql()]}
            onChange={(value) => setSqlQuery(value)}
            theme="light"
          />
        </Box>
      </Paper>

      {result && (
        <Paper elevation={3} sx={{ p: 2 }}>
          <Box mb={2}>
            <Typography variant="h6" gutterBottom>
              Results
            </Typography>
            {result.success ? (
              <Box display="flex" gap={1} flexWrap="wrap">
                <Chip label={`Type: ${result.queryType}`} color="primary" size="small" />
                {result.executionTimeMs !== undefined && (
                  <Chip label={`Execution Time: ${result.executionTimeMs}ms`} size="small" />
                )}
                {result.rowsAffected !== undefined && (
                  <Chip label={`Rows Affected: ${result.rowsAffected}`} size="small" />
                )}
              </Box>
            ) : (
              <Alert severity="error">{result.errorMessage}</Alert>
            )}
          </Box>

          {result.success && result.data && result.data.length > 0 && (
            <Box className="ag-theme-material" sx={{ height: '400px', width: '100%' }}>
              <AgGridReact
                rowData={result.data}
                columnDefs={columnDefs}
                pagination={true}
                paginationPageSize={50}
                domLayout="normal"
              />
            </Box>
          )}

          {result.success && result.queryType !== 'SELECT' && (
            <Alert severity="success" sx={{ mt: 2 }}>
              {result.message}
            </Alert>
          )}

          {result.success && result.data && result.data.length === 0 && result.queryType === 'SELECT' && (
            <Alert severity="info">No data returned</Alert>
          )}
        </Paper>
      )}
    </Box>
  );
};

export default SqlEditor;
