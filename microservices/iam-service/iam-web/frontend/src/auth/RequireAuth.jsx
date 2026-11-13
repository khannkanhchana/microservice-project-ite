import { useEffect } from 'react'
import { useLocation } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { Spinner } from '../components/ui'
import { useAuth } from './AuthContext'

/** Renders children for signed-in users; otherwise starts the OAuth2 sign-in and comes back here. */
export default function RequireAuth({ children }) {
  const { t } = useTranslation()
  const { status, login } = useAuth()
  const location = useLocation()

  useEffect(() => {
    if (status === 'anonymous') login({ returnTo: location.pathname + location.search })
  }, [status, login, location.pathname, location.search])

  if (status === 'authenticated') return children
  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-3 text-muted">
      <Spinner className="size-8 text-primary" />
      <p>{status === 'loading' ? t('common.loading') : t('auth.redirecting')}</p>
    </div>
  )
}
