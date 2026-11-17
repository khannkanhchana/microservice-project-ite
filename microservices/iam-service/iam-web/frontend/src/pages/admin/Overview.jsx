import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { adminApi } from '../../api/admin'
import { Alert, Badge, Button, Card, EmptyState, PageHeader } from '../../components/ui'
import { LoadingBlock, LoginBars, StatTile } from '../../components/admin'
import { AlertIcon, AppIcon, CheckIcon, KeyIcon, ListIcon, PulseIcon, RefreshIcon, ShieldIcon, TagIcon, UsersIcon } from '../../components/icons'
import { errorMessage, relativeTime } from '../../lib/format'
import { useResource } from '../../lib/useResource'

export function auditTone(entry) {
  if (entry.outcome === 'FAILURE') return 'danger'
  if (entry.category === 'SECURITY') return 'warning'
  if (entry.category === 'AUTHENTICATION') return 'primary'
  return 'neutral'
}

const ACTIVITY_PAGE = 10
const ACTIVITY_MAX = 100

export default function Overview() {
  const { t, i18n } = useTranslation()
  const { data, loading, error, reload: reloadOverview } = useResource(() => adminApi.overview(), [])
  const [activityLimit, setActivityLimit] = useState(ACTIVITY_PAGE)
  const activities = useResource(() => adminApi.recentActivities(activityLimit), [activityLimit])
  const reload = () => {
    reloadOverview()
    activities.reload()
  }
  const recent = activities.data || data?.recentActivities || []
  const hasMore = activities.data && activities.data.length >= activityLimit && activityLimit < ACTIVITY_MAX

  return (
    <>
      <PageHeader
        title={t('admin.overview.title')}
        description={t('admin.overview.description')}
        actions={
          <Button variant="secondary" onClick={reload} disabled={loading}>
            <RefreshIcon className="size-4" />
            {t('common.refresh')}
          </Button>
        }
      />
      {error && (
        <div className="mb-4">
          <Alert tone="danger">{errorMessage(error, t)}</Alert>
        </div>
      )}
      {loading && !data ? (
        <LoadingBlock />
      ) : (
        data && (
          <div className="flex flex-col gap-6">
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
              <StatTile label={t('admin.overview.totalUsers')} icon={UsersIcon} tone="primary" value={data.totalUsers} hint={t('admin.overview.newUsers', { count: data.newUsersLast7Days })} />
              <StatTile label={t('admin.overview.activeUsers')} icon={CheckIcon} value={data.activeUsers} tone="success"
                hint={t('admin.overview.inactiveHint', { disabled: data.disabledUsers, locked: data.lockedUsers })} />
              <StatTile label={t('admin.overview.signedIn')} icon={PulseIcon} value={data.usersSignedInNow} tone="primary" hint={t('admin.overview.signedInHint')} />
              <StatTile label={t('admin.overview.suspicious')} icon={AlertIcon} value={data.suspiciousEvents7Days}
                tone={data.suspiciousEvents7Days ? 'warning' : 'neutral'} hint={t('admin.overview.last7Days')} />
              <StatTile label={t('admin.overview.roles')} icon={TagIcon} value={data.roles} />
              <StatTile label={t('admin.overview.permissions')} icon={ShieldIcon} value={data.permissions} />
              <StatTile label={t('admin.overview.clients')} icon={AppIcon} value={data.clients} />
              <StatTile label={t('admin.overview.logins24h')} icon={KeyIcon} value={data.successfulLogins24h}
                hint={t('admin.overview.failed24h', { count: data.failedLogins24h })} tone={data.failedLogins24h > data.successfulLogins24h ? 'warning' : 'neutral'} />
            </div>

            <div className="grid grid-cols-1 gap-6 lg:grid-cols-5">
              <Card className="lg:col-span-2" title={t('admin.overview.loginsChart')} description={t('admin.overview.last7Days')}>
                <LoginBars
                  data={data.loginsLast7Days}
                  labels={{ aria: t('admin.overview.loginsChart'), successful: t('admin.logins.successful'), failed: t('admin.logins.failed') }}
                />
              </Card>
              <Card
                className="lg:col-span-3"
                padded={false}
                title={t('admin.overview.recentActivity')}
                actions={
                  <Link to="/admin/audit-logs" className="text-sm font-medium text-primary hover:underline">
                    {t('admin.overview.viewAll')}
                  </Link>
                }
              >
                {activities.error && <div className="px-6 pt-4"><Alert tone="danger">{errorMessage(activities.error, t)}</Alert></div>}
                {recent.length === 0 ? (
                  <EmptyState icon={<ListIcon />} title={t('admin.audit.empty')} />
                ) : (
                  <ul className="divide-y divide-line">
                    {recent.map((entry) => (
                      <li key={entry.id}>
                        <Link to={`/admin/audit-logs/${entry.id}`} className="flex items-start gap-3 px-6 py-3.5 transition-colors hover:bg-surface-2/50">
                        <Badge tone={auditTone(entry)}>{t(`admin.audit.categories.${entry.category}`, { defaultValue: entry.category })}</Badge>
                        <div className="min-w-0 flex-1">
                          <p className="truncate text-sm text-ink">{entry.description || entry.action}</p>
                          <p className="text-xs text-subtle">
                            {entry.actor || t('common.unknown')} · {relativeTime(entry.occurredAt, i18n.language)}
                          </p>
                        </div>
                        </Link>
                      </li>
                    ))}
                  </ul>
                )}
                {hasMore && (
                  <div className="border-t border-line px-6 py-3 text-center">
                    <Button
                      size="sm"
                      variant="secondary"
                      loading={activities.loading}
                      onClick={() => setActivityLimit((n) => Math.min(n + ACTIVITY_PAGE, ACTIVITY_MAX))}
                    >
                      {t('admin.overview.showMore')}
                    </Button>
                  </div>
                )}
              </Card>
            </div>
          </div>
        )
      )}
    </>
  )
}
