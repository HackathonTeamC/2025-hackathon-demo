import React, { useState } from 'react';
import { ThemeProvider, createTheme } from '@mui/material/styles';
import { CssBaseline, Box } from '@mui/material';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import Sidebar from './components/Sidebar';
import ConnectionManager from './components/ConnectionManager';
import DatabaseExplorer from './components/DatabaseExplorer';
import DataGrid from './components/DataGrid';
import SQLEditor from './components/SQLEditor';

const theme = createTheme({
  palette: {
    mode: 'light',
    primary: {
      main: '#1976d2',
    },
    secondary: {
      main: '#dc004e',
    },
  },
});

function App() {
  const [selectedConnection, setSelectedConnection] = useState(null);
  const [selectedTable, setSelectedTable] = useState(null);

  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <Router>
        <Box sx={{ display: 'flex', height: '100vh' }}>
          <Sidebar 
            selectedConnection={selectedConnection}
            onConnectionSelect={setSelectedConnection}
          />
          <Box component="main" sx={{ flexGrow: 1, overflow: 'auto' }}>
            <Routes>
              <Route path="/" element={<Navigate to="/connections" replace />} />
              <Route path="/connections" element={<ConnectionManager />} />
              <Route 
                path="/explorer" 
                element={
                  <DatabaseExplorer 
                    connectionId={selectedConnection}
                    onTableSelect={setSelectedTable}
                  />
                } 
              />
              <Route 
                path="/data" 
                element={
                  <DataGrid 
                    connectionId={selectedConnection}
                    tableName={selectedTable?.tableName}
                    schema={selectedTable?.schema}
                  />
                } 
              />
              <Route 
                path="/sql" 
                element={
                  <SQLEditor connectionId={selectedConnection} />
                } 
              />
            </Routes>
          </Box>
        </Box>
      </Router>
    </ThemeProvider>
  );
}

export default App;
