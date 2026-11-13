import { config } from '../config'
import { getAccessToken } from '../auth/oauth'

export class ApiError extends Error {
  constructor(status, problem = {}) {
    super(problem.detail || problem.title || `Request failed (${status})`)
    this.status = status
    this.title = problem.title
    this.fieldErrors = Object.fromEntries((problem.errors || []).map((e) => [e.field, e.message]))
  }
}

export const UNAUTHORIZED_EVENT = 'iam:unauthorized'

/**
 * Calls the IAM REST API. JSON in/out, RFC 7807 problem details become ApiError.
 * A 401 on an authenticated call emits UNAUTHORIZED_EVENT so the app can re-authenticate.
 */
export async function request(path, { method = 'GET', body, form, auth = true, signal } = {}) {
  const headers = { Accept: 'application/json' }
  if (auth) {
    const token = getAccessToken()
    if (!token) {
      window.dispatchEvent(new CustomEvent(UNAUTHORIZED_EVENT, { detail: { reason: 'expired' } }))
      throw new ApiError(401, { detail: 'Your session expired.' })
    }
    headers.Authorization = `Bearer ${token}`
  }
  let payload
  if (form) payload = form
  else if (body !== undefined) {
    headers['Content-Type'] = 'application/json'
    payload = JSON.stringify(body)
  }

  let response
  try {
    response = await fetch(`${config.iamUrl}${path}`, { method, headers, body: payload, signal })
  } catch (error) {
    if (error.name === 'AbortError') throw error
    throw new ApiError(0, { title: 'network', detail: 'network' })
  }

  if (response.status === 401 && auth) {
    const challenge = response.headers.get('WWW-Authenticate') || ''
    const reason = /signed out/i.test(challenge) ? 'revoked' : 'expired'
    window.dispatchEvent(new CustomEvent(UNAUTHORIZED_EVENT, { detail: { reason } }))
  }
  if (response.status === 204 || response.status === 202) return null

  const text = await response.text()
  const data = text ? safeJson(text) : null
  if (!response.ok) throw new ApiError(response.status, data || {})
  return data
}

function safeJson(text) {
  try {
    return JSON.parse(text)
  } catch {
    return { detail: text }
  }
}
