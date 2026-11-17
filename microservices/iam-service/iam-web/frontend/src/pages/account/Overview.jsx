import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { accountApi } from '../../api/account'
import { useAuth } from '../../auth/AuthContext'
import { Alert, Avatar, Badge, buttonVariants, Card, EmptyState, PageHeader } from '../../components/ui'
import { LoadingBlock, StatTile } from '../../components/admin'
import {
  ActivityIcon, AlertIcon, CheckIcon, DevicesIcon, GridIcon, HistoryIcon, KeyIcon, LinkIcon, ShieldIcon, UserIcon,
} from '../../components/icons'
import { DEVICE_ICONS, deviceLabel } from '../../lib/devices'
import { errorMessage, formatDateTime, relativeTime } from '../../lib/format'
import { canAdminister } from '../../lib/permissions'
import { useResource } from '../../lib/useResource'
import { ACTIVITY_STYLE, describeActivity } from './Activity'

const RECENT = 5

/** Everything the overview shows, loaded in parallel; a failing part does not hide the others. */
async function loadOverview() {
  const settle = (promise) => promise.then((value) => ({ value }), (error) => ({ error }))
  const [sessions, identities, logins, failed, activity, deactivation] = await Promise.all([
    settle(accountApi.sessions()),
    settle(accountApi.identities()),
    settle(accountApi.loginHistory({ page: 0, size: RECENT })),
    settle(accountApi.loginHistory({ status: 'failed', page: 0, size: 1 })),
    settle(accountApi.activity({ page: 0, size: RECENT })),
    settle(accountApi.deactivationRequest()),
  ])
  return { sessions, identities, logins, failed, activity, deactivation }
}

