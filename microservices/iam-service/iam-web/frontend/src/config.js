const trimSlash = (value) => value.replace(/\/+$/, '')

// Path the SPA is served under (`base` in vite.config.js). iam-service serves it at the root.
export const basePath = trimSlash(import.meta.env.BASE_URL || '/')

/** Turns a full browser path into an in-app route (strips `base` when it is not '/'). */
export const toAppPath = (path) =>
  basePath && path.startsWith(basePath) ? path.slice(basePath.length) || '/' : path

export const config = {
  // In production the SPA is served by iam-service itself, so the API is the same origin.
  // In `npm run dev` (Vite on :5173) it calls iam-service on :9000.
  iamUrl: trimSlash(
    import.meta.env.VITE_IAM_URL || (import.meta.env.DEV ? 'http://localhost:9000' : window.location.origin),
  ),
  clientId: import.meta.env.VITE_IAM_CLIENT_ID || 'iam-web',
  scopes: 'openid profile email phone address',
  get redirectUri() {
    return `${window.location.origin}${basePath}/callback`
  },
}
