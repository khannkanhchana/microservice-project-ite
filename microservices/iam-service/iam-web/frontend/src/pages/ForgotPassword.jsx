import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { accountApi } from '../api/account'
import PublicLayout from '../components/PublicLayout'
import { Alert, Button, Field, Input } from '../components/ui'
import { MailIcon } from '../components/icons'
import { errorMessage } from '../lib/format'

export default function ForgotPassword() {
  const { t } = useTranslation()
  const [email, setEmail] = useState('')
  const [sentTo, setSentTo] = useState(null)
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(false)

  const submit = async (event) => {
    event.preventDefault()
    setBusy(true)
    setError(null)
    try {
      await accountApi.forgotPassword(email.trim())
      setSentTo(email.trim())
    } catch (err) {
      setError(err.fieldErrors?.email || errorMessage(err, t))
    } finally {
      setBusy(false)
    }
  }

  return (
    <PublicLayout>
      <div className="rounded-2xl border border-line bg-surface p-6 shadow-sm sm:p-8">
        <div className="mb-4 inline-flex rounded-full bg-primary-soft p-3 text-primary">
          <MailIcon />
        </div>
        <h1 className="text-xl font-semibold text-ink">{t('forgot.title')}</h1>
        <p className="mt-1 text-sm text-muted">{t('forgot.subtitle')}</p>

        {sentTo ? (
          <div className="mt-6 flex flex-col gap-3">
            <Alert tone="success">{t('forgot.sent', { email: sentTo })}</Alert>
            {import.meta.env.DEV && <Alert tone="info">{t('forgot.devNote')}</Alert>}
          </div>
        ) : (
          <form onSubmit={submit} className="mt-6 flex flex-col gap-4">
            {error && <Alert tone="danger">{error}</Alert>}
            <Field label={t('forgot.email')}>
              {(p) => (
                <Input {...p} type="email" required autoComplete="email" value={email} onChange={(e) => setEmail(e.target.value)} />
              )}
            </Field>
            <Button type="submit" className="h-11 w-full" loading={busy}>
              {t('forgot.submit')}
            </Button>
          </form>
        )}
        <p className="mt-6 text-center text-sm">
          <Link to="/" className="font-medium text-primary hover:underline">
            {t('forgot.back')}
          </Link>
        </p>
      </div>
    </PublicLayout>
  )
}
