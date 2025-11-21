export const DatabaseType = {
  MYSQL: 'MYSQL',
  POSTGRESQL: 'POSTGRESQL',
  SQLITE: 'SQLITE',
  ORACLE: 'ORACLE',
  SQLSERVER: 'SQLSERVER',
  MONGODB: 'MONGODB',
  SNOWFLAKE: 'SNOWFLAKE',
  SALESFORCE: 'SALESFORCE',
};

export const getDefaultPort = (type) => {
  const ports = {
    MYSQL: 3306,
    POSTGRESQL: 5432,
    ORACLE: 1521,
    SQLSERVER: 1433,
    MONGODB: 27017,
  };
  return ports[type] || '';
};
