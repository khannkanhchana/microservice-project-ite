import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { adminApi } from '../../api/admin'
import { useAuth } from '../../auth/AuthContext'
import { Alert, Badge, Button, Card, ConfirmDialog, Dialog, EmptyState, Field, Input, PageHeader, Select, Textarea, useToast } from '../../components/ui'
import { LoadingBlock, SearchBox, Toolbar } from '../../components/admin'
import { PlusIcon, ShieldIcon } from '../../components/icons'
import { errorMessage } from '../../lib/format'
import { hasPermission } from '../../lib/permissions'
import { useResource } from '../../lib/useResource'

export default function Permissions() {
  const { t } = useTranslation()
  const { user: me } = useAuth()
  const toast = useToast()
  const [filters, setFilters] = useState({ search: '', resource: '' })
  const { data, loading, error, reload } = useResource(() => adminApi.permissions(filters), [filters])
  const resources = useResource(() => adminApi.permissions(), [])
  const [editing, setEditing] = useState(null)
  const [confirm, setConfirm] = useState(null)
  const [actionError, setActionError] = useState(null)
  const can = (p) => hasPermission(me, p)

  const resourceNames = [...new Set((resources.data?.content || []).map((p) => p.resource))].sort()
  const groups = (data?.content || []).reduce((acc, p) => {
    ;(acc[p.resource] ||= []).push(p)
    return acc
  }, {})

  const toggleActive = (p) => async () => {
    setActionError(null)
    try {
      await adminApi.setPermissionActive(p.uuid, !p.active)
      toast(p.active ? t('admin.permissions.deactivated') : t('admin.permissions.activated'))
      reload()
    } catch (err) {
      setActionError(errorMessage(err, t))
    }
  }

  return (
    <>
      <PageHeader
        title={t('admin.permissions.title')}
        description={t('admin.permissions.description')}
        actions={can('permission:create') && (
          <Button onClick={() => setEditing({ resource: filters.resource, action: '', description: '' })}>
            <PlusIcon className="size-4" />{t('admin.permissions.new')}
          </Button>
        )}
      />
      {(error || actionError) && <div className="mb-4"><Alert tone="danger" onDismiss={() => setActionError(null)}>{actionError || errorMessage(error, t)}</Alert></div>}

      <Card padded={false}>
        <Toolbar>
          <SearchBox value={filters.search} onChange={(search) => setFilters((f) => ({ ...f, search }))} placeholder={t('admin.permissions.search')} />
          <Select value={filters.resource} onChange={(e) => setFilters((f) => ({ ...f, resource: e.target.value }))} className="sm:w-48" aria-label={t('admin.permissions.resource')}>
            <option value="">{t('admin.permissions.allResources')}</option>
            {resourceNames.map((r) => <option key={r} value={r}>{r}</option>)}
          </Select>
        </Toolbar>
        {loading && !data ? <LoadingBlock /> : Object.keys(groups).length === 0 ? (
          <EmptyState icon={<ShieldIcon />} title={t('admin.permissions.empty')} />
        ) : (
          <div className="divide-y divide-line">
            {Object.entries(groups).map(([resource, items]) => (
              <section key={resource} className="px-4 py-4">
                <h2 className="mb-2 text-sm font-semibold text-ink">
                  <code className="font-mono">{resource}</code>
                  <span className="ml-2 text-xs font-normal text-subtle">{t('admin.permissions.count', { count: items.length })}</span>
                </h2>
                <ul className="flex flex-col gap-1.5">
                  {items.map((p) => (
                    <li key={p.uuid} className="flex flex-col gap-2 rounded-lg border border-line px-3 py-2 sm:flex-row sm:items-center">
                      <div className="min-w-0 flex-1">
                        <p className="flex flex-wrap items-center gap-2">
                          <code className="font-mono text-sm text-ink">{p.name}</code>
                          {p.system && <Badge tone="primary">{t('admin.common.system')}</Badge>}
                          {!p.active && <Badge>{t('admin.status.inactive')}</Badge>}
                        </p>
                        {p.description && <p className="text-xs text-muted">{p.description}</p>}
                      </div>
                      <div className="flex gap-1.5">
                        {can('permission:update') && (
                          <Button size="sm" variant="ghost" onClick={() => setEditing({ uuid: p.uuid, resource: p.resource, action: p.action, description: p.description || '', system: p.system })}>
                            {t('admin.common.edit')}
                          </Button>
                        )}
                        {can('permission:update') && !p.system && (
                          <Button size="sm" variant="ghost" onClick={toggleActive(p)}>{p.active ? t('admin.common.deactivate') : t('admin.common.activate')}</Button>
                        )}
                        {can('permission:delete') && !p.system && (
                          <Button size="sm" variant="danger-outline" onClick={() => setConfirm(p)}>{t('admin.common.delete')}</Button>
                        )}
                      </div>
                    </li>
                  ))}
                </ul>
              </section>
            ))}
          </div>
        )}
      </Card>

      {editing && <PermissionDialog permission={editing} resources={resourceNames} onClose={() => setEditing(null)}
        onSaved={() => { setEditing(null); reload(); resources.reload() }} />}
      <ConfirmDialog
        open={Boolean(confirm)}
        onClose={() => setConfirm(null)}
        onConfirm={async () => { await adminApi.deletePermission(confirm.uuid); toast(t('admin.permissions.deleted')); reload() }}
        title={t('admin.permissions.deleteTitle')}
        message={t('admin.permissions.deleteMessage', { name: confirm?.name })}
        confirmLabel={t('admin.common.delete')}
      />
    </>
  )
}

