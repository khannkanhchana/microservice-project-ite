import { Fragment, useState } from 'react'
import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { adminApi } from '../../api/admin'
import { Alert, Badge, Card, EmptyState, Input, PageHeader, Pagination, Select } from '../../components/ui'
import { SearchBox, Table, Td, Toolbar } from '../../components/admin'
import { ListIcon } from '../../components/icons'
import { errorMessage, formatDateTime } from '../../lib/format'
import { useResource } from '../../lib/useResource'
import { auditTone } from './Overview'

export const AUDIT_CATEGORIES = [
  'AUTHENTICATION', 'ACCOUNT', 'USER', 'ROLE_ASSIGNMENT', 'PERMISSION_ASSIGNMENT', 'ROLE', 'ROLE_PERMISSION',
  'PERMISSION', 'CLIENT', 'SESSION', 'SECURITY', 'CONFIGURATION',
]

const dayToInstant = (day, end) => {
  if (!day) return undefined
  const date = new Date(`${day}T00:00:00`)
  if (end) date.setDate(date.getDate() + 1)
  return date.toISOString()
}

const pretty = (json) => {
  try {
    return JSON.stringify(JSON.parse(json), null, 2)
  } catch {
    return json
  }
}

export default function AuditLogs() {
  const { t, i18n } = useTranslation()
  const [filters, setFilters] = useState({ category: '', outcome: '', actor: '', search: '', from: '', to: '', page: 0 })
  const { data, loading, error } = useResource(
    () => adminApi.auditLogs({ ...filters, from: dayToInstant(filters.from), to: dayToInstant(filters.to, true), size: 25 }),
    [filters],
  )
  const [open, setOpen] = useState(null)
  const filter = (patch) => setFilters((f) => ({ ...f, ...patch, page: 0 }))

  return (
    <>
      <PageHeader title={t('admin.audit.title')} description={t('admin.audit.description')} />
      {error && <div className="mb-4"><Alert tone="danger">{errorMessage(error, t)}</Alert></div>}
      <Card padded={false}>
        <Toolbar>
          <SearchBox value={filters.search} onChange={(search) => filter({ search })} placeholder={t('admin.audit.search')} />
          <Select value={filters.category} onChange={(e) => filter({ category: e.target.value })} className="sm:w-52" aria-label={t('admin.audit.category')}>
            <option value="">{t('admin.audit.allCategories')}</option>
            {AUDIT_CATEGORIES.map((c) => <option key={c} value={c}>{t(`admin.audit.categories.${c}`)}</option>)}
          </Select>
          <Select value={filters.outcome} onChange={(e) => filter({ outcome: e.target.value })} className="sm:w-36" aria-label={t('admin.audit.outcome')}>
            <option value="">{t('admin.audit.anyOutcome')}</option>
            <option value="SUCCESS">{t('admin.audit.success')}</option>
            <option value="FAILURE">{t('admin.audit.failure')}</option>
          </Select>
          <Input value={filters.actor} onChange={(e) => filter({ actor: e.target.value })} placeholder={t('admin.audit.actor')} className="sm:w-40" />
          <label className="flex items-center gap-2 text-xs text-muted">
            {t('admin.common.from')}
            <Input type="date" value={filters.from} onChange={(e) => filter({ from: e.target.value })} className="sm:w-40" />
          </label>
          <label className="flex items-center gap-2 text-xs text-muted">
            {t('admin.common.to')}
            <Input type="date" value={filters.to} onChange={(e) => filter({ to: e.target.value })} className="sm:w-40" />
          </label>
        </Toolbar>
        <Table
          head={[t('admin.audit.when'), t('admin.audit.category'), t('admin.audit.event'), t('admin.audit.actor'), t('admin.audit.outcome')]}
          loading={loading && !data}
          empty={data && data.content.length === 0 && <EmptyState icon={<ListIcon />} title={t('admin.audit.empty')} />}
        >
          {(data?.content || []).map((e) => (
            <Fragment key={e.id}>
              <tr className="cursor-pointer hover:bg-surface-2/50" onClick={() => setOpen(open === e.id ? null : e.id)} aria-expanded={open === e.id}>
                <Td className="whitespace-nowrap text-muted">{formatDateTime(e.occurredAt, i18n.language)}</Td>
                <Td><Badge tone={auditTone(e)}>{t(`admin.audit.categories.${e.category}`, { defaultValue: e.category })}</Badge></Td>
                <Td>
                  <p className="text-ink">{e.description || e.action}</p>
                  <p className="font-mono text-xs text-subtle">{e.action}</p>
                </Td>
                <Td className="text-muted">
                  {e.actorUuid ? <Link to={`/admin/users/${e.actorUuid}`} onClick={(ev) => ev.stopPropagation()} className="hover:text-primary">{e.actor}</Link> : e.actor || '—'}
                </Td>
                <Td>
                  {e.outcome === 'FAILURE' ? <Badge tone="danger">{t('admin.audit.failure')}</Badge> : <Badge tone="success">{t('admin.audit.success')}</Badge>}
                  {e.status && <span className="ml-1.5 font-mono text-xs text-subtle">{e.status}</span>}
                </Td>
              </tr>
              {open === e.id && (
                <tr className="bg-surface-2/40">
                  <td colSpan={5} className="px-4 py-3">
                    <dl className="grid grid-cols-1 gap-2 text-xs sm:grid-cols-3">
                      <div><dt className="text-subtle">{t('admin.audit.target')}</dt><dd className="break-all font-mono text-ink">{e.targetType} {e.targetId || '—'}</dd></div>
                      <div><dt className="text-subtle">{t('admin.audit.request')}</dt><dd className="break-all font-mono text-ink">{e.httpMethod ? `${e.httpMethod} ${e.path}` : '—'}</dd></div>
                      <div><dt className="text-subtle">{t('sessions.ip')}</dt><dd className="font-mono text-ink">{e.ipAddress || '—'}</dd></div>
                    </dl>
                    {e.details && <pre className="mt-2 max-h-56 overflow-auto rounded-lg bg-surface p-3 font-mono text-xs text-ink">{pretty(e.details)}</pre>}
                  </td>
                </tr>
              )}
            </Fragment>
          ))}
        </Table>
        {data && <Pagination page={data.page} totalPages={data.totalPages} totalElements={data.totalElements} onChange={(page) => setFilters((f) => ({ ...f, page }))} />}
      </Card>
    </>
  )
}
