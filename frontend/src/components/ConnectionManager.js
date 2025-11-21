import React, { useState, useEffect } from 'react';
import {
  Box,
  Button,
  Card,
  CardContent,
  Container,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControl,
  Grid,
  IconButton,
  InputLabel,
  MenuItem,
  Select,
  TextField,
  Typography,
  Alert,
  Chip,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import { connectionApi } from '../services/api';
import { DatabaseType, getDefaultPort } from '../types';

const ConnectionManager = () => {
  const [connections, setConnections] = useState([]);
  const [openDialog, setOpenDialog] = useState(false);
  const [editingConnection, setEditingConnection] = useState(null);
  const [testResult, setTestResult] = useState(null);
  const [formData, setFormData] = useState({
    connectionName: '',
    databaseType: 'MYSQL',
    host: 'localhost',
    port: 3306,
    databaseName: '',
    username: '',
    password: '',
  });

  useEffect(() => {
    loadConnections();
  }, []);

  const loadConnections = async () => {
    try {
      const response = await connectionApi.getAll();
      setConnections(response.data);
    } catch (error) {
      console.error('Failed to load connections:', error);
    }
  };

  const handleOpenDialog = (connection = null) => {
    if (connection) {
      setEditingConnection(connection);
      setFormData({
        connectionName: connection.connectionName,
        databaseType: connection.databaseType,
        host: connection.host,
        port: connection.port,
        databaseName: connection.databaseName,
        username: connection.username,
        password: '',
      });
    } else {
      setEditingConnection(null);
      setFormData({
        connectionName: '',
        databaseType: 'MYSQL',
        host: 'localhost',
        port: 3306,
        databaseName: '',
        username: '',
        password: '',
      });
    }
    setTestResult(null);
    setOpenDialog(true);
  };

  const handleCloseDialog = () => {
    setOpenDialog(false);
    setEditingConnection(null);
    setTestResult(null);
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: value,
    }));

    if (name === 'databaseType') {
      const defaultPort = getDefaultPort(value);
      if (defaultPort) {
        setFormData((prev) => ({ ...prev, port: defaultPort }));
      }
    }
  };

  const handleTestConnection = async () => {
    try {
      const response = await connectionApi.test(formData);
      setTestResult(response.data);
    } catch (error) {
      setTestResult({
        success: false,
        message: error.response?.data?.message || 'Connection test failed',
      });
    }
  };

  const handleSave = async () => {
    try {
      if (editingConnection) {
        await connectionApi.update(editingConnection.id, formData);
      } else {
        await connectionApi.create(formData);
      }
      loadConnections();
      handleCloseDialog();
    } catch (error) {
      console.error('Failed to save connection:', error);
    }
  };

  const handleDelete = async (id) => {
    if (window.confirm('Are you sure you want to delete this connection?')) {
      try {
        await connectionApi.delete(id);
        loadConnections();
      } catch (error) {
        console.error('Failed to delete connection:', error);
      }
    }
  };

  return (
    <Container maxWidth="lg" sx={{ mt: 4, mb: 4 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h4">Database Connections</Typography>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={() => handleOpenDialog()}
        >
          New Connection
        </Button>
      </Box>

      <Grid container spacing={3}>
        {connections.map((conn) => (
          <Grid item xs={12} md={6} lg={4} key={conn.id}>
            <Card>
              <CardContent>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                  <Typography variant="h6" gutterBottom>
                    {conn.connectionName}
                  </Typography>
                  <Box>
                    <IconButton size="small" onClick={() => handleOpenDialog(conn)}>
                      <EditIcon fontSize="small" />
                    </IconButton>
                    <IconButton size="small" onClick={() => handleDelete(conn.id)}>
                      <DeleteIcon fontSize="small" />
                    </IconButton>
                  </Box>
                </Box>
                <Chip label={conn.databaseType} size="small" color="primary" sx={{ mb: 1 }} />
                <Typography variant="body2" color="text.secondary">
                  Host: {conn.host}:{conn.port}
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  Database: {conn.databaseName}
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  User: {conn.username}
                </Typography>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>

      <Dialog open={openDialog} onClose={handleCloseDialog} maxWidth="sm" fullWidth>
        <DialogTitle>
          {editingConnection ? 'Edit Connection' : 'New Connection'}
        </DialogTitle>
        <DialogContent>
          <Box sx={{ pt: 2 }}>
            <TextField
              fullWidth
              label="Connection Name"
              name="connectionName"
              value={formData.connectionName}
              onChange={handleChange}
              margin="normal"
              required
            />
            <FormControl fullWidth margin="normal">
              <InputLabel>Database Type</InputLabel>
              <Select
                name="databaseType"
                value={formData.databaseType}
                label="Database Type"
                onChange={handleChange}
              >
                {Object.keys(DatabaseType).map((type) => (
                  <MenuItem key={type} value={type}>
                    {type}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            <TextField
              fullWidth
              label="Host"
              name="host"
              value={formData.host}
              onChange={handleChange}
              margin="normal"
              required
            />
            <TextField
              fullWidth
              label="Port"
              name="port"
              type="number"
              value={formData.port}
              onChange={handleChange}
              margin="normal"
              required
            />
            <TextField
              fullWidth
              label="Database Name"
              name="databaseName"
              value={formData.databaseName}
              onChange={handleChange}
              margin="normal"
              required
            />
            <TextField
              fullWidth
              label="Username"
              name="username"
              value={formData.username}
              onChange={handleChange}
              margin="normal"
              required
            />
            <TextField
              fullWidth
              label="Password"
              name="password"
              type="password"
              value={formData.password}
              onChange={handleChange}
              margin="normal"
              required={!editingConnection}
            />
            {testResult && (
              <Alert
                severity={testResult.success ? 'success' : 'error'}
                sx={{ mt: 2 }}
                icon={testResult.success ? <CheckCircleIcon /> : undefined}
              >
                {testResult.message}
                {testResult.databaseVersion && (
                  <Typography variant="caption" display="block">
                    {testResult.databaseVersion}
                  </Typography>
                )}
              </Alert>
            )}
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleTestConnection}>Test Connection</Button>
          <Button onClick={handleCloseDialog}>Cancel</Button>
          <Button onClick={handleSave} variant="contained">
            Save
          </Button>
        </DialogActions>
      </Dialog>
    </Container>
  );
};

export default ConnectionManager;
