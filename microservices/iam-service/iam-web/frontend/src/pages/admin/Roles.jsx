import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { adminApi } from '../../api/admin'
import { useAuth } from '../../auth/AuthContext'
import { Alert, Badge, Button, Card, ConfirmDialog, Dialog, EmptyState, Field, Input, PageHeader, Textarea, useToast } from '../../components/ui'
import { CheckGroups, LoadingBlock, SearchBox, Table, Td, Toolbar } from '../../components/admin'
import { PlusIcon, TagIcon } from '../../components/icons'
import { errorMessage, formatDateTime } from '../../lib/format'
import { hasPermission } from '../../lib/permissions'
import { useResource } from '../../lib/useResource'

export default function Roles() {
  const { t, i18n } = useTranslation()
  const { user: me } = useAuth()
  const toast = useToast()
  const [search, setSearch] = useState('')
  const { data, loading, error, reload } = useResource(() => adminApi.roles({ search }), [search])
  const [editing, setEditing] = useState(null) // {uuid?, name, description}
  const [matrix, setMatrix] = useState(null) // role
  const [confirm, setConfirm] = useState(null)
  const [actionError, setActionError] = useState(null)
  const can = (p) => hasPermission(me, p)

  const run = (fn, message) => async () => {
    setActionError(null)
    try {
      await fn()
      toast(message)
      reload()
    } catch (err) {
      setActionError(errorMessage(err, t))
    }
  }

  return (
    <>
      <PageHeader
        title={t('admin.roles.title')}
        description={t('admin.roles.description')}
        actions={can('role:create') && (
          <Button onClick={() => setEditing({ name: '', description: '' })}><PlusIcon className="size-4" />{t('admin.roles.new')}</Button>
        )}
      />
      {(error || actionError) && <div className="mb-4"><Alert tone="danger" onDismiss={() => setActionError(null)}>{actionError || errorMessage(error, t)}</Alert></div>}

      <Card padded={false}>
        <Toolbar><SearchBox value={search} onChange={setSearch} placeholder={t('admin.roles.search')} /></Toolbar>
        <Table
          head={[t('admin.roles.name'), t('admin.common.status'), t('admin.users.created'), '']}
          loading={loading && !data}
          empty={data && data.content.length === 0 && <EmptyState icon={<TagIcon />} title={t('admin.roles.empty')} />}
        >
          {(data?.content || []).map((r) => (
            <tr key={r.uuid}>
              <Td>
                <p className="flex items-center gap-2 font-medium text-ink">
                  {r.name}
                  {r.system && <Badge tone="primary">{t('admin.common.system')}</Badge>}
                </p>
                <p className="text-xs text-muted">{r.description || '—'}</p>
              </Td>
              <Td>{r.active ? <Badge tone="success">{t('admin.status.active')}</Badge> : <Badge>{t('admin.status.inactive')}</Badge>}</Td>
              <Td className="whitespace-nowrap text-muted">{formatDateTime(r.createdAt, i18n.language)}</Td>
              <Td>
                <div className="flex flex-wrap justify-end gap-1.5">
                  <Button size="sm" variant="secondary" onClick={() => setMatrix(r)}>{t('admin.roles.permissions')}</Button>
                  {can('role:update') && (
                    <Button size="sm" variant="ghost" onClick={() => setEditing({ uuid: r.uuid, name: r.name, description: r.description || '', system: r.system })}>
                      {t('admin.common.edit')}
                    </Button>
                  )}
                  {can('role:update') && !r.system && (
                    <Button size="sm" variant="ghost" onClick={run(() => adminApi.setRoleActive(r.uuid, !r.active), r.active ? t('admin.roles.deactivated') : t('admin.roles.activated'))}>
                      {r.active ? t('admin.common.deactivate') : t('admin.common.activate')}
                    </Button>
                  )}
                  {can('role:delete') && !r.system && (
                    <Button size="sm" variant="danger-outline" onClick={() => setConfirm(r)}>{t('admin.common.delete')}</Button>
                  )}
                </div>
              </Td>
            </tr>
          ))}
        </Table>
      </Card>

      {editing && <RoleDialog role={editing} onClose={() => setEditing(null)} onSaved={() => { setEditing(null); reload() }} />}
      {matrix && <PermissionMatrix role={matrix} canAssign={can('role:assign')} onClose={() => setMatrix(null)} />}
      <ConfirmDialog
        open={Boolean(confirm)}
        onClose={() => setConfirm(null)}
        onConfirm={async () => { await adminApi.deleteRole(confirm.uuid); toast(t('admin.roles.deleted')); reload() }}
        title={t('admin.roles.deleteTitle')}
        message={t('admin.roles.deleteMessage', { name: confirm?.name })}
        confirmLabel={t('admin.common.delete')}
      />
    </>
  )
}

