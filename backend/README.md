# Universal Database Manager - Backend

Spring Boot backend for the Universal Database Manager application.

## Requirements

- Java 17 or higher
- Maven 3.6 or higher

## Quick Start (Windows)

### 1. Start the backend server
```bash
start-backend.bat
```
This will start the Spring Boot application on port 8080.

### 2. Build the project (optional)
```bash
build.bat
```
This will create a JAR file in the `target/` directory.

## API Endpoints

### Connection Management
- `POST /api/connections` - Create a new database connection
- `GET /api/connections` - Get all connections
- `GET /api/connections/{id}` - Get a specific connection
- `PUT /api/connections/{id}` - Update a connection
- `DELETE /api/connections/{id}` - Delete a connection
- `POST /api/connections/test` - Test a connection

### Metadata
- `GET /api/connections/{id}/metadata/schemas` - Get all schemas
- `GET /api/connections/{id}/metadata/tables` - Get all tables
- `GET /api/connections/{id}/metadata/tables/{tableName}` - Get table details

### Query Execution
- `POST /api/connections/{id}/query` - Execute a SQL query
- `GET /api/connections/{id}/data/{tableName}` - Get table data with pagination

## Configuration

Edit `src/main/resources/application.yml` to configure:
- Server port (default: 8080)
- Database settings
- Query timeout and limits

## H2 Console

Access the H2 console at: http://localhost:8080/h2-console

- JDBC URL: `jdbc:h2:file:./data/udbmanager`
- Username: `sa`
- Password: (empty)

## Supported Databases

- MySQL
- PostgreSQL
- SQLite
- Oracle Database
- Microsoft SQL Server
- MongoDB (partial support)
- Snowflake (requires additional configuration)
- Salesforce (requires additional configuration)
