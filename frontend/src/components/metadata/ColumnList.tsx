import React, { useEffect, useState } from 'react';
import {
  Box,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Chip,
  CircularProgress,
  Alert,
  Typography
} from '@mui/material';
import { Key, Check, Close } from '@mui/icons-material';
import { ColumnInfo, TableInfo } from '../../types';
import { metadataApi } from '../../services/api';

interface ColumnListProps {
  connectionId: string;
  table: TableInfo;
}

const ColumnList: React.FC<ColumnListProps> = ({ connectionId, table }) => {
  const [columns, setColumns] = useState<ColumnInfo[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    loadColumns();
  }, [connectionId, table]);

  const loadColumns = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await metadataApi.getColumns(connectionId, table.tableName, table.schemaName);
      setColumns(response.data);
    } catch (err: any) {
      console.error('Failed to load columns', err);
      setError(err.response?.data?.message || 'Failed to load columns');
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" height="200px">
        <CircularProgress />
      </Box>
    );
  }

  if (error) {
    return (
      <Alert severity="error" sx={{ m: 2 }}>
        {error}
      </Alert>
    );
  }

  return (
    <Box>
      <Box p={2} bgcolor="background.paper" borderBottom={1} borderColor="divider">
        <Typography variant="h6">{table.tableName}</Typography>
        <Typography variant="body2" color="textSecondary">
          {columns.length} column(s)
        </Typography>
      </Box>

      <TableContainer component={Paper} elevation={0}>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Column Name</TableCell>
              <TableCell>Data Type</TableCell>
              <TableCell align="center">Nullable</TableCell>
              <TableCell align="center">Primary Key</TableCell>
              <TableCell align="center">Auto Increment</TableCell>
              <TableCell>Default Value</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {columns.map((column) => (
              <TableRow key={column.columnName} hover>
                <TableCell>
                  <Box display="flex" alignItems="center" gap={1}>
                    {column.primaryKey && <Key fontSize="small" color="primary" />}
                    <Typography variant="body2" fontWeight={column.primaryKey ? 'bold' : 'normal'}>
                      {column.columnName}
                    </Typography>
                  </Box>
                </TableCell>
                <TableCell>
                  <Chip
                    label={`${column.dataType}${
                      column.columnSize ? `(${column.columnSize}${column.decimalDigits ? `,${column.decimalDigits}` : ''})` : ''
                    }`}
                    size="small"
                    variant="outlined"
                  />
                </TableCell>
                <TableCell align="center">
                  {column.nullable ? (
                    <Check fontSize="small" color="success" />
                  ) : (
                    <Close fontSize="small" color="error" />
                  )}
                </TableCell>
                <TableCell align="center">
                  {column.primaryKey ? (
                    <Check fontSize="small" color="primary" />
                  ) : (
                    <Close fontSize="small" color="disabled" />
                  )}
                </TableCell>
                <TableCell align="center">
                  {column.autoIncrement ? (
                    <Check fontSize="small" color="success" />
                  ) : (
                    <Close fontSize="small" color="disabled" />
                  )}
                </TableCell>
                <TableCell>
                  <Typography variant="body2" color="textSecondary">
                    {column.defaultValue || '-'}
                  </Typography>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>
    </Box>
  );
};

export default ColumnList;
