import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { accountApi } from '../../api/account'
import { useAuth } from '../../auth/AuthContext'
import { Alert, Button, Card, Checkbox, ConfirmDialog, Field, Input, PageHeader, PasswordInput, Select, Textarea } from '../../components/ui'
import { AlertIcon } from '../../components/icons'
import { errorMessage } from '../../lib/format'

const REASONS = ['reasonBreak', 'reasonPrivacy', 'reasonDuplicate', 'reasonNotUseful', 'reasonOther']

export default function Deactivate() {
  const { t } = useTranslation()
  const { user, endLocally } = useAuth()
  const [reason, setReason] = useState('')
  const [details, setDetails] = useState('')
  const [password, setPassword] = useState('')
  const [confirmation, setConfirmation] = useState('')
  const [understood, setUnderstood] = useState(false)
  const [open, setOpen] = useState(false)
  const [error, setError] = useState(null)

  const ready = understood && confirmation.trim() === user.username && (!user.hasPassword || password)

  const deactivate = async () => {
    setError(null)
    try {
      const reasonText = [reason && t(`deactivate.${reason}`, { lng: 'en' }), details.trim()].filter(Boolean).join(' — ')
      await accountApi.deactivate({
        password: user.hasPassword ? password : null,
        reason: reasonText || null,
        confirmation: confirmation.trim(),
      })
      endLocally('deactivated')
    } catch (err) {
      setError(errorMessage(err, t))
    }
  }

  return (
    <>
      <PageHeader title={t('deactivate.title')} description={t('deactivate.description')} />
      <div className="flex flex-col gap-6">
        <div className="rounded-xl border border-danger/30 bg-danger-soft p-5">
          <p className="flex items-center gap-2 font-semibold text-danger">
            <AlertIcon className="size-5" />
            {t('deactivate.warningTitle')}
          </p>
          <ul className="mt-3 list-disc space-y-1.5 pl-6 text-sm text-ink">
            {['warning1', 'warning2', 'warning3', 'warning4'].map((key) => (
              <li key={key}>{t(`deactivate.${key}`)}</li>
            ))}
          </ul>
        </div>

        <Card>
          <form
            className="flex max-w-lg flex-col gap-5"
            onSubmit={(event) => {
              event.preventDefault()
              if (ready) setOpen(true)
            }}
          >
            {error && (
              <Alert tone="danger" onDismiss={() => setError(null)}>
                {error}
              </Alert>
            )}
            <Field label={t('deactivate.reason')}>
              {(p) => (
                <Select {...p} value={reason} onChange={(e) => setReason(e.target.value)}>
                  <option value="">—</option>
                  {REASONS.map((key) => (
                    <option key={key} value={key}>
                      {t(`deactivate.${key}`)}
                    </option>
                  ))}
                </Select>
              )}
            </Field>
            <Field>
              {(p) => (
                <Textarea
                  {...p}
                  aria-label={t('deactivate.reason')}
                  maxLength={400}
                  placeholder={t('deactivate.reasonPlaceholder')}
                  value={details}
                  onChange={(e) => setDetails(e.target.value)}
                />
              )}
            </Field>
            {user.hasPassword && (
              <Field label={t('deactivate.password')}>
                {(p) => <PasswordInput {...p} autoComplete="current-password" value={password} onChange={(e) => setPassword(e.target.value)} />}
              </Field>
            )}
            <Field label={t('deactivate.confirmation', { username: user.username })}>
              {(p) => (
                <Input
                  {...p}
                  autoComplete="off"
                  spellCheck={false}
                  placeholder={user.username}
                  value={confirmation}
                  onChange={(e) => setConfirmation(e.target.value)}
                />
              )}
            </Field>
            <Checkbox checked={understood} onChange={(e) => setUnderstood(e.target.checked)} label={t('deactivate.understand')} />
            <div>
              <Button type="submit" variant="danger" disabled={!ready}>
                {t('deactivate.submit')}
              </Button>
            </div>
          </form>
        </Card>
      </div>

      <ConfirmDialog
        open={open}
        onClose={() => setOpen(false)}
        onConfirm={deactivate}
        title={t('deactivate.dialogTitle')}
        message={t('deactivate.dialogMessage')}
        confirmLabel={t('deactivate.submit')}
      />
    </>
  )
}
