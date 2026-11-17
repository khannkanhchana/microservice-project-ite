import { request } from './client'

const query = (params) => {
  const search = new URLSearchParams(
    Object.entries(params).filter(([, value]) => value !== undefined && value !== null && value !== ''),
  )
  return search.toString() ? `?${search}` : ''
}

export const accountApi = {
  // Profile
  me: () => request('/api/me'),
  updateMe: (changes) => request('/api/me', { method: 'PATCH', body: changes }),
  uploadPicture: (file) => {
    const form = new FormData()
    form.append('file', file)
    return request('/api/me/avatar', { method: 'POST', form })
  },
  removePicture: () => request('/api/me/avatar', { method: 'DELETE' }),

  // Password
  changePassword: (body) => request('/api/me/password', { method: 'PUT', body }),
  forgotPassword: (email) =>
    request('/api/auth/forgot-password', { method: 'POST', body: { email }, auth: false }),
  resetPassword: (body) => request('/api/auth/reset-password', { method: 'POST', body, auth: false }),
  register: (body) => request('/api/auth/register', { method: 'POST', body, auth: false }),

  // Linked accounts
  providers: () => request('/api/auth/providers', { auth: false }),
  identities: () => request('/api/me/identities'),
  startLink: (provider, returnUrl) =>
    request(`/api/me/identities/${encodeURIComponent(provider)}/link`, { method: 'POST', body: { returnUrl } }),
  unlink: (identityUuid) => request(`/api/me/identities/${identityUuid}`, { method: 'DELETE' }),

  // Sessions
  sessions: () => request('/api/me/sessions'),
  revokeSession: (sessionId) => request(`/api/me/sessions/${sessionId}`, { method: 'DELETE' }),
  revokeSessions: (scope) => request(`/api/me/sessions${query({ scope })}`, { method: 'DELETE' }),

  // History
  loginHistory: ({ status, page, size }) => request(`/api/me/login-history${query({ status, page, size })}`),
  activity: ({ page, size }) => request(`/api/me/activities${query({ page, size })}`),

  // Deactivation
  deactivate: (body) => request('/api/me/deactivation', { method: 'POST', body }), // immediate (legacy)
  deactivationRequest: () => request('/api/me/deactivation-request'), // null when there has been none
  requestDeactivation: (body) => request('/api/me/deactivation-request', { method: 'POST', body }),
  cancelDeactivation: () => request('/api/me/deactivation-request', { method: 'DELETE' }),
}
