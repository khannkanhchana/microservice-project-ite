import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { accountApi } from '../../api/account'
import { useAuth } from '../../auth/AuthContext'
import { Alert, Badge, Button, Card, Checkbox, ConfirmDialog, Field, Input, PageHeader, PasswordInput, Select, Spinner, Textarea, useToast } from '../../components/ui'
import { AlertIcon, PowerIcon } from '../../components/icons'
import { errorMessage, formatDateTime } from '../../lib/format'
import { useResource } from '../../lib/useResource'

const REASONS = ['reasonBreak', 'reasonPrivacy', 'reasonDuplicate', 'reasonNotUseful', 'reasonOther']

export default function Deactivate() {
  const { t, i18n } = useTranslation()
  const { user, endLocally } = useAuth()
  const toast = useToast()
  // GET /api/me/deactivation-request: the latest request (PENDING, CANCELLED or COMPLETED) or null.
  const request = useResource(() => accountApi.deactivationRequest(), [])
  const [cancelOpen, setCancelOpen] = useState(false)
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
      const result = await accountApi.requestDeactivation({
        password: user.hasPassword ? password : null,
        reason: reasonText || null,
        confirmation: confirmation.trim(),
      })
      // With no grace period configured on the server the account is deactivated at once.
      if (result?.status === 'COMPLETED') {
        endLocally('deactivated')
        return
      }
      setPassword('')
      setConfirmation('')
      setUnderstood(false)
      request.setData(result)
      toast(t('deactivate.requested'))
    } catch (err) {
      setError(errorMessage(err, t))
    }
  }

  const cancelRequest = async () => {
    try {
      request.setData(await accountApi.cancelDeactivation())
      toast(t('deactivate.cancelled'))
    } catch (err) {
      toast(errorMessage(err, t), 'danger')
      request.reload()
    }
  }

  const current = request.data
  const pending = current?.status === 'PENDING'

  if (request.loading && !current) {
    return (
      <>
        <PageHeader title={t('deactivate.title')} description={t('deactivate.description')} />
        <div className="flex justify-center py-12"><Spinner /></div>
      </>
    )
  }

  if (pending) {
    return (
      <>
        <PageHeader title={t('deactivate.title')} description={t('deactivate.description')} />
        <Card>
          <div className="flex flex-col gap-5 sm:flex-row sm:items-start">
            <div className="flex size-11 shrink-0 items-center justify-center rounded-xl bg-warning-soft text-warning">
              <PowerIcon className="size-5" />
            </div>
            <div className="min-w-0 flex-1">
              <div className="flex flex-wrap items-center gap-2">
                <h2 className="text-base font-semibold text-ink">{t('deactivate.pendingTitle')}</h2>
                <Badge tone="warning">{t('deactivate.statusPending')}</Badge>
              </div>
              <p className="mt-2 text-sm text-muted">
                {t('deactivate.pendingBody', { date: formatDateTime(current.scheduledFor, i18n.language) })}
              </p>
              <dl className="mt-4 grid gap-3 text-sm sm:grid-cols-2">
                <div>
                  <dt className="text-subtle">{t('deactivate.requestedAt')}</dt>
                  <dd className="text-ink">{formatDateTime(current.requestedAt, i18n.language)}</dd>
                </div>
                <div>
                  <dt className="text-subtle">{t('deactivate.scheduledFor')}</dt>
                  <dd className="text-ink">{formatDateTime(current.scheduledFor, i18n.language)}</dd>
                </div>
                {current.reason && (
                  <div className="sm:col-span-2">
                    <dt className="text-subtle">{t('deactivate.reasonLabel')}</dt>
                    <dd className="break-words text-ink">{current.reason}</dd>
                  </div>
                )}
              </dl>
              <div className="mt-5">
                <Button variant="secondary" onClick={() => setCancelOpen(true)}>{t('deactivate.cancel')}</Button>
              </div>
            </div>
          </div>
        </Card>
        <ConfirmDialog
          open={cancelOpen}
          onClose={() => setCancelOpen(false)}
          onConfirm={cancelRequest}
          tone="primary"
          title={t('deactivate.cancelDialogTitle')}
          message={t('deactivate.cancelDialogMessage')}
          confirmLabel={t('deactivate.cancel')}
        />
      </>
    )
  }

  return (
    <>
      <PageHeader title={t('deactivate.title')} description={t('deactivate.description')} />
      <div className="flex flex-col gap-6">
        {request.error && <Alert tone="danger">{errorMessage(request.error, t)}</Alert>}
        {current?.status === 'CANCELLED' && (
          <Alert tone="info">{t('deactivate.lastCancelled', { date: formatDateTime(current.cancelledAt, i18n.language) })}</Alert>
        )}
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
