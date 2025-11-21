# Universal Database Manager - Frontend

React frontend for the Universal Database Manager application.

## Requirements

- Node.js 16 or higher
- npm 8 or higher

## Quick Start (Windows)

### 1. Install dependencies (first time only)
```bash
install.bat
```
This will install all required npm packages.

### 2. Start the development server
```bash
start-frontend.bat
```
This will start the React development server on port 3000.

The application will automatically open in your default browser at: http://localhost:3000

## Features

### Connection Manager
- Create, edit, and delete database connections
- Test connections before saving
- Support for MySQL and PostgreSQL (MVP)

### Database Explorer
- Browse all tables in the connected database
- View table structure (columns, data types, constraints)
- See row counts for each table

### Data Grid
- View table data with pagination
- Sort and filter columns
- Customizable page size

### SQL Editor
- Write and execute SQL queries
- Syntax highlighting with Monaco Editor
- View query results in a table format
- Execution time display

## Manual Setup

If you prefer to use npm commands directly:

```bash
# Install dependencies
npm install

# Start development server
npm start

# Build for production
npm run build
```

## Configuration

The frontend is configured to proxy API requests to the backend at `http://localhost:8080`.

If you need to change the backend URL, edit the `proxy` field in `package.json`.

## Troubleshooting

### Dependencies Installation Failed
- Make sure you have Node.js 16 or higher installed
- Try deleting `node_modules` folder and `package-lock.json`, then run `install.bat` again

### Frontend Won't Start
- Make sure the backend is running on port 8080
- Check if another application is using port 3000
- Check the console for any error messages

### Cannot Connect to Backend
- Verify the backend is running at http://localhost:8080
- Check the proxy configuration in `package.json`
