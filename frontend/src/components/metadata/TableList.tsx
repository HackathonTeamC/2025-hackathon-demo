import React, { useEffect, useState } from 'react';
import {
  Box,
  List,
  ListItemButton,
  ListItemText,
  Typography,
  Chip,
  CircularProgress,
  Alert,
  TextField,
  InputAdornment,
  FormControl,
  Select,
  MenuItem,
  InputLabel
} from '@mui/material';
import { Search, TableChart } from '@mui/icons-material';
import { TableInfo } from '../../types';
import { metadataApi } from '../../services/api';

interface TableListProps {
  connectionId: string;
  onSelectTable: (table: TableInfo) => void;
}

const TableList: React.FC<TableListProps> = ({ connectionId, onSelectTable }) => {
  const [tables, setTables] = useState<TableInfo[]>([]);
  const [filteredTables, setFilteredTables] = useState<TableInfo[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedTableName, setSelectedTableName] = useState<string>('');
  const [selectedSchema, setSelectedSchema] = useState<string>('all');
  const [schemas, setSchemas] = useState<string[]>([]);

  useEffect(() => {
    loadTables();
  }, [connectionId]);

  useEffect(() => {
    // Extract unique schemas
    const uniqueSchemas = Array.from(new Set(tables.map(t => t.schemaName).filter(s => s)));
    setSchemas(uniqueSchemas);
  }, [tables]);

  useEffect(() => {
    let filtered = tables;
    
    // Filter by schema
    if (selectedSchema !== 'all') {
      filtered = filtered.filter((table) => table.schemaName === selectedSchema);
    }
    
    // Filter by search query
    if (searchQuery) {
      filtered = filtered.filter((table) =>
        table.tableName.toLowerCase().includes(searchQuery.toLowerCase())
      );
    }
    
    setFilteredTables(filtered);
  }, [searchQuery, tables, selectedSchema]);

  const loadTables = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await metadataApi.getAllTables(connectionId);
      setTables(response.data);
      setFilteredTables(response.data);
    } catch (err: any) {
      console.error('Failed to load tables', err);
      setError(err.response?.data?.message || 'Failed to load tables');
    } finally {
      setLoading(false);
    }
  };

  const handleSelectTable = (table: TableInfo) => {
    setSelectedTableName(table.tableName);
    onSelectTable(table);
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
      <Box p={2}>
        {schemas.length > 0 && (
          <FormControl fullWidth size="small" sx={{ mb: 2 }}>
            <InputLabel>Schema</InputLabel>
            <Select
              value={selectedSchema}
              label="Schema"
              onChange={(e) => setSelectedSchema(e.target.value)}
            >
              <MenuItem value="all">All Schemas</MenuItem>
              {schemas.map((schema) => (
                <MenuItem key={schema} value={schema}>
                  {schema}
                </MenuItem>
              ))}
            </Select>
          </FormControl>
        )}
        <TextField
          fullWidth
          size="small"
          placeholder="Search tables..."
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          InputProps={{
            startAdornment: (
              <InputAdornment position="start">
                <Search />
              </InputAdornment>
            )
          }}
        />
      </Box>

      <Typography variant="body2" color="textSecondary" sx={{ px: 2, pb: 1 }}>
        {filteredTables.length} table(s) found
      </Typography>

      <List dense sx={{ maxHeight: '500px', overflow: 'auto' }}>
        {filteredTables.map((table) => (
          <ListItemButton
            key={`${table.schemaName || ''}.${table.tableName}`}
            selected={selectedTableName === table.tableName}
            onClick={() => handleSelectTable(table)}
          >
            <TableChart sx={{ mr: 2, color: 'primary.main' }} />
            <ListItemText
              primary={table.tableName}
              secondary={
                <Box display="flex" alignItems="center" gap={1} mt={0.5}>
                  {table.schemaName && (
                    <Chip label={table.schemaName} size="small" variant="outlined" />
                  )}
                  {table.rowCount !== null && table.rowCount !== undefined && (
                    <Typography variant="caption" color="textSecondary">
                      {table.rowCount.toLocaleString()} rows
                    </Typography>
                  )}
                </Box>
              }
            />
          </ListItemButton>
        ))}
      </List>

      {filteredTables.length === 0 && (
        <Box textAlign="center" py={4}>
          <Typography variant="body2" color="textSecondary">
            No tables found
          </Typography>
        </Box>
      )}
    </Box>
  );
};

export default TableList;
