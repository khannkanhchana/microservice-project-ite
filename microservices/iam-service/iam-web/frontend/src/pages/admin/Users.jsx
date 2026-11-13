import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { adminApi } from '../../api/admin'
import { useAuth } from '../../auth/AuthContext'
import { Alert, Avatar, Badge, Button, Card, Dialog, EmptyState, Field, Input, Pagination, PageHeader, PasswordInput, Select, useToast } from '../../components/ui'
import { SearchBox, Table, Td, Toolbar, UserStatus } from '../../components/admin'
import { PlusIcon, UsersIcon } from '../../components/icons'
import { errorMessage, formatDateTime, relativeTime } from '../../lib/format'
import { hasPermission } from '../../lib/permissions'
import { useResource } from '../../lib/useResource'

const EMPTY = { username: '', email: '', givenName: '', familyName: '', password: '', locale: 'en', roleUuids: [] }

export default function Users() {
  const { t, i18n } = useTranslation()
  const { user: me } = useAuth()
  const toast = useToast()
  const navigate = useNavigate()
  const [filters, setFilters] = useState({ search: '', status: 'all', role: '', page: 0 })
  const { data, loading, error } = useResource(() => adminApi.users({ ...filters, size: 20 }), [filters])
  const roles = useResource(() => adminApi.roles({ active: true }), [])
  const [creating, setCreating] = useState(false)
  const [form, setForm] = useState(EMPTY)
  const [formError, setFormError] = useState(null)
  const [busy, setBusy] = useState(false)

  const set = (key) => (event) => setForm((f) => ({ ...f, [key]: event.target.value }))
  const filter = (patch) => setFilters((f) => ({ ...f, ...patch, page: 0 }))

  const create = async (event) => {
    event.preventDefault()
    setBusy(true)
    setFormError(null)
    try {
      const created = await adminApi.createUser(form)
      toast(t('admin.users.createdToast', { name: created.username }))
      setCreating(false)
      navigate(`/admin/users/${created.uuid}`)
    } catch (err) {
      setFormError(errorMessage(err, t))
    } finally {
      setBusy(false)
    }
  }

  return (
    <>
      <PageHeader
        title={t('admin.users.title')}
        description={t('admin.users.description')}
        actions={
          hasPermission(me, 'user:create') && (
            <Button onClick={() => { setForm(EMPTY); setFormError(null); setCreating(true) }}>
              <PlusIcon className="size-4" />
              {t('admin.users.new')}
            </Button>
          )
        }
      />
      {error && <div className="mb-4"><Alert tone="danger">{errorMessage(error, t)}</Alert></div>}

      <Card padded={false}>
        <Toolbar>
          <SearchBox value={filters.search} onChange={(search) => filter({ search })} placeholder={t('admin.users.search')} />
          <Select value={filters.status} onChange={(e) => filter({ status: e.target.value })} className="sm:w-40" aria-label={t('admin.common.status')}>
            {['all', 'active', 'disabled', 'locked'].map((s) => <option key={s} value={s}>{t(`admin.status.${s}`)}</option>)}
          </Select>
          <Select value={filters.role} onChange={(e) => filter({ role: e.target.value })} className="sm:w-48" aria-label={t('admin.users.role')}>
            <option value="">{t('admin.users.allRoles')}</option>
            {(roles.data?.content || []).map((r) => <option key={r.uuid} value={r.uuid}>{r.name}</option>)}
          </Select>
        </Toolbar>
        <Table
          head={[t('admin.users.user'), t('admin.common.status'), t('admin.users.roles'), t('admin.users.lastLogin'), t('admin.users.created')]}
          loading={loading && !data}
          empty={data && data.content.length === 0 && <EmptyState icon={<UsersIcon />} title={t('admin.users.empty')} />}
        >
          {(data?.content || []).map((u) => (
            <tr key={u.uuid} className="hover:bg-surface-2/50">
              <Td>
                <Link to={`/admin/users/${u.uuid}`} className="flex items-center gap-3">
                  <Avatar src={u.pictureUrl} name={u.name || u.username} size="sm" />
                  <span className="min-w-0">
                    <span className="block truncate font-medium text-ink hover:text-primary">{u.name || u.username}</span>
                    <span className="block truncate text-xs text-muted">@{u.username} · {u.email}</span>
                  </span>
                </Link>
              </Td>
              <Td>
                <UserStatus user={u} />
                {u.failedLoginAttempts > 0 && (
                  <span className="ml-1.5 text-xs text-warning">{t('admin.users.failedCount', { count: u.failedLoginAttempts })}</span>
                )}
              </Td>
              <Td>
                <div className="flex flex-wrap gap-1">
                  {u.roles.length ? u.roles.map((r) => <Badge key={r} tone="primary">{r}</Badge>) : <span className="text-subtle">—</span>}
                </div>
              </Td>
              <Td className="whitespace-nowrap text-muted">{u.lastLoginAt ? relativeTime(u.lastLoginAt, i18n.language) : t('common.never')}</Td>
              <Td className="whitespace-nowrap text-muted">{formatDateTime(u.createdAt, i18n.language)}</Td>
            </tr>
          ))}
        </Table>
        {data && <Pagination page={data.page} totalPages={data.totalPages} totalElements={data.totalElements} onChange={(page) => setFilters((f) => ({ ...f, page }))} />}
      </Card>

      <Dialog
        open={creating}
        onClose={() => setCreating(false)}
        title={t('admin.users.new')}
        size="lg"
        footer={
          <>
            <Button variant="secondary" onClick={() => setCreating(false)} disabled={busy}>{t('common.cancel')}</Button>
            <Button type="submit" form="create-user" loading={busy}>{t('admin.users.create')}</Button>
          </>
        }
      >
        <form id="create-user" onSubmit={create} className="flex flex-col gap-4 pt-2 text-ink">
          {formError && <Alert tone="danger">{formError}</Alert>}
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <Field label={t('profile.givenName')}>{(p) => <Input {...p} required value={form.givenName} onChange={set('givenName')} />}</Field>
            <Field label={t('profile.familyName')}>{(p) => <Input {...p} required value={form.familyName} onChange={set('familyName')} />}</Field>
            <Field label={t('profile.username')}>{(p) => <Input {...p} required value={form.username} onChange={set('username')} autoComplete="off" />}</Field>
            <Field label={t('profile.email')}>{(p) => <Input {...p} type="email" required value={form.email} onChange={set('email')} />}</Field>
            <Field label={t('admin.users.initialPassword')} hint={t('admin.users.passwordHint')}>
              {(p) => <PasswordInput {...p} required value={form.password} onChange={set('password')} autoComplete="new-password" />}
            </Field>
            <Field label={t('profile.locale')}>
              {(p) => (
                <Select {...p} value={form.locale} onChange={set('locale')}>
                  <option value="en">English</option>
                  <option value="km">ភាសាខ្មែរ</option>
                </Select>
              )}
            </Field>
          </div>
          <fieldset>
            <legend className="mb-2 text-sm font-medium text-ink">{t('admin.users.roles')}</legend>
            <div className="flex flex-wrap gap-2">
              {(roles.data?.content || []).map((r) => {
                const on = form.roleUuids.includes(r.uuid)
                return (
                  <button
                    key={r.uuid}
                    type="button"
                    aria-pressed={on}
                    onClick={() => setForm((f) => ({ ...f, roleUuids: on ? f.roleUuids.filter((x) => x !== r.uuid) : [...f.roleUuids, r.uuid] }))}
                    className={'rounded-full border px-3 py-1 text-sm ' + (on ? 'border-primary bg-primary-soft text-primary' : 'border-line text-muted hover:bg-surface-2')}
                  >
                    {r.name}
                  </button>
                )
              })}
            </div>
          </fieldset>
        </form>
      </Dialog>
    </>
  )
}
