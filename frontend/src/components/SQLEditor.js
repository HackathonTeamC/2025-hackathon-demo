import React, { useState } from 'react';
import {
  Box,
  Container,
  Typography,
  Paper,
  Button,
  Alert,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
} from '@mui/material';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import Editor from '@monaco-editor/react';
import { queryApi } from '../services/api';

const SQLEditor = ({ connectionId }) => {
  const [query, setQuery] = useState('SELECT * FROM your_table LIMIT 10;');
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);

  const handleExecute = async () => {
    if (!connectionId) {
      setError('Please select a connection first.');
      return;
    }

    setLoading(true);
    setError(null);
    setResult(null);

    try {
      const response = await queryApi.execute(connectionId, {
        query,
        page: 0,
        pageSize: 1000,
      });

      setResult(response.data);
    } catch (error) {
      console.error('Query execution failed:', error);
      setError(
        error.response?.data?.message || 'Query execution failed. Please check your SQL syntax.'
      );
    } finally {
      setLoading(false);
    }
  };

  if (!connectionId) {
    return (
      <Container maxWidth="lg" sx={{ mt: 4 }}>
        <Alert severity="info">Please select a connection first.</Alert>
      </Container>
    );
  }

  return (
    <Container maxWidth="xl" sx={{ mt: 4, mb: 4 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
        <Typography variant="h4">SQL Editor</Typography>
        <Button
          variant="contained"
          startIcon={<PlayArrowIcon />}
          onClick={handleExecute}
          disabled={loading}
        >
          Execute
        </Button>
      </Box>

      <Paper sx={{ mb: 2, height: 300 }}>
        <Editor
          height="300px"
          defaultLanguage="sql"
          value={query}
          onChange={(value) => setQuery(value || '')}
          theme="vs-light"
          options={{
            minimap: { enabled: false },
            fontSize: 14,
            lineNumbers: 'on',
            scrollBeyondLastLine: false,
            automaticLayout: true,
          }}
        />
      </Paper>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}

      {result && (
        <Paper sx={{ p: 2 }}>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
            <Typography variant="h6">Results</Typography>
            <Typography variant="body2" color="text.secondary">
              {result.totalRows} rows returned in {result.executionTime}ms
            </Typography>
          </Box>

          {result.columns && result.columns.length > 0 && (
            <TableContainer sx={{ maxHeight: 500 }}>
              <Table stickyHeader size="small">
                <TableHead>
                  <TableRow>
                    {result.columns.map((col) => (
                      <TableCell key={col}>{col}</TableCell>
                    ))}
                  </TableRow>
                </TableHead>
                <TableBody>
                  {result.rows?.map((row, index) => (
                    <TableRow key={index}>
                      {result.columns.map((col) => (
                        <TableCell key={col}>
                          {row[col] !== null && row[col] !== undefined
                            ? String(row[col])
                            : 'NULL'}
                        </TableCell>
                      ))}
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          )}

          {(!result.columns || result.columns.length === 0) && (
            <Alert severity="success">Query executed successfully.</Alert>
          )}
        </Paper>
      )}
    </Container>
  );
};

export default SQLEditor;
