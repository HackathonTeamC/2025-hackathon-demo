import React, { useEffect, useState } from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Button,
  IconButton,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Grid,
  Chip,
  Alert,
  CircularProgress
} from '@mui/material';
import { Add, Delete, Edit, Link as LinkIcon, CheckCircle, Error } from '@mui/icons-material';
import { ConnectionResponse } from '../../types';
import { connectionApi } from '../../services/api';
import ConnectionForm from './ConnectionForm';
import LanguageSwitcher from '../LanguageSwitcher';
import { useTranslation } from 'react-i18next';

interface ConnectionListProps {
  onSelectConnection: (connection: ConnectionResponse) => void;
}

const ConnectionList: React.FC<ConnectionListProps> = ({ onSelectConnection }) => {
  const { t } = useTranslation();
  const [connections, setConnections] = useState<ConnectionResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [openDialog, setOpenDialog] = useState(false);
  const [editingConnection, setEditingConnection] = useState<ConnectionResponse | null>(null);
  const [testResults, setTestResults] = useState<Map<string, { success: boolean; message: string }>>(new Map());

  useEffect(() => {
    loadConnections();
  }, []);

  const loadConnections = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await connectionApi.getAll();
      setConnections(response.data);
    } catch (err: any) {
      console.error('Failed to load connections', err);
      setError(err.response?.data?.message || 'Failed to load connections');
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (id: string) => {
    if (!window.confirm(t('connections.deleteConfirm'))) {
      return;
    }

    try {
      await connectionApi.delete(id);
      loadConnections();
    } catch (err: any) {
      console.error('Failed to delete connection', err);
      alert(err.response?.data?.message || 'Failed to delete connection');
    }
  };

  const handleTest = async (id: string) => {
    try {
      const response = await connectionApi.test(id);
      setTestResults(new Map(testResults.set(id, response.data)));
      setTimeout(() => {
        setTestResults(new Map(testResults));
        testResults.delete(id);
      }, 5000);
    } catch (err: any) {
      console.error('Connection test failed', err);
    }
  };

  const handleConnect = (connection: ConnectionResponse) => {
    onSelectConnection(connection);
  };

  const handleOpenDialog = (connection?: ConnectionResponse) => {
    setEditingConnection(connection || null);
    setOpenDialog(true);
  };

  const handleCloseDialog = () => {
    setOpenDialog(false);
    setEditingConnection(null);
  };

  const handleSaveSuccess = () => {
    handleCloseDialog();
    loadConnections();
  };

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" height="400px">
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box p={3}>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h4">{t('connections.title')}</Typography>
        <Box display="flex" gap={2} alignItems="center">
          <LanguageSwitcher />
          <Button
            variant="contained"
            startIcon={<Add />}
            onClick={() => handleOpenDialog()}
          >
            {t('connections.newConnection')}
          </Button>
        </Box>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}

      {connections.length === 0 ? (
        <Box textAlign="center" py={8}>
          <Typography variant="h6" color="textSecondary" gutterBottom>
            {t('connections.noConnections')}
          </Typography>
          <Typography variant="body2" color="textSecondary" paragraph>
            {t('connections.createFirst')}
          </Typography>
          <Button
            variant="contained"
            startIcon={<Add />}
            onClick={() => handleOpenDialog()}
          >
            {t('connections.createConnection')}
          </Button>
        </Box>
      ) : (
        <Grid container spacing={2}>
          {connections.map((connection) => {
            const testResult = testResults.get(connection.id);
            return (
              <Grid item xs={12} sm={6} md={4} key={connection.id}>
                <Card>
                  <CardContent>
                    <Box display="flex" justifyContent="space-between" alignItems="flex-start" mb={2}>
                      <Typography variant="h6" component="div">
                        {connection.connectionName}
                      </Typography>
                      <Box>
                        <IconButton size="small" onClick={() => handleOpenDialog(connection)}>
                          <Edit fontSize="small" />
                        </IconButton>
                        <IconButton size="small" color="error" onClick={() => handleDelete(connection.id)}>
                          <Delete fontSize="small" />
                        </IconButton>
                      </Box>
                    </Box>

                    <Chip label={connection.databaseType} size="small" color="primary" sx={{ mb: 1 }} />

                    <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
                      {connection.databaseType === 'SALESFORCE' 
                        ? connection.host
                        : `${connection.host}:${connection.port}${connection.databaseName ? '/' + connection.databaseName : ''}`
                      }
                    </Typography>

                    <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                      User: {connection.username}
                    </Typography>

                    {testResult && (
                      <Alert
                        severity={testResult.success ? 'success' : 'error'}
                        icon={testResult.success ? <CheckCircle /> : <Error />}
                        sx={{ mb: 2, py: 0 }}
                      >
                        {testResult.message}
                      </Alert>
                    )}

                    <Box display="flex" gap={1}>
                      <Button
                        variant="contained"
                        size="small"
                        fullWidth
                        startIcon={<LinkIcon />}
                        onClick={() => handleConnect(connection)}
                      >
                        {t('connections.connect')}
                      </Button>
                      <Button
                        variant="outlined"
                        size="small"
                        fullWidth
                        onClick={() => handleTest(connection.id)}
                      >
                        {t('connections.test')}
                      </Button>
                    </Box>
                  </CardContent>
                </Card>
              </Grid>
            );
          })}
        </Grid>
      )}

      <Dialog open={openDialog} onClose={handleCloseDialog} maxWidth="sm" fullWidth>
        <DialogTitle>
          {editingConnection ? t('connections.editConnection') : t('connections.newConnection')}
        </DialogTitle>
        <DialogContent>
          <ConnectionForm
            connection={editingConnection}
            onSuccess={handleSaveSuccess}
            onCancel={handleCloseDialog}
          />
        </DialogContent>
      </Dialog>
    </Box>
  );
};

export default ConnectionList;
