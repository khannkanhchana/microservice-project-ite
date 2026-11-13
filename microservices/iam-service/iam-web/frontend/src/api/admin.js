import { request } from './client'

const query = (params = {}) => {
  const search = new URLSearchParams(
    Object.entries(params).filter(([, value]) => value !== undefined && value !== null && value !== ''),
  )
  return search.toString() ? `?${search}` : ''
}

/** Administration API (iam-service /api/admin, /api/users, /api/roles, /api/permissions). */
export const adminApi = {
  overview: () => request('/api/admin/overview'),

  // Users
  users: (params) => request(`/api/admin/users${query(params)}`),
  user: (uuid) => request(`/api/users/${uuid}`),
  createUser: (body) => request('/api/users', { method: 'POST', body }),
  updateUser: (uuid, body) => request(`/api/users/${uuid}`, { method: 'PATCH', body }),
  deleteUser: (uuid) => request(`/api/users/${uuid}`, { method: 'DELETE' }),
  setUserStatus: (uuid, action) => request(`/api/users/${uuid}/${action}`, { method: 'POST' }), // enable|disable|lock
  unlockUser: (uuid) => request(`/api/admin/users/${uuid}/unlock`, { method: 'POST' }),
  setPassword: (uuid, newPassword) => request(`/api/users/${uuid}/password`, { method: 'PUT', body: { newPassword } }),
  sendResetLink: (uuid) => request(`/api/admin/users/${uuid}/password-reset-link`, { method: 'POST' }),
  security: (uuid) => request(`/api/admin/users/${uuid}/security`),
  authorities: (uuid) => request(`/api/users/${uuid}/authorities`),
  assignRoles: (uuid, roleUuids, reason) => request(`/api/users/${uuid}/roles`, { method: 'POST', body: { roleUuids, reason } }),
  revokeRole: (uuid, roleUuid) => request(`/api/users/${uuid}/roles/${roleUuid}`, { method: 'DELETE' }),
  grantPermissions: (uuid, permissionUuids, effect, reason) =>
    request(`/api/users/${uuid}/permissions`, { method: 'POST', body: { permissionUuids, effect, reason } }),
  revokePermission: (uuid, permissionUuid) => request(`/api/users/${uuid}/permissions/${permissionUuid}`, { method: 'DELETE' }),
  identities: (uuid) => request(`/api/users/${uuid}/identities`),
  unlinkIdentity: (uuid, identityUuid) => request(`/api/users/${uuid}/identities/${identityUuid}`, { method: 'DELETE' }),

  // Sessions & login monitoring
  userSessions: (uuid) => request(`/api/admin/users/${uuid}/sessions`),
  revokeUserSession: (uuid, sessionId) => request(`/api/admin/users/${uuid}/sessions/${sessionId}`, { method: 'DELETE' }),
  revokeUserSessions: (uuid) => request(`/api/admin/users/${uuid}/sessions`, { method: 'DELETE' }),
  loginHistory: (params) => request(`/api/admin/login-history${query(params)}`),
  suspicious: (days = 7) => request(`/api/admin/login-history/suspicious${query({ days })}`),

  // Roles
  roles: (params) => request(`/api/roles${query({ size: 200, sort: 'name,asc', ...params })}`),
  role: (uuid) => request(`/api/roles/${uuid}`),
  createRole: (body) => request('/api/roles', { method: 'POST', body }),
  updateRole: (uuid, body) => request(`/api/roles/${uuid}`, { method: 'PATCH', body }),
  deleteRole: (uuid) => request(`/api/roles/${uuid}`, { method: 'DELETE' }),
  setRoleActive: (uuid, active) => request(`/api/roles/${uuid}/${active ? 'activate' : 'deactivate'}`, { method: 'POST' }),
  rolePermissions: (uuid) => request(`/api/admin/roles/${uuid}/permissions`),
  assignRolePermissions: (uuid, permissionUuids) =>
    request(`/api/roles/${uuid}/permissions`, { method: 'POST', body: { permissionUuids } }),
  revokeRolePermission: (uuid, permissionUuid) => request(`/api/roles/${uuid}/permissions/${permissionUuid}`, { method: 'DELETE' }),

  // Permissions
  permissions: (params) => request(`/api/permissions${query({ size: 200, sort: 'resource,asc', ...params })}`),
  createPermission: (body) => request('/api/permissions', { method: 'POST', body }),
  updatePermission: (uuid, body) => request(`/api/permissions/${uuid}`, { method: 'PATCH', body }),
  deletePermission: (uuid) => request(`/api/permissions/${uuid}`, { method: 'DELETE' }),
  setPermissionActive: (uuid, active) =>
    request(`/api/permissions/${uuid}/${active ? 'activate' : 'deactivate'}`, { method: 'POST' }),

  // OAuth2 clients
  clients: (search) => request(`/api/admin/clients${query({ search })}`),
  createClient: (body) => request('/api/admin/clients', { method: 'POST', body }),
  updateClient: (id, body) => request(`/api/admin/clients/${id}`, { method: 'PUT', body }),
  rotateSecret: (id) => request(`/api/admin/clients/${id}/secret`, { method: 'POST' }),
  deleteClient: (id) => request(`/api/admin/clients/${id}`, { method: 'DELETE' }),

  // Audit & settings
  auditLogs: (params) => request(`/api/admin/audit-logs${query(params)}`),
  settings: () => request('/api/admin/settings'),
  updateSettings: (body) => request('/api/admin/settings', { method: 'PUT', body }),
}
