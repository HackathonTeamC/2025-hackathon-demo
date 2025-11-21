import React, { useState, useEffect, useMemo } from 'react';
import {
  Box,
  Container,
  Typography,
  Paper,
  Alert,
  Button,
  TextField,
} from '@mui/material';
import { AgGridReact } from 'ag-grid-react';
import 'ag-grid-community/styles/ag-grid.css';
import 'ag-grid-community/styles/ag-theme-material.css';
import NavigateBeforeIcon from '@mui/icons-material/NavigateBefore';
import NavigateNextIcon from '@mui/icons-material/NavigateNext';
import { queryApi } from '../services/api';

const DataGrid = ({ connectionId, tableName, schema }) => {
  const [rowData, setRowData] = useState([]);
  const [columnDefs, setColumnDefs] = useState([]);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(100);
  const [totalRows, setTotalRows] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (connectionId && tableName) {
      loadData();
    }
  }, [connectionId, tableName, schema, page, pageSize]);

  const loadData = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await queryApi.getTableData(
        connectionId,
        tableName,
        schema,
        page,
        pageSize
      );

      const data = response.data;
      setTotalRows(data.totalRows);

      if (data.columns && data.columns.length > 0) {
        const cols = data.columns.map((col) => ({
          field: col,
          headerName: col,
          sortable: true,
          filter: true,
          resizable: true,
        }));
        setColumnDefs(cols);
      }

      setRowData(data.rows || []);
    } catch (error) {
      console.error('Failed to load data:', error);
      setError('Failed to load table data. Please check your connection.');
    } finally {
      setLoading(false);
    }
  };

  const defaultColDef = useMemo(
    () => ({
      sortable: true,
      filter: true,
      resizable: true,
      minWidth: 100,
    }),
    []
  );

  const handlePreviousPage = () => {
    if (page > 0) {
      setPage(page - 1);
    }
  };

  const handleNextPage = () => {
    if ((page + 1) * pageSize < totalRows) {
      setPage(page + 1);
    }
  };

  if (!connectionId || !tableName) {
    return (
      <Container maxWidth="lg" sx={{ mt: 4 }}>
        <Alert severity="info">
          Please select a connection and a table from the Database Explorer.
        </Alert>
      </Container>
    );
  }

  return (
    <Container maxWidth="xl" sx={{ mt: 4, mb: 4 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
        <Typography variant="h4">{tableName}</Typography>
        <Box sx={{ display: 'flex', gap: 2, alignItems: 'center' }}>
          <TextField
            label="Page Size"
            type="number"
            size="small"
            value={pageSize}
            onChange={(e) => {
              setPageSize(Number(e.target.value));
              setPage(0);
            }}
            sx={{ width: 120 }}
          />
          <Button
            startIcon={<NavigateBeforeIcon />}
            onClick={handlePreviousPage}
            disabled={page === 0}
          >
            Previous
          </Button>
          <Typography>
            Page {page + 1} ({totalRows} total rows)
          </Typography>
          <Button
            endIcon={<NavigateNextIcon />}
            onClick={handleNextPage}
            disabled={(page + 1) * pageSize >= totalRows}
          >
            Next
          </Button>
        </Box>
      </Box>

      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      <Paper sx={{ height: 'calc(100vh - 250px)', width: '100%' }}>
        <div className="ag-theme-material" style={{ height: '100%', width: '100%' }}>
          <AgGridReact
            rowData={rowData}
            columnDefs={columnDefs}
            defaultColDef={defaultColDef}
            pagination={false}
            loading={loading}
          />
        </div>
      </Paper>
    </Container>
  );
};

export default DataGrid;
