import axios from 'axios';

const API_BASE_URL = '/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

export const connectionApi = {
  getAll: () => api.get('/connections'),
  getById: (id) => api.get(`/connections/${id}`),
  create: (data) => api.post('/connections', data),
  update: (id, data) => api.put(`/connections/${id}`, data),
  delete: (id) => api.delete(`/connections/${id}`),
  test: (data) => api.post('/connections/test', data),
};

export const metadataApi = {
  getSchemas: (connectionId) => api.get(`/connections/${connectionId}/metadata/schemas`),
  getTables: (connectionId, schema) => api.get(`/connections/${connectionId}/metadata/tables`, {
    params: { schema },
  }),
  getTableDetails: (connectionId, tableName, schema) => api.get(
    `/connections/${connectionId}/metadata/tables/${tableName}`,
    { params: { schema } }
  ),
};

export const queryApi = {
  execute: (connectionId, data) => api.post(`/connections/${connectionId}/query`, data),
  getTableData: (connectionId, tableName, schema, page, pageSize) => api.get(
    `/connections/${connectionId}/data/${tableName}`,
    { params: { schema, page, pageSize } }
  ),
};

export default api;
