import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../auth/AuthContext'
import { handleCallback } from '../auth/oauth'
import { hasPermission } from '../lib/permissions'
import PublicLayout from '../components/PublicLayout'
import { Alert, Button, Spinner } from '../components/ui'

export default function Callback() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const { reloadUser, login } = useAuth()
  const [error, setError] = useState(null)
  const started = useRef(false)

  useEffect(() => {
    // StrictMode runs effects twice in development; the authorization code is single-use.
    if (started.current) return
    started.current = true
    handleCallback(window.location.search)
      .then(async (returnTo) => {
        const me = await reloadUser()
        // A plain sign-in (no deep link to return to) lands on an overview: the admin console overview for
        // users who may see it, the account overview for everyone else.
        const deepLink = returnTo.startsWith('/') && returnTo !== '/me' && returnTo !== '/'
        const home = hasPermission(me, 'dashboard:read') ? '/admin' : '/me'
        navigate(deepLink ? returnTo : home, { replace: true })
      })
      .catch((err) => setError(err.message))
  }, [navigate, reloadUser])

  return (
    <PublicLayout>
      <div className="rounded-xl border border-line bg-surface p-8 text-center">
        {error ? (
          <>
            <Alert tone="danger" title={t('callback.failed')}>
              {error}
            </Alert>
            <Button className="mt-6 w-full" onClick={() => login({ returnTo: '/me' })}>
              {t('callback.tryAgain')}
            </Button>
          </>
        ) : (
          <div className="flex flex-col items-center gap-3 text-muted">
            <Spinner className="size-8 text-primary" />
            <p>{t('callback.working')}</p>
          </div>
        )}
      </div>
    </PublicLayout>
  )
}
