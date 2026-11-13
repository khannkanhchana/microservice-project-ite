import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../auth/AuthContext'
import { handleCallback } from '../auth/oauth'
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
        await reloadUser()
        navigate(returnTo.startsWith('/') ? returnTo : '/account', { replace: true })
      })
      .catch((err) => setError(err.message))
  }, [navigate, reloadUser])

  return (
    <PublicLayout>
      <div className="rounded-2xl border border-line bg-surface p-8 text-center shadow-sm">
        {error ? (
          <>
            <Alert tone="danger" title={t('callback.failed')}>
              {error}
            </Alert>
            <Button className="mt-6 w-full" onClick={() => login({ returnTo: '/account' })}>
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
