import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { adminApi } from '../../api/admin'
import { Alert, Badge, Button, Card, EmptyState, PageHeader } from '../../components/ui'
import { LoadingBlock, LoginBars, StatTile } from '../../components/admin'
import { ListIcon, RefreshIcon } from '../../components/icons'
import { errorMessage, relativeTime } from '../../lib/format'
import { useResource } from '../../lib/useResource'

export function auditTone(entry) {
  if (entry.outcome === 'FAILURE') return 'danger'
  if (entry.category === 'SECURITY') return 'warning'
  if (entry.category === 'AUTHENTICATION') return 'primary'
  return 'neutral'
}

export default function Overview() {
  const { t, i18n } = useTranslation()
  const { data, loading, error, reload } = useResource(() => adminApi.overview(), [])

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
            <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
              <StatTile label={t('admin.overview.totalUsers')} value={data.totalUsers} hint={t('admin.overview.newUsers', { count: data.newUsersLast7Days })} />
              <StatTile label={t('admin.overview.activeUsers')} value={data.activeUsers} tone="success"
                hint={t('admin.overview.inactiveHint', { disabled: data.disabledUsers, locked: data.lockedUsers })} />
              <StatTile label={t('admin.overview.signedIn')} value={data.usersSignedInNow} tone="primary" hint={t('admin.overview.signedInHint')} />
              <StatTile label={t('admin.overview.suspicious')} value={data.suspiciousEvents7Days}
                tone={data.suspiciousEvents7Days ? 'warning' : 'neutral'} hint={t('admin.overview.last7Days')} />
              <StatTile label={t('admin.overview.roles')} value={data.roles} />
              <StatTile label={t('admin.overview.permissions')} value={data.permissions} />
              <StatTile label={t('admin.overview.clients')} value={data.clients} />
              <StatTile label={t('admin.overview.logins24h')} value={data.successfulLogins24h}
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
                {data.recentActivities.length === 0 ? (
                  <EmptyState icon={<ListIcon />} title={t('admin.audit.empty')} />
                ) : (
                  <ul className="divide-y divide-line">
                    {data.recentActivities.map((entry) => (
                      <li key={entry.id} className="flex items-start gap-3 px-5 py-3">
                        <Badge tone={auditTone(entry)}>{t(`admin.audit.categories.${entry.category}`, { defaultValue: entry.category })}</Badge>
                        <div className="min-w-0 flex-1">
                          <p className="truncate text-sm text-ink">{entry.description || entry.action}</p>
                          <p className="text-xs text-subtle">
                            {entry.actor || t('common.unknown')} · {relativeTime(entry.occurredAt, i18n.language)}
                          </p>
                        </div>
                      </li>
                    ))}
                  </ul>
                )}
              </Card>
            </div>
          </div>
        )
      )}
    </>
  )
}
