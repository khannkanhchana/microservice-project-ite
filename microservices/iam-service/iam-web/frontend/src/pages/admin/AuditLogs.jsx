import { Fragment, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { adminApi } from '../../api/admin'
import { Alert, Badge, Button, Card, Dialog, EmptyState, Input, PageHeader, Pagination, Select, Spinner, useToast } from '../../components/ui'
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
  const { id: detailId } = useParams()
  const navigate = useNavigate()
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
                    <div className="mt-3">
                      <Button size="sm" variant="secondary" onClick={() => navigate(`/admin/audit-logs/${e.id}`)}>{t('admin.audit.viewDetails')}</Button>
                    </div>
                  </td>
                </tr>
              )}
            </Fragment>
          ))}
        </Table>
        {data && <Pagination page={data.page} totalPages={data.totalPages} totalElements={data.totalElements} onChange={(page) => setFilters((f) => ({ ...f, page }))} />}
      </Card>
      <AuditLogDialog id={detailId} onClose={() => navigate('/admin/audit-logs')} />
    </>
  )
}

/** One audit log entry, loaded from GET /api/admin/audit-logs/{id}; it has its own URL so it can be shared. */
function AuditLogDialog({ id, onClose }) {
  const { t, i18n } = useTranslation()
  const toast = useToast()
  const { data: e, loading, error } = useResource(() => (id ? adminApi.auditLog(id) : Promise.resolve(null)), [id])
  const copyLink = async () => {
    try {
      await navigator.clipboard.writeText(window.location.href)
      toast?.(t('admin.clients.copied'))
    } catch {
      // clipboard unavailable (e.g. insecure context): nothing to do
    }
  }
  const row = (label, value, mono) => (
    <div className="grid grid-cols-3 gap-3 py-2">
      <dt className="text-subtle">{label}</dt>
      <dd className={`col-span-2 break-all text-ink ${mono ? 'font-mono text-xs' : ''}`}>{value ?? '—'}</dd>
    </div>
  )
  return (
    <Dialog
      open={Boolean(id)}
      onClose={onClose}
      size="lg"
      title={t('admin.audit.detailTitle')}
      footer={
        <>
          <Button variant="secondary" onClick={copyLink} disabled={!e}>{t('admin.audit.copyLink')}</Button>
          <Button onClick={onClose}>{t('admin.common.close')}</Button>
        </>
      }
    >
      {loading && !e && <div className="flex justify-center py-8"><Spinner /></div>}
      {error && <Alert tone="danger">{error.status === 404 ? t('admin.audit.notFound') : errorMessage(error, t)}</Alert>}
      {e && (
        <>
          <div className="mb-3 flex flex-wrap items-center gap-2">
            <Badge tone={auditTone(e)}>{t(`admin.audit.categories.${e.category}`, { defaultValue: e.category })}</Badge>
            {e.outcome === 'FAILURE' ? <Badge tone="danger">{t('admin.audit.failure')}</Badge> : <Badge tone="success">{t('admin.audit.success')}</Badge>}
            <p className="text-sm font-medium text-ink">{e.description || e.action}</p>
          </div>
          <dl className="divide-y divide-line text-sm">
            {row(t('admin.audit.when'), formatDateTime(e.occurredAt, i18n.language))}
            {row(t('admin.audit.event'), e.action, true)}
            {row(t('admin.audit.actor'), e.actorUuid ? <Link to={`/admin/users/${e.actorUuid}`} className="hover:text-primary">{e.actor}</Link> : e.actor)}
            {row(t('admin.audit.target'), [e.targetType, e.targetId].filter(Boolean).join(' ') || null, true)}
            {row(t('admin.audit.request'), e.httpMethod ? `${e.httpMethod} ${e.path}${e.status ? ` → ${e.status}` : ''}` : null, true)}
            {row(t('sessions.ip'), e.ipAddress, true)}
            {row(t('admin.audit.id'), e.id, true)}
          </dl>
          {e.details && <pre className="mt-3 max-h-72 overflow-auto rounded-lg bg-surface-2 p-3 font-mono text-xs text-ink">{pretty(e.details)}</pre>}
        </>
      )}
    </Dialog>
  )
}
