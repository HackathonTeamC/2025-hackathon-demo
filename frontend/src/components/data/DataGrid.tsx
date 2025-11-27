import React, { useState, useEffect } from 'react';
import { AgGridReact } from 'ag-grid-react';
import 'ag-grid-community/styles/ag-grid.css';
import 'ag-grid-community/styles/ag-theme-material.css';
import { Box, Paper, Typography, CircularProgress, Pagination, Alert, Button } from '@mui/material';
import { Refresh } from '@mui/icons-material';
import { dataApi } from '../../services/api';
import { TableInfo, DataQueryRequest } from '../../types';
import { ColDef } from 'ag-grid-community';

interface DataGridProps {
  connectionId: string;
  table: TableInfo;
}

const DataGrid: React.FC<DataGridProps> = ({ connectionId, table }) => {
  const [rowData, setRowData] = useState<any[]>([]);
  const [columnDefs, setColumnDefs] = useState<ColDef[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [pageSize] = useState(100);
  const [totalPages, setTotalPages] = useState(0);
  const [totalRecords, setTotalRecords] = useState(0);

  useEffect(() => {
    loadData();
  }, [connectionId, table, page]);

  const loadData = async () => {
    setLoading(true);
    setError(null);
    try {
      const request: DataQueryRequest = {
        page,
        size: pageSize
      };

      const response = await dataApi.getTableData(
        connectionId,
        table.tableName,
        request,
        table.schemaName
      );

      const data = response.data.data;
      setRowData(data);
      setTotalRecords(response.data.totalRecords);
      setTotalPages(response.data.totalPages);

      // Generate column definitions from data
      if (data.length > 0) {
        const columns: ColDef[] = Object.keys(data[0]).map((key) => ({
          field: key,
          headerName: key,
          sortable: true,
          filter: true,
          resizable: true,
          minWidth: 150
        }));
        setColumnDefs(columns);
      }
    } catch (err: any) {
      console.error('Failed to load table data', err);
      setError(err.response?.data?.message || 'Failed to load table data');
    } finally {
      setLoading(false);
    }
  };

  const handlePageChange = (_: React.ChangeEvent<unknown>, value: number) => {
    setPage(value - 1);
  };

  const handleRefresh = () => {
    loadData();
  };

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" height="400px">
        <CircularProgress />
      </Box>
    );
  }

  if (error) {
    return (
      <Alert severity="error" sx={{ mt: 2 }}>
        {error}
      </Alert>
    );
  }

  return (
    <Paper elevation={3} sx={{ p: 2 }}>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
        <Box>
          <Typography variant="h6">{table.tableName}</Typography>
          <Typography variant="body2" color="textSecondary">
            Total Records: {totalRecords.toLocaleString()} | Page: {page + 1} of {totalPages}
          </Typography>
        </Box>
        <Button startIcon={<Refresh />} onClick={handleRefresh} variant="outlined" size="small">
          Refresh
        </Button>
      </Box>

      <Box className="ag-theme-material" sx={{ height: '500px', width: '100%' }}>
        <AgGridReact
          rowData={rowData}
          columnDefs={columnDefs}
          pagination={false}
          domLayout="normal"
          rowSelection="single"
        />
      </Box>

      <Box display="flex" justifyContent="center" mt={2}>
        <Pagination
          count={totalPages}
          page={page + 1}
          onChange={handlePageChange}
          color="primary"
        />
      </Box>
    </Paper>
  );
};

export default DataGrid;
