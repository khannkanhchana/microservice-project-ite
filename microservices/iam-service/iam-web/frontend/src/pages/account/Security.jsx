import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { accountApi } from '../../api/account'
import { useAuth } from '../../auth/AuthContext'
import PasswordStrength from '../../components/PasswordStrength'
import { Alert, Button, Card, Checkbox, Field, PageHeader, PasswordInput, useToast } from '../../components/ui'
import { CheckIcon, MailIcon } from '../../components/icons'
import { errorMessage, relativeTime } from '../../lib/format'

const EMPTY = { currentPassword: '', newPassword: '', confirmPassword: '' }

function ChangePasswordCard() {
  const { t, i18n } = useTranslation()
  const toast = useToast()
  const { user, reloadUser } = useAuth()
  const [form, setForm] = useState(EMPTY)
  const [signOutOthers, setSignOutOthers] = useState(true)
  const [errors, setErrors] = useState({})
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(false)
  const set = (key) => (event) => setForm((current) => ({ ...current, [key]: event.target.value }))

  const submit = async (event) => {
    event.preventDefault()
    const local = {}
    if (user.hasPassword && !form.currentPassword) local.currentPassword = t('profile.required')
    if (form.newPassword.length < 8) local.newPassword = t('password.tooShort')
    if (form.newPassword !== form.confirmPassword) local.confirmPassword = t('password.mismatch')
    setErrors(local)
    if (Object.keys(local).length) return
    setBusy(true)
    setError(null)
    try {
      await accountApi.changePassword({
        currentPassword: user.hasPassword ? form.currentPassword : null,
        newPassword: form.newPassword,
        confirmPassword: form.confirmPassword,
        signOutOtherSessions: signOutOthers,
      })
      setForm(EMPTY)
      await reloadUser()
      toast(t('security.changed'))
    } catch (err) {
      setErrors(err.fieldErrors || {})
      setError(errorMessage(err, t))
    } finally {
      setBusy(false)
    }
  }

  const description = !user.hasPassword
    ? t('security.setPasswordDescription')
    : user.passwordChangedAt
      ? t('security.passwordDescription', { when: relativeTime(user.passwordChangedAt, i18n.language) })
      : t('security.passwordNever')

  return (
    <Card title={user.hasPassword ? t('security.passwordTitle') : t('security.setPasswordTitle')} description={description}>
      <form onSubmit={submit} className="flex max-w-md flex-col gap-4" noValidate>
        {error && (
          <Alert tone="danger" onDismiss={() => setError(null)}>
            {error}
          </Alert>
        )}
        {user.hasPassword && (
          <Field label={t('password.current')} error={errors.currentPassword}>
            {(p) => <PasswordInput {...p} autoComplete="current-password" value={form.currentPassword} onChange={set('currentPassword')} />}
          </Field>
        )}
        <Field label={t('password.new')} error={errors.newPassword}>
          {(p) => (
            <>
              <PasswordInput {...p} autoComplete="new-password" value={form.newPassword} onChange={set('newPassword')} />
              <PasswordStrength password={form.newPassword} />
            </>
          )}
        </Field>
        <Field label={t('password.confirm')} error={errors.confirmPassword}>
          {(p) => <PasswordInput {...p} autoComplete="new-password" value={form.confirmPassword} onChange={set('confirmPassword')} />}
        </Field>
        <Checkbox
          checked={signOutOthers}
          onChange={(event) => setSignOutOthers(event.target.checked)}
          label={t('security.signOutOthers')}
          description={t('security.signOutOthersHint')}
        />
        <div>
          <Button type="submit" loading={busy}>
            {t('security.submit')}
          </Button>
        </div>
      </form>
    </Card>
  )
}

function ResetByEmailCard() {
  const { t } = useTranslation()
  const { user } = useAuth()
  const [state, setState] = useState({ busy: false, sent: false, error: null })

  const send = async () => {
    setState({ busy: true, sent: false, error: null })
    try {
      await accountApi.forgotPassword(user.email)
      setState({ busy: false, sent: true, error: null })
    } catch (err) {
      setState({ busy: false, sent: false, error: errorMessage(err, t) })
    }
  }

  return (
    <Card title={t('security.resetTitle')} description={t('security.resetDescription', { email: user.email })}>
      <div className="flex flex-col items-start gap-3">
        {state.sent && <Alert tone="success">{t('security.resetSent', { email: user.email })}</Alert>}
        {state.sent && import.meta.env.DEV && <Alert tone="info">{t('forgot.devNote')}</Alert>}
        {state.error && <Alert tone="danger">{state.error}</Alert>}
        <Button variant="secondary" onClick={send} loading={state.busy}>
          <MailIcon className="size-4" />
          {t('security.resetSend')}
        </Button>
      </div>
    </Card>
  )
}

export default function Security() {
  const { t } = useTranslation()
  return (
    <>
      <PageHeader title={t('security.title')} description={t('security.description')} />
      <div className="grid grid-cols-1 gap-6 xl:grid-cols-3">
        <div className="flex flex-col gap-6 xl:col-span-2">
          <ChangePasswordCard />
          <ResetByEmailCard />
        </div>
        <Card title={t('security.tipsTitle')} className="h-fit">
          <ul className="flex flex-col gap-3 text-sm text-muted">
            {['security.tip1', 'security.tip2', 'security.tip3'].map((key) => (
              <li key={key} className="flex gap-2">
                <CheckIcon className="mt-0.5 size-4 shrink-0 text-success" />
                {t(key)}
              </li>
            ))}
          </ul>
        </Card>
      </div>
    </>
  )
}