function PermissionDialog({ permission, resources, onClose, onSaved }) {
  const { t } = useTranslation()
  const toast = useToast()
  const [form, setForm] = useState(permission)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState(null)
  const set = (key) => (event) => setForm((f) => ({ ...f, [key]: event.target.value.toLowerCase() }))

  const save = async (event) => {
    event.preventDefault()
    setBusy(true)
    setError(null)
    try {
      const body = { resource: form.resource, action: form.action, description: form.description }
      if (permission.uuid) await adminApi.updatePermission(permission.uuid, body)
      else await adminApi.createPermission(body)
      toast(permission.uuid ? t('admin.permissions.saved') : t('admin.permissions.created'))
      onSaved()
    } catch (err) {
      setError(errorMessage(err, t))
    } finally {
      setBusy(false)
    }
  }

  return (
    <Dialog
      open
      onClose={onClose}
      title={permission.uuid ? t('admin.permissions.edit') : t('admin.permissions.new')}
      footer={<><Button variant="secondary" onClick={onClose} disabled={busy}>{t('common.cancel')}</Button><Button type="submit" form="permission-form" loading={busy}>{t('common.save')}</Button></>}
    >
      <form id="permission-form" onSubmit={save} className="flex flex-col gap-4 pt-2 text-ink">
        <p className="text-muted">{t('admin.permissions.formHint')}</p>
        {error && <Alert tone="danger">{error}</Alert>}
        <div className="grid grid-cols-2 gap-3">
          <Field label={t('admin.permissions.resource')}>
            {(p) => (
              <>
                <Input {...p} list="permission-resources" required value={form.resource} onChange={set('resource')} disabled={permission.system} placeholder="report" />
                <datalist id="permission-resources">{resources.map((r) => <option key={r} value={r} />)}</datalist>
              </>
            )}
          </Field>
          <Field label={t('admin.permissions.action')}>
            {(p) => <Input {...p} required value={form.action} onChange={set('action')} disabled={permission.system} placeholder="read" />}
          </Field>
        </div>
        <p className="text-xs text-muted">
          {t('admin.permissions.preview')}: <code className="font-mono text-ink">{(form.resource || 'resource') + ':' + (form.action || 'action')}</code>
        </p>
        <Field label={t('admin.common.descriptionLabel')}>
          {(p) => <Textarea {...p} value={form.description} onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))} />}
        </Field>
      </form>
    </Dialog>
  )
}
