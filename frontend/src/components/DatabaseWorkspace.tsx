import React, { useState } from 'react';
import {
  Box,
  AppBar,
  Toolbar,
  Typography,
  IconButton,
  Drawer,
  Tabs,
  Tab,
  Divider,
  Chip
} from '@mui/material';
import { ArrowBack, Menu as MenuIcon } from '@mui/icons-material';
import { ConnectionResponse, TableInfo } from '../types';
import TableList from './metadata/TableList';
import ColumnList from './metadata/ColumnList';
import DataGrid from './data/DataGrid';
import SqlEditor from './sql/SqlEditor';
import LanguageSwitcher from './LanguageSwitcher';

interface DatabaseWorkspaceProps {
  connection: ConnectionResponse;
  onBack: () => void;
}

const DatabaseWorkspace: React.FC<DatabaseWorkspaceProps> = ({ connection, onBack }) => {
  const [drawerOpen, setDrawerOpen] = useState(true);
  const [selectedTable, setSelectedTable] = useState<TableInfo | null>(null);
  const [activeTab, setActiveTab] = useState(0);

  const handleSelectTable = (table: TableInfo) => {
    setSelectedTable(table);
    setActiveTab(1); // Switch to Data tab when table is selected
  };

  const handleTabChange = (_: React.SyntheticEvent, newValue: number) => {
    setActiveTab(newValue);
  };

  const toggleDrawer = () => {
    setDrawerOpen(!drawerOpen);
  };

  return (
    <Box sx={{ display: 'flex', height: '100vh' }}>
      {/* App Bar */}
      <AppBar position="fixed" sx={{ zIndex: (theme) => theme.zIndex.drawer + 1 }}>
        <Toolbar variant="dense" sx={{ minHeight: 48, py: 0 }}>
          <IconButton
            edge="start"
            color="inherit"
            onClick={onBack}
            sx={{ mr: 1 }}
            size="small"
          >
            <ArrowBack fontSize="small" />
          </IconButton>
          <Typography 
            variant="body1" 
            component="div" 
            noWrap 
            sx={{ 
              fontWeight: 600,
              mr: 2
            }}
          >
            {connection.connectionName}
          </Typography>
          <Chip
            label={connection.databaseType}
            size="small"
            color="secondary"
            sx={{ mr: 2, height: 22 }}
          />
          <Typography 
            variant="body2" 
            color="inherit" 
            noWrap 
            sx={{ 
              flexGrow: 1,
              opacity: 0.9
            }}
          >
            {connection.host}
            {connection.port ? `:${connection.port}` : ''}
            {connection.databaseName ? ` / ${connection.databaseName}` : ''}
          </Typography>
          <LanguageSwitcher />
        </Toolbar>
      </AppBar>

      {/* Left Sidebar - Tables List */}
      <Drawer
        variant="persistent"
        anchor="left"
        open={drawerOpen}
        sx={{
          width: 280,
          flexShrink: 0,
          '& .MuiDrawer-paper': {
            width: 280,
            boxSizing: 'border-box',
            mt: '48px',
            borderRight: '1px solid',
            borderColor: 'divider'
          }
        }}
      >
        <Box>
          <Box px={2} py={1.5} display="flex" justifyContent="space-between" alignItems="center">
            <Typography variant="h6" fontWeight="600">Tables</Typography>
            <IconButton size="small" onClick={toggleDrawer}>
              <MenuIcon />
            </IconButton>
          </Box>
          <Divider />
          <TableList connectionId={connection.id} onSelectTable={handleSelectTable} />
        </Box>
      </Drawer>

      {/* Main Content Area */}
      <Box
        component="main"
        sx={{
          flexGrow: 1,
          p: 1,
          mt: '48px',
          width: drawerOpen ? `calc(100% - 280px)` : '100%',
          ml: drawerOpen ? 0 : 0,
          transition: (theme) =>
            theme.transitions.create(['margin', 'width'], {
              easing: theme.transitions.easing.sharp,
              duration: theme.transitions.duration.leavingScreen
            })
        }}
      >
        {!drawerOpen && (
          <IconButton onClick={toggleDrawer} sx={{ mb: 1 }} size="small">
            <MenuIcon />
          </IconButton>
        )}

        <Box sx={{ borderBottom: 1, borderColor: 'divider', mb: 1 }}>
          <Tabs value={activeTab} onChange={handleTabChange}>
            <Tab label="Table Structure" />
            <Tab label="Data" disabled={!selectedTable} />
            <Tab label="SQL Editor" />
          </Tabs>
        </Box>

        {/* Tab Content */}
        {activeTab === 0 && selectedTable && (
          <ColumnList connectionId={connection.id} table={selectedTable} />
        )}

        {activeTab === 0 && !selectedTable && (
          <Box textAlign="center" py={8}>
            <Typography variant="h6" color="textSecondary">
              Select a table from the left sidebar
            </Typography>
          </Box>
        )}

        {activeTab === 1 && selectedTable && (
          <DataGrid connectionId={connection.id} table={selectedTable} />
        )}

        {activeTab === 2 && (
          <SqlEditor connectionId={connection.id} />
        )}
      </Box>
    </Box>
  );
};

export default DatabaseWorkspace;
