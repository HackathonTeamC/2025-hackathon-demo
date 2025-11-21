import React, { useState, useEffect } from 'react';
import {
  Box,
  Container,
  Typography,
  List,
  ListItem,
  ListItemButton,
  ListItemText,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Chip,
  Alert,
} from '@mui/material';
import TableChartIcon from '@mui/icons-material/TableChart';
import { metadataApi } from '../services/api';

const DatabaseExplorer = ({ connectionId, onTableSelect }) => {
  const [tables, setTables] = useState([]);
  const [selectedTable, setSelectedTable] = useState(null);
  const [tableDetails, setTableDetails] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (connectionId) {
      loadTables();
    }
  }, [connectionId]);

  const loadTables = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await metadataApi.getTables(connectionId, null);
      setTables(response.data);
    } catch (error) {
      console.error('Failed to load tables:', error);
      setError('Failed to load tables. Please check your connection.');
    } finally {
      setLoading(false);
    }
  };

  const handleTableClick = async (table) => {
    setSelectedTable(table);
    if (onTableSelect) {
      onTableSelect(table);
    }

    try {
      const response = await metadataApi.getTableDetails(
        connectionId,
        table.tableName,
        table.schema
      );
      setTableDetails(response.data);
    } catch (error) {
      console.error('Failed to load table details:', error);
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
      <Typography variant="h4" gutterBottom>
        Database Explorer
      </Typography>

      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      <Box sx={{ display: 'flex', gap: 2, height: 'calc(100vh - 200px)' }}>
        <Paper sx={{ width: 300, overflow: 'auto' }}>
          <List>
            {tables.map((table) => (
              <ListItem key={table.tableName} disablePadding>
                <ListItemButton
                  selected={selectedTable?.tableName === table.tableName}
                  onClick={() => handleTableClick(table)}
                >
                  <TableChartIcon sx={{ mr: 1 }} fontSize="small" />
                  <ListItemText
                    primary={table.tableName}
                    secondary={`${table.rowCount || 0} rows`}
                  />
                </ListItemButton>
              </ListItem>
            ))}
          </List>
        </Paper>

        <Paper sx={{ flex: 1, p: 2, overflow: 'auto' }}>
          {tableDetails ? (
            <>
              <Typography variant="h5" gutterBottom>
                {tableDetails.tableName}
              </Typography>
              <Box sx={{ mb: 2 }}>
                <Chip label={tableDetails.tableType} size="small" sx={{ mr: 1 }} />
                <Chip
                  label={`${tableDetails.rowCount || 0} rows`}
                  size="small"
                  color="primary"
                />
              </Box>

              <Typography variant="h6" gutterBottom sx={{ mt: 3 }}>
                Columns
              </Typography>
              <TableContainer>
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell>Column Name</TableCell>
                      <TableCell>Data Type</TableCell>
                      <TableCell>Size</TableCell>
                      <TableCell>Nullable</TableCell>
                      <TableCell>Key</TableCell>
                      <TableCell>Auto Increment</TableCell>
                      <TableCell>Default</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {tableDetails.columns?.map((column) => (
                      <TableRow key={column.columnName}>
                        <TableCell>{column.columnName}</TableCell>
                        <TableCell>{column.dataType}</TableCell>
                        <TableCell>{column.columnSize}</TableCell>
                        <TableCell>{column.nullable ? 'Yes' : 'No'}</TableCell>
                        <TableCell>
                          {column.primaryKey && (
                            <Chip label="PK" size="small" color="secondary" />
                          )}
                        </TableCell>
                        <TableCell>{column.autoIncrement ? 'Yes' : 'No'}</TableCell>
                        <TableCell>{column.defaultValue || '-'}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            </>
          ) : (
            <Typography color="text.secondary">
              Select a table to view its details
            </Typography>
          )}
        </Paper>
      </Box>
    </Container>
  );
};

export default DatabaseExplorer;
