import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { accountApi } from '../api/account'
import { UNAUTHORIZED_EVENT } from '../api/client'
import { applyLanguage, hasStoredLanguage } from '../i18n'
import { clearTokens, getAccessToken, login } from './oauth'
import { toAppPath } from '../config'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const navigate = useNavigate()
  const [status, setStatus] = useState(() => (getAccessToken() ? 'loading' : 'anonymous'))
  const [user, setUser] = useState(null)

  const loadUser = useCallback(async () => {
    if (!getAccessToken()) {
      setUser(null)
      setStatus('anonymous')
      return null
    }
    try {
      const me = await accountApi.me()
      setUser(me)
      setStatus('authenticated')
      if (!hasStoredLanguage() && (me.locale === 'km' || me.locale === 'en')) applyLanguage(me.locale, false)
      return me
    } catch {
      setUser(null)
      setStatus('anonymous')
      return null
    }
  }, [])

  useEffect(() => {
    loadUser()
  }, [loadUser])

  // A 401 means the access token expired (re-authorize; usually silent) or the session was revoked elsewhere.
  useEffect(() => {
    const onUnauthorized = (event) => {
      clearTokens()
      setUser(null)
      setStatus('anonymous')
      if (event.detail?.reason === 'revoked') {
        navigate('/?signedOut=revoked', { replace: true })
      } else {
        login({ returnTo: toAppPath(window.location.pathname) + window.location.search })
      }
    }
    window.addEventListener(UNAUTHORIZED_EVENT, onUnauthorized)
    return () => window.removeEventListener(UNAUTHORIZED_EVENT, onUnauthorized)
  }, [navigate])

  const logout = useCallback(async () => {
    try {
      await accountApi.revokeSession('current')
    } catch {
      // Already signed out server-side; clear locally anyway.
    }
    clearTokens()
    setUser(null)
    setStatus('anonymous')
    navigate('/?signedOut=1', { replace: true })
  }, [navigate])

  /** Signed out by the server as a consequence of the user's own action (e.g. "sign out everywhere"). */
  const endLocally = useCallback(
    (reason = '1') => {
      clearTokens()
      setUser(null)
      setStatus('anonymous')
      navigate(`/?signedOut=${reason}`, { replace: true })
    },
    [navigate],
  )

  const value = useMemo(
    () => ({ status, user, setUser, reloadUser: loadUser, login, logout, endLocally }),
    [status, user, loadUser, logout, endLocally],
  )
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export const useAuth = () => useContext(AuthContext)
