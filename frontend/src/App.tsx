import React, { useState } from 'react';
import { Box, CssBaseline, ThemeProvider, createTheme } from '@mui/material';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import ConnectionList from './components/connections/ConnectionList';
import DatabaseWorkspace from './components/DatabaseWorkspace';
import { ConnectionResponse } from './types';

const theme = createTheme({
  palette: {
    primary: {
      main: '#1976d2'
    },
    secondary: {
      main: '#dc004e'
    }
  }
});

function App() {
  const [selectedConnection, setSelectedConnection] = useState<ConnectionResponse | null>(null);

  const handleSelectConnection = (connection: ConnectionResponse) => {
    setSelectedConnection(connection);
  };

  const handleBackToConnections = () => {
    setSelectedConnection(null);
  };

  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <Router>
        <Box sx={{ minHeight: '100vh', bgcolor: 'background.default' }}>
          {!selectedConnection ? (
            <ConnectionList onSelectConnection={handleSelectConnection} />
          ) : (
            <DatabaseWorkspace
              connection={selectedConnection}
              onBack={handleBackToConnections}
            />
          )}
        </Box>
      </Router>
    </ThemeProvider>
  );
}

export default App;
