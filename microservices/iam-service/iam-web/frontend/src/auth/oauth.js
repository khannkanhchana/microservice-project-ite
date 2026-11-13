import { config } from '../config'

// OAuth2 Authorization Code + PKCE for a public SPA client.
// Tokens live in sessionStorage (per tab). When they expire the app re-authorizes, which completes
// silently while the user's IAM browser session is still alive.

const PKCE_KEY = 'iam.pkce'
const TOKEN_KEY = 'iam.tokens'
const EXPIRY_SKEW_MS = 30_000

const base64Url = (bytes) =>
  btoa(String.fromCharCode(...new Uint8Array(bytes)))
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '')

const randomString = (byteLength = 32) => base64Url(crypto.getRandomValues(new Uint8Array(byteLength)))

async function challengeFor(verifier) {
  const digest = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(verifier))
  return base64Url(digest)
}

function readJson(storage, key) {
  try {
    const raw = storage.getItem(key)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

function writeJson(storage, key, value) {
  try {
    if (value == null) storage.removeItem(key)
    else storage.setItem(key, JSON.stringify(value))
  } catch {
    // Storage can be unavailable (private mode); auth then lasts for this page load only.
  }
}

let memoryTokens = null

export function getTokens() {
  return memoryTokens || readJson(sessionStorage, TOKEN_KEY)
}

export function getAccessToken() {
  const tokens = getTokens()
  if (!tokens?.accessToken) return null
  if (tokens.expiresAt && Date.now() > tokens.expiresAt - EXPIRY_SKEW_MS) return null
  return tokens.accessToken
}

export function clearTokens() {
  memoryTokens = null
  writeJson(sessionStorage, TOKEN_KEY, null)
}

/** Decodes a JWT payload without verifying it (display/claims only; the API verifies tokens). */
export function decodeJwt(token) {
  try {
    const payload = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')
    const json = decodeURIComponent(
      atob(payload)
        .split('')
        .map((c) => `%${c.charCodeAt(0).toString(16).padStart(2, '0')}`)
        .join(''),
    )
    return JSON.parse(json)
  } catch {
    return null
  }
}

/** Sends the browser to the IAM authorization endpoint. */
export async function login({ returnTo = '/account', prompt } = {}) {
  const verifier = randomString(48)
  const state = randomString(16)
  const nonce = randomString(16)
  writeJson(sessionStorage, PKCE_KEY, { verifier, state, nonce, returnTo })

  const params = new URLSearchParams({
    response_type: 'code',
    client_id: config.clientId,
    redirect_uri: config.redirectUri,
    scope: config.scopes,
    state,
    nonce,
    code_challenge: await challengeFor(verifier),
    code_challenge_method: 'S256',
  })
  if (prompt) params.set('prompt', prompt)
  window.location.assign(`${config.iamUrl}/oauth2/authorize?${params}`)
}

/** Completes the redirect back from IAM; returns the in-app path to continue to. */
export async function handleCallback(search) {
  const params = new URLSearchParams(search)
  const pending = readJson(sessionStorage, PKCE_KEY)
  writeJson(sessionStorage, PKCE_KEY, null)

  if (params.get('error')) {
    throw new Error(params.get('error_description') || params.get('error'))
  }
  if (!pending || !params.get('code') || params.get('state') !== pending.state) {
    throw new Error('invalid_state')
  }

  const response = await fetch(`${config.iamUrl}/oauth2/token`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({
      grant_type: 'authorization_code',
      code: params.get('code'),
      redirect_uri: config.redirectUri,
      client_id: config.clientId,
      code_verifier: pending.verifier,
    }),
  })
  const body = await response.json().catch(() => ({}))
  if (!response.ok) throw new Error(body.error_description || body.error || 'token_exchange_failed')

  const idClaims = body.id_token ? decodeJwt(body.id_token) : null
  if (idClaims && idClaims.nonce !== pending.nonce) throw new Error('invalid_nonce')

  const tokens = {
    accessToken: body.access_token,
    idToken: body.id_token,
    expiresAt: Date.now() + (body.expires_in || 3600) * 1000,
  }
  memoryTokens = tokens
  writeJson(sessionStorage, TOKEN_KEY, tokens)
  return pending.returnTo || '/account'
}

/** IAM pages that are reached by full-page navigation (sign-in UI lives on the authorization server). */
export const iamPage = (path) => `${config.iamUrl}${path}`
