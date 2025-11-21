import React from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import {
  Drawer,
  List,
  ListItem,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Toolbar,
  Typography,
  Divider,
} from '@mui/material';
import StorageIcon from '@mui/icons-material/Storage';
import ExploreIcon from '@mui/icons-material/Explore';
import TableChartIcon from '@mui/icons-material/TableChart';
import CodeIcon from '@mui/icons-material/Code';

const drawerWidth = 240;

const Sidebar = ({ selectedConnection }) => {
  const navigate = useNavigate();
  const location = useLocation();

  const menuItems = [
    { text: 'Connections', icon: <StorageIcon />, path: '/connections' },
    { text: 'Explorer', icon: <ExploreIcon />, path: '/explorer', requiresConnection: true },
    { text: 'Data Grid', icon: <TableChartIcon />, path: '/data', requiresConnection: true },
    { text: 'SQL Editor', icon: <CodeIcon />, path: '/sql', requiresConnection: true },
  ];

  return (
    <Drawer
      variant="permanent"
      sx={{
        width: drawerWidth,
        flexShrink: 0,
        '& .MuiDrawer-paper': {
          width: drawerWidth,
          boxSizing: 'border-box',
        },
      }}
    >
      <Toolbar>
        <Typography variant="h6" noWrap component="div">
          UDB Manager
        </Typography>
      </Toolbar>
      <Divider />
      <List>
        {menuItems.map((item) => (
          <ListItem key={item.text} disablePadding>
            <ListItemButton
              selected={location.pathname === item.path}
              onClick={() => navigate(item.path)}
              disabled={item.requiresConnection && !selectedConnection}
            >
              <ListItemIcon>{item.icon}</ListItemIcon>
              <ListItemText primary={item.text} />
            </ListItemButton>
          </ListItem>
        ))}
      </List>
    </Drawer>
  );
};

export default Sidebar;
