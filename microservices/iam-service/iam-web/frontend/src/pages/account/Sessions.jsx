import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { accountApi } from '../../api/account'
import { useAuth } from '../../auth/AuthContext'
import { Alert, Badge, Button, Card, ConfirmDialog, EmptyState, PageHeader, Spinner, useToast } from '../../components/ui'
import { DevicesIcon, RefreshIcon } from '../../components/icons'
import { DEVICE_ICONS, deviceLabel } from '../../lib/devices'
import { errorMessage, formatDateTime, relativeTime } from '../../lib/format'
import { useResource } from '../../lib/useResource'

const ACTIVE_NOW_MS = 2 * 60 * 1000

export default function Sessions() {
  const { t, i18n } = useTranslation()
  const toast = useToast()
  const { logout, endLocally } = useAuth()
  const { data, loading, error, reload } = useResource(() => accountApi.sessions(), [])
  const [confirm, setConfirm] = useState(null)
  const [actionError, setActionError] = useState(null)

  const sessions = data || []
  const others = sessions.filter((session) => !session.current)

  const run = (action) => async () => {
    setActionError(null)
    try {
      await action()
    } catch (err) {
      setActionError(errorMessage(err, t))
    }
  }

  const revokeOne = (session) =>
    run(async () => {
      if (session.current) return logout()
      await accountApi.revokeSession(session.id)
      toast(t('sessions.revoked'))
      reload()
    })

  const revokeOthers = run(async () => {
    const { revoked } = await accountApi.revokeSessions('others')
    toast(revoked ? t('sessions.othersDone', { count: revoked }) : t('sessions.othersNone'))
    reload()
  })

  const revokeAll = run(async () => {
    await accountApi.revokeSessions('all')
    endLocally('1')
  })

  const dialogs = {
    one: confirm?.session && {
      title: confirm.session.current ? t('common.signOut') : t('sessions.revokeTitle'),
      message: t('sessions.revokeMessage', { device: deviceLabel(t, confirm.session) }),
      label: t('sessions.signOut'),
      action: revokeOne(confirm.session),
    },
    others: { title: t('sessions.othersTitle'), message: t('sessions.othersMessage'), label: t('sessions.signOutOthers'), action: revokeOthers },
    all: { title: t('sessions.allTitle'), message: t('sessions.allMessage'), label: t('sessions.signOutAll'), action: revokeAll },
  }
  const dialog = confirm && dialogs[confirm.kind]

  return (
    <>
      <PageHeader
        title={t('sessions.title')}
        description={t('sessions.description')}
        actions={
          <>
            <Button variant="secondary" onClick={reload} disabled={loading}>
              <RefreshIcon className="size-4" />
              {t('common.refresh')}
            </Button>
            <Button variant="secondary" disabled={!others.length} onClick={() => setConfirm({ kind: 'others' })}>
              {t('sessions.signOutOthers')}
            </Button>
            <Button variant="danger" disabled={!sessions.length} onClick={() => setConfirm({ kind: 'all' })}>
              {t('sessions.signOutAll')}
            </Button>
          </>
        }
      />
      {(error || actionError) && (
        <div className="mb-4">
          <Alert tone="danger" onDismiss={actionError ? () => setActionError(null) : undefined}>
            {actionError || errorMessage(error, t)}
          </Alert>
        </div>
      )}

      <Card padded={false} title={data ? t('sessions.count', { count: sessions.length }) : undefined}>
        {loading && !data ? (
          <div className="flex justify-center py-12 text-primary">
            <Spinner className="size-6" />
          </div>
        ) : sessions.length === 0 ? (
          <EmptyState icon={<DevicesIcon />} title={t('sessions.empty')} description={t('sessions.emptyDescription')} />
        ) : (
          <ul className="divide-y divide-line">
            {sessions.map((session) => {
              const Icon = DEVICE_ICONS[session.deviceType] || DevicesIcon
              const activeNow = Date.now() - new Date(session.lastActivityAt).getTime() < ACTIVE_NOW_MS
              return (
                <li key={session.id} className="flex flex-col gap-4 px-5 py-4 sm:flex-row sm:items-start">
                  <span
                    className={
                      'flex size-11 shrink-0 items-center justify-center rounded-xl ' +
                      (session.current ? 'bg-primary-soft text-primary' : 'bg-surface-2 text-muted')
                    }
                  >
                    <Icon />
                  </span>
                  <div className="min-w-0 flex-1">
                    <p className="flex flex-wrap items-center gap-2 font-medium text-ink">
                      {deviceLabel(t, session)}
                      {session.current && <Badge tone="primary">{t('sessions.thisDevice')}</Badge>}
                    </p>
                    <p className="mt-0.5 text-sm text-muted">
                      <span className={activeNow ? 'font-medium text-success' : ''}>
                        {activeNow ? t('sessions.activeNow') : t('sessions.lastActive', { when: relativeTime(session.lastActivityAt, i18n.language) })}
                      </span>
                      {' · '}
                      {t('sessions.via', { method: t(`methods.${session.loginMethod}`, { defaultValue: session.loginMethod }) })}
                    </p>
                    <dl className="mt-2 grid grid-cols-1 gap-x-6 gap-y-1 text-xs text-subtle sm:grid-cols-3">
                      <div>
                        <dt className="inline">{t('sessions.ip')}: </dt>
                        <dd className="inline font-mono text-muted">{session.ipAddress || '—'}</dd>
                      </div>
                      <div>
                        <dt className="inline">{t('sessions.location')}: </dt>
                        <dd className="inline text-muted">{session.location || t('common.unknown')}</dd>
                      </div>
                      <div>
                        <dd className="text-muted">{t('sessions.signedIn', { date: formatDateTime(session.createdAt, i18n.language) })}</dd>
                      </div>
                    </dl>
                  </div>
                  <Button variant="danger-outline" size="sm" onClick={() => setConfirm({ kind: 'one', session })}>
                    {t('sessions.signOut')}
                  </Button>
                </li>
              )
            })}
          </ul>
        )}
      </Card>

      <ConfirmDialog
        open={Boolean(dialog)}
        onClose={() => setConfirm(null)}
        onConfirm={dialog?.action || (() => {})}
        title={dialog?.title}
        message={dialog?.message}
        confirmLabel={dialog?.label}
      />
    </>
  )
}
