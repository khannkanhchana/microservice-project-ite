import { useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { accountApi } from '../api/account'
import PublicLayout from '../components/PublicLayout'
import PasswordStrength from '../components/PasswordStrength'
import { Alert, Button, Field, PasswordInput } from '../components/ui'
import { errorMessage } from '../lib/format'

export default function ResetPassword() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const token = params.get('token')
  const [form, setForm] = useState({ newPassword: '', confirmPassword: '' })
  const [errors, setErrors] = useState({})
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(false)

  const submit = async (event) => {
    event.preventDefault()
    const local = {}
    if (form.newPassword.length < 8) local.newPassword = t('password.tooShort')
    if (form.newPassword !== form.confirmPassword) local.confirmPassword = t('password.mismatch')
    setErrors(local)
    if (Object.keys(local).length) return
    setBusy(true)
    setError(null)
    try {
      await accountApi.resetPassword({ token, ...form })
      navigate('/?notice=reset', { replace: true })
    } catch (err) {
      setError(errorMessage(err, t))
    } finally {
      setBusy(false)
    }
  }

  return (
    <PublicLayout>
      <div className="rounded-2xl border border-line bg-surface p-6 shadow-sm sm:p-8">
        <h1 className="text-xl font-semibold text-ink">{t('reset.title')}</h1>
        <p className="mt-1 text-sm text-muted">{t('reset.subtitle')}</p>
        {!token ? (
          <div className="mt-6">
            <Alert tone="danger">{t('reset.missingToken')}</Alert>
          </div>
        ) : (
          <form onSubmit={submit} className="mt-6 flex flex-col gap-4" noValidate>
            {error && <Alert tone="danger">{error}</Alert>}
            <Field label={t('password.new')} error={errors.newPassword}>
              {(p) => (
                <>
                  <PasswordInput
                    {...p}
                    autoComplete="new-password"
                    value={form.newPassword}
                    onChange={(e) => setForm({ ...form, newPassword: e.target.value })}
                  />
                  <PasswordStrength password={form.newPassword} />
                </>
              )}
            </Field>
            <Field label={t('password.confirm')} error={errors.confirmPassword}>
              {(p) => (
                <PasswordInput
                  {...p}
                  autoComplete="new-password"
                  value={form.confirmPassword}
                  onChange={(e) => setForm({ ...form, confirmPassword: e.target.value })}
                />
              )}
            </Field>
            <Button type="submit" className="h-11 w-full" loading={busy}>
              {t('reset.submit')}
            </Button>
          </form>
        )}
        <p className="mt-6 text-center text-sm">
          <Link to="/forgot-password" className="font-medium text-primary hover:underline">
            {t('reset.requestNew')}
          </Link>
        </p>
      </div>
    </PublicLayout>
  )
}
