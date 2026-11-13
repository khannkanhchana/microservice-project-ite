import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { adminApi } from '../../api/admin'
import { Alert, Badge, Button, Card, EmptyState, Input, PageHeader, Pagination, Select } from '../../components/ui'
import { LoadingBlock, SearchBox, Toolbar } from '../../components/admin'
import { CheckIcon, RefreshIcon } from '../../components/icons'
import { errorMessage, relativeTime } from '../../lib/format'
import { useResource } from '../../lib/useResource'
import { LoginTable } from './UserDetail'

const SEVERITY_TONE = { high: 'danger', medium: 'warning', low: 'neutral' }

/** yyyy-mm-dd (local) -> ISO instant at local midnight; `end` adds one day so the range includes that date. */
const dayToInstant = (day, end) => {
  if (!day) return undefined
  const date = new Date(`${day}T00:00:00`)
  if (end) date.setDate(date.getDate() + 1)
  return date.toISOString()
}

export default function LoginMonitor() {
  const { t, i18n } = useTranslation()
  const [days, setDays] = useState(7)
  const suspicious = useResource(() => adminApi.suspicious(days), [days])
  const [filters, setFilters] = useState({ search: '', status: 'all', method: '', from: '', to: '', page: 0 })
  const history = useResource(
    () => adminApi.loginHistory({ ...filters, from: dayToInstant(filters.from), to: dayToInstant(filters.to, true), size: 20 }),
    [filters],
  )
  const filter = (patch) => setFilters((f) => ({ ...f, ...patch, page: 0 }))

  return (
    <>
      <PageHeader
        title={t('admin.logins.title')}
        description={t('admin.logins.description')}
        actions={
          <Button variant="secondary" onClick={() => { suspicious.reload(); history.reload() }}>
            <RefreshIcon className="size-4" />{t('common.refresh')}
          </Button>
        }
      />

      <Card
        className="mb-6"
        padded={false}
        title={t('admin.logins.suspicious')}
        description={t('admin.logins.suspiciousHint')}
        actions={
          <Select value={days} onChange={(e) => setDays(Number(e.target.value))} className="h-8 w-36 py-0" aria-label={t('admin.logins.window')}>
            {[1, 7, 30, 90].map((d) => <option key={d} value={d}>{t('admin.logins.lastDays', { count: d })}</option>)}
          </Select>
        }
      >
        {suspicious.error && <div className="p-4"><Alert tone="danger">{errorMessage(suspicious.error, t)}</Alert></div>}
        {suspicious.loading && !suspicious.data ? <LoadingBlock /> : (suspicious.data || []).length === 0 ? (
          <EmptyState icon={<CheckIcon />} title={t('admin.logins.noSuspicious')} description={t('admin.logins.noSuspiciousHint')} />
        ) : (
          <ul className="divide-y divide-line">
            {suspicious.data.map((s, i) => (
              <li key={`${s.type}-${s.subject}-${i}`} className="flex flex-col gap-2 px-5 py-3 sm:flex-row sm:items-center">
                <div className="flex items-center gap-2 sm:w-64">
                  <Badge tone={SEVERITY_TONE[s.severity]}>{t(`admin.logins.severity.${s.severity}`)}</Badge>
                  <span className="text-sm font-medium text-ink">{t(`admin.logins.types.${s.type}`)}</span>
                </div>
                <div className="min-w-0 flex-1 text-sm text-muted">
                  <p className="text-ink">
                    {s.userUuid ? <Link to={`/admin/users/${s.userUuid}`} className="font-medium hover:text-primary">{s.subject}</Link>
                      : <span className="font-mono">{s.subject}</span>}
                  </p>
                  <p className="text-xs">
                    {t(`admin.logins.detail.${s.type}`, { count: s.attempts, accounts: s.distinctAccounts })}
                    {s.ipAddress && <> · <span className="font-mono">{s.ipAddress}</span></>}
                    {' · '}{t('admin.logins.lastSeen', { when: relativeTime(s.lastSeen, i18n.language) })}
                  </p>
                </div>
                <button type="button" className="self-start text-xs font-medium text-primary hover:underline sm:self-center"
                  onClick={() => filter({ search: s.type === 'PASSWORD_SPRAY' ? s.ipAddress : s.subject, status: 'all' })}>
                  {t('admin.logins.showAttempts')}
                </button>
              </li>
            ))}
          </ul>
        )}
      </Card>

      <Card padded={false} title={t('admin.logins.history')}>
        <Toolbar>
          <SearchBox key={filters.search} value={filters.search} onChange={(search) => filter({ search })} placeholder={t('admin.logins.search')} />
          <Select value={filters.status} onChange={(e) => filter({ status: e.target.value })} className="sm:w-36" aria-label={t('admin.logins.result')}>
            <option value="all">{t('admin.logins.all')}</option>
            <option value="success">{t('admin.logins.successful')}</option>
            <option value="failed">{t('admin.logins.failed')}</option>
          </Select>
          <Select value={filters.method} onChange={(e) => filter({ method: e.target.value })} className="sm:w-36" aria-label={t('admin.logins.method')}>
            <option value="">{t('admin.logins.allMethods')}</option>
            {['password', 'google', 'github', 'facebook', 'telegram'].map((m) => <option key={m} value={m}>{t(`methods.${m}`, { defaultValue: m })}</option>)}
          </Select>
          <label className="flex items-center gap-2 text-xs text-muted">
            {t('admin.common.from')}
            <Input type="date" value={filters.from} onChange={(e) => filter({ from: e.target.value })} className="sm:w-40" />
          </label>
          <label className="flex items-center gap-2 text-xs text-muted">
            {t('admin.common.to')}
            <Input type="date" value={filters.to} onChange={(e) => filter({ to: e.target.value })} className="sm:w-40" />
          </label>
        </Toolbar>
        {history.error && <div className="p-4"><Alert tone="danger">{errorMessage(history.error, t)}</Alert></div>}
        <LoginTable data={history.data} loading={history.loading} t={t} lang={i18n.language} />
        {history.data && (
          <Pagination page={history.data.page} totalPages={history.data.totalPages} totalElements={history.data.totalElements}
            onChange={(page) => setFilters((f) => ({ ...f, page }))} />
        )}
      </Card>
    </>
  )
}