/** Account home after sign-in: who you are, how secure the account is, and what happened recently. */
export default function AccountOverview() {
  const { t, i18n } = useTranslation()
  const { user } = useAuth()
  const { data, loading, error } = useResource(loadOverview, [])

  if (loading && !data) return <LoadingBlock />
  if (error) return <Alert tone="danger">{errorMessage(error, t)}</Alert>
  if (!data) return null

  const sessions = data.sessions.value || []
  const identities = data.identities.value || []
  const logins = data.logins.value?.content || []
  const failedCount = data.failed.value?.totalElements ?? 0
  const activity = data.activity.value?.content || []
  const pendingDeactivation = data.deactivation.value?.status === 'PENDING' ? data.deactivation.value : null
  const partError = ['sessions', 'identities', 'logins', 'activity'].map((k) => data[k].error).find(Boolean)

  const checks = [
    { ok: user.hasPassword, label: t('overview.checkPassword'), to: '/account/security' },
    { ok: user.emailVerified, label: t('overview.checkEmail'), to: '/account/profile' },
    { ok: Boolean(user.pictureUrl), label: t('overview.checkPicture'), to: '/account/profile' },
    { ok: identities.length > 0, label: t('overview.checkLinked'), to: '/account/linked-accounts' },
  ]
  const score = checks.filter((c) => c.ok).length

  return (
    <>
      <PageHeader
        title={t('overview.welcome', { name: user.name?.split(' ')[0] || user.username })}
        description={t('overview.description')}
        actions={canAdminister(user) && (
          <Link to="/admin" className={buttonVariants({ variant: 'secondary' })}>
            <GridIcon className="size-4" />{t('nav.workspaceAdmin')}
          </Link>
        )}
      />

      <div className="flex flex-col gap-6">
        {partError && <Alert tone="warning">{errorMessage(partError, t)}</Alert>}
        {pendingDeactivation && (
          <Alert tone="warning" title={t('deactivate.pendingTitle')}>
            {t('deactivate.pendingBody', { date: formatDateTime(pendingDeactivation.scheduledFor, i18n.language) })}{' '}
            <Link to="/account/deactivate" className="font-medium underline">{t('overview.manage')}</Link>
          </Alert>
        )}

        {/* Profile summary */}
        <Card>
          <div className="flex flex-col gap-5 sm:flex-row sm:items-center">
            <Avatar src={user.pictureUrl} name={user.name || user.username} size="xl" />
            <div className="min-w-0 flex-1">
              <p className="truncate text-lg font-semibold text-ink">{user.name || user.username}</p>
              <p className="truncate text-sm text-muted">@{user.username} · {user.email}</p>
              <div className="mt-2 flex flex-wrap gap-1.5">
                {(user.roles || []).map((role) => <Badge key={role.uuid || role.name} tone="primary">{role.name}</Badge>)}
              </div>
            </div>
            <div className="text-sm sm:text-right">
              <p className="text-subtle">{t('profile.lastLogin')}</p>
              <p className="text-ink">{user.lastLoginAt ? formatDateTime(user.lastLoginAt, i18n.language) : t('common.never')}</p>
              <Link to="/account/profile" className="mt-1 inline-block font-medium text-primary hover:underline">{t('overview.editProfile')}</Link>
            </div>
          </div>
        </Card>

        {/* Key numbers */}
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          <Link to="/account/sessions" className="rounded-xl outline-none focus-visible:ring-[3px] focus-visible:ring-ring"><StatTile label={t('overview.activeSessions')} value={sessions.length} icon={DevicesIcon} tone="primary" /></Link>
          <Link to="/account/linked-accounts" className="rounded-xl outline-none focus-visible:ring-[3px] focus-visible:ring-ring"><StatTile label={t('overview.linkedAccounts')} value={identities.length} icon={LinkIcon} /></Link>
          <Link to="/account/login-history" className="rounded-xl outline-none focus-visible:ring-[3px] focus-visible:ring-ring"><StatTile label={t('overview.failedSignIns')} value={failedCount} icon={AlertIcon} tone={failedCount > 0 ? 'danger' : 'success'} /></Link>
          <Link to="/account/security" className="rounded-xl outline-none focus-visible:ring-[3px] focus-visible:ring-ring"><StatTile label={t('overview.securityScore')} value={`${score}/${checks.length}`} icon={ShieldIcon} tone={score === checks.length ? 'success' : 'warning'} /></Link>
        </div>

        <div className="grid gap-6 lg:grid-cols-2">
          {/* Security checklist */}
          <Card title={t('overview.checklist')} description={t('overview.checklistHint')} padded={false}>
            <ul className="divide-y divide-line">
              {checks.map((c) => (
                <li key={c.label}>
                  <Link to={c.to} className="flex items-center gap-3 px-6 py-3.5 transition-colors hover:bg-surface-2/50">
                    <span className={`flex size-7 shrink-0 items-center justify-center rounded-full ${c.ok ? 'bg-success-soft text-success' : 'bg-warning-soft text-warning'}`}>
                      {c.ok ? <CheckIcon className="size-4" /> : <AlertIcon className="size-4" />}
                    </span>
                    <span className="flex-1 text-sm text-ink">{c.label}</span>
                    <Badge tone={c.ok ? 'success' : 'warning'}>{c.ok ? t('overview.done') : t('overview.todo')}</Badge>
                  </Link>
                </li>
              ))}
            </ul>
          </Card>

          {/* Recent sign-ins */}
          <Card
            title={t('overview.recentSignIns')}
            padded={false}
            actions={<Link to="/account/login-history" className="text-sm font-medium text-primary hover:underline">{t('overview.viewAll')}</Link>}
          >
            {logins.length === 0 ? (
              <EmptyState icon={<HistoryIcon />} title={t('overview.noSignIns')} />
            ) : (
              <ul className="divide-y divide-line">
                {logins.map((row) => {
                  const Icon = DEVICE_ICONS[row.deviceType] || DEVICE_ICONS.desktop
                  return (
                    <li key={row.id} className="flex items-center gap-3 px-6 py-3.5">
                      <Icon className="size-5 shrink-0 text-subtle" />
                      <div className="min-w-0 flex-1">
                        <p className="truncate text-sm text-ink">{deviceLabel(t, row)}</p>
                        <p className="truncate text-xs text-subtle">
                          <span className="font-mono">{row.ipAddress || '—'}</span> · {relativeTime(row.attemptedAt, i18n.language)}
                        </p>
                      </div>
                      <Badge tone={row.success ? 'success' : 'danger'}>{row.success ? t('overview.success') : t('overview.failed')}</Badge>
                    </li>
                  )
                })}
              </ul>
            )}
          </Card>
        </div>

        {/* Recent activity */}
        <Card
          title={t('overview.recentActivity')}
          padded={false}
          actions={<Link to="/account/activity" className="text-sm font-medium text-primary hover:underline">{t('overview.viewAll')}</Link>}
        >
          {activity.length === 0 ? (
            <EmptyState icon={<ActivityIcon />} title={t('overview.noActivity')} />
          ) : (
            <ul className="divide-y divide-line">
              {activity.map((item) => {
                const [Icon, tone] = ACTIVITY_STYLE[item.type] || [ActivityIcon, 'bg-surface-2 text-muted']
                const [title, detail] = describeActivity(t, item)
                return (
                  <li key={item.id} className="flex items-start gap-3 px-6 py-3.5">
                    <span className={`flex size-8 shrink-0 items-center justify-center rounded-lg ${tone}`}><Icon className="size-4" /></span>
                    <div className="min-w-0 flex-1">
                      <p className="truncate text-sm text-ink">{title}</p>
                      <p className="truncate text-xs text-subtle">{[detail, relativeTime(item.occurredAt, i18n.language)].filter(Boolean).join(' · ')}</p>
                    </div>
                  </li>
                )
              })}
            </ul>
          )}
        </Card>

        {/* Shortcuts */}
        <div className="grid gap-3 sm:grid-cols-3">
          {[
            { to: '/account/profile', icon: UserIcon, label: 'nav.profile' },
            { to: '/account/security', icon: KeyIcon, label: 'nav.security' },
            { to: '/account/sessions', icon: DevicesIcon, label: 'nav.sessions' },
          ].map(({ to, icon: Icon, label }) => (
            <Link key={to} to={to} className="flex items-center gap-3 rounded-xl border border-line bg-surface px-4 py-3 text-sm font-medium text-ink transition-colors hover:border-primary hover:text-primary">
              <Icon className="size-4 text-subtle" />
              {t(label)}
            </Link>
          ))}
        </div>
      </div>
    </>
  )
}
