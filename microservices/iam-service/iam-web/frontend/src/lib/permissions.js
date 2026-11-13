/** Permissions that open a section of the administration console. */
export const ADMIN_SECTIONS = [
  { path: '/admin', permission: 'dashboard:read' },
  { path: '/admin/users', permission: 'user:read' },
  { path: '/admin/roles', permission: 'role:read' },
  { path: '/admin/permissions', permission: 'permission:read' },
  { path: '/admin/clients', permission: 'client:read' },
  { path: '/admin/login-monitor', permission: 'session:read' },
  { path: '/admin/audit-logs', permission: 'audit:read' },
  { path: '/admin/settings', permission: 'settings:read' },
]

export const hasPermission = (user, permission) => Boolean(user?.permissions?.includes(permission))

export const canAdminister = (user) => ADMIN_SECTIONS.some((section) => hasPermission(user, section.permission))