function RoleDialog({ role, onClose, onSaved }) {
  const { t } = useTranslation()
  const toast = useToast()
  const [form, setForm] = useState(role)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState(null)

  const save = async (event) => {
    event.preventDefault()
    setBusy(true)
    setError(null)
    try {
      if (role.uuid) await adminApi.updateRole(role.uuid, { name: form.name, description: form.description })
      else await adminApi.createRole({ name: form.name, description: form.description, permissionUuids: [] })
      toast(role.uuid ? t('admin.roles.saved') : t('admin.roles.created'))
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
      title={role.uuid ? t('admin.roles.edit') : t('admin.roles.new')}
      footer={<><Button variant="secondary" onClick={onClose} disabled={busy}>{t('common.cancel')}</Button><Button type="submit" form="role-form" loading={busy}>{t('common.save')}</Button></>}
    >
      <form id="role-form" onSubmit={save} className="flex flex-col gap-4 pt-2 text-ink">
        {error && <Alert tone="danger">{error}</Alert>}
        <Field label={t('admin.roles.name')} hint={t('admin.roles.nameHint')}>
          {(p) => <Input {...p} required value={form.name} disabled={role.system} onChange={(e) => setForm((f) => ({ ...f, name: e.target.value.toUpperCase() }))} />}
        </Field>
        <Field label={t('admin.common.descriptionLabel')}>
          {(p) => <Textarea {...p} value={form.description} onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))} />}
        </Field>
      </form>
    </Dialog>
  )
}

/** Shows and edits which permissions a role grants, grouped by resource. */
function PermissionMatrix({ role, canAssign, onClose }) {
  const { t } = useTranslation()
  const toast = useToast()
  const all = useResource(() => adminApi.permissions({ active: true }), [])
  const current = useResource(() => adminApi.rolePermissions(role.uuid), [role.uuid])
  const [selected, setSelected] = useState(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState(null)

  const assigned = (current.data || []).map((p) => p.uuid)
  const chosen = selected ?? assigned
  const toggle = (uuid) => setSelected((s) => {
    const base = s ?? assigned
    return base.includes(uuid) ? base.filter((x) => x !== uuid) : [...base, uuid]
  })
  const lockedSystem = (p) => role.name === 'ADMIN' && p.system

  const save = async () => {
    setBusy(true)
    setError(null)
    try {
      const add = chosen.filter((x) => !assigned.includes(x))
      const remove = assigned.filter((x) => !chosen.includes(x))
      if (add.length) await adminApi.assignRolePermissions(role.uuid, add)
      for (const uuid of remove) await adminApi.revokeRolePermission(role.uuid, uuid)
      toast(t('admin.roles.permissionsSaved', { added: add.length, removed: remove.length }))
      onClose()
    } catch (err) {
      setError(errorMessage(err, t))
      current.reload()
    } finally {
      setBusy(false)
    }
  }

  return (
    <Dialog
      open
      size="xl"
      onClose={onClose}
      title={t('admin.roles.permissionsOf', { name: role.name })}
      footer={
        <>
          <Button variant="secondary" onClick={onClose} disabled={busy}>{canAssign ? t('common.cancel') : t('admin.common.close')}</Button>
          {canAssign && <Button onClick={save} loading={busy}>{t('common.save')}</Button>}
        </>
      }
    >
      <div className="flex flex-col gap-3 pt-2 text-ink">
        <p className="text-muted">{t('admin.roles.matrixHint', { count: chosen.length })}</p>
        {error && <Alert tone="danger">{error}</Alert>}
        {all.loading || current.loading ? <LoadingBlock /> : (
          <CheckGroups
            items={all.data?.content || []}
            groupBy={(p) => p.resource}
            selected={chosen}
            onToggle={toggle}
            disabled={(p) => !canAssign || lockedSystem(p)}
            label={(p) => <><code className="font-mono text-xs">{p.action}</code> <span className="text-muted">{p.description}</span></>}
          />
        )}
      </div>
    </Dialog>
  )
}
