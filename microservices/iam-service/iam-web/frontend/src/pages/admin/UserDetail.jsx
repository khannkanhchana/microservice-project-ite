import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { adminApi } from '../../api/admin'
import { useAuth } from '../../auth/AuthContext'
import {
  Alert, Avatar, Badge, Button, Card, ConfirmDialog, Dialog, EmptyState, Field, Input, PasswordInput, Select, useToast,
} from '../../components/ui'
import { LoadingBlock, Table, Td, Tabs, UserStatus } from '../../components/admin'
import { DevicesIcon, HistoryIcon, ProviderIcon, ShieldIcon, TagIcon } from '../../components/icons'
import { DEVICE_ICONS, deviceLabel } from '../../lib/devices'
import { errorMessage, formatDateTime, relativeTime } from '../../lib/format'
import { hasPermission } from '../../lib/permissions'
import { useResource } from '../../lib/useResource'

export default function UserDetail() {
  const { uuid } = useParams()
  const { t } = useTranslation()
  const { user: me } = useAuth()
  const navigate = useNavigate()
  const toast = useToast()
  const user = useResource(() => adminApi.user(uuid), [uuid])
  const security = useResource(() => adminApi.security(uuid), [uuid])
  const [tab, setTab] = useState('profile')
  const [confirm, setConfirm] = useState(null)
  const [error, setError] = useState(null)

  const u = user.data
  const sec = security.data
  const self = me?.uuid === uuid
  const can = (p) => hasPermission(me, p)

  const reloadAll = () => {
    user.reload()
    security.reload()
  }

  const act = (fn, message) => async () => {
    setError(null)
    try {
      await fn()
      if (message) toast(message)
      reloadAll()
    } catch (err) {
      setError(errorMessage(err, t))
    }
  }

  if (user.loading && !u) return <LoadingBlock />
  if (user.error) return <Alert tone="danger">{errorMessage(user.error, t)}</Alert>
  if (!u) return null

  const actions = {
    disable: { title: t('admin.users.deactivateTitle'), message: t('admin.users.deactivateMessage', { name: u.username }), label: t('admin.users.deactivate'),
      run: act(() => adminApi.setUserStatus(uuid, 'disable'), t('admin.users.deactivated')) },
    lock: { title: t('admin.users.lockTitle'), message: t('admin.users.lockMessage', { name: u.username }), label: t('admin.users.lock'),
      run: act(() => adminApi.setUserStatus(uuid, 'lock'), t('admin.users.locked')) },
    delete: { title: t('admin.users.deleteTitle'), message: t('admin.users.deleteMessage', { name: u.username }), label: t('admin.common.delete'),
      run: async () => {
        try {
          await adminApi.deleteUser(uuid)
          toast(t('admin.users.deleted'))
          navigate('/admin/users')
        } catch (err) {
          setError(errorMessage(err, t))
        }
      } },
  }
  const dialog = confirm && actions[confirm]

  return (
    <>
      <Link to="/admin/users" className="mb-3 inline-block text-sm text-muted hover:text-ink">← {t('admin.users.back')}</Link>
      <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex items-center gap-4">
          <Avatar src={u.pictureUrl} name={u.name || u.username} size="lg" />
          <div className="min-w-0">
            <h1 className="flex flex-wrap items-center gap-2 text-2xl font-semibold text-ink">
              {u.name || u.username}
              {sec && <UserStatus user={{ enabled: sec.enabled, locked: sec.locked }} />}
              {self && <Badge tone="primary">{t('admin.users.you')}</Badge>}
            </h1>
            <p className="text-sm text-muted">@{u.username} · {u.email}</p>
          </div>
        </div>
        {can('user:update') && sec && (
          <div className="flex flex-wrap gap-2">
            {sec.enabled ? (
              <Button variant="secondary" disabled={self} onClick={() => setConfirm('disable')}>{t('admin.users.deactivate')}</Button>
            ) : (
              <Button variant="secondary" onClick={act(() => adminApi.setUserStatus(uuid, 'enable'), t('admin.users.activated'))}>{t('admin.users.activate')}</Button>
            )}
            {sec.locked ? (
              <Button variant="secondary" onClick={act(() => adminApi.unlockUser(uuid), t('admin.users.unlocked'))}>{t('admin.users.unlock')}</Button>
            ) : (
              <Button variant="secondary" disabled={self} onClick={() => setConfirm('lock')}>{t('admin.users.lock')}</Button>
            )}
            {can('user:delete') && (
              <Button variant="danger-outline" disabled={self} onClick={() => setConfirm('delete')}>{t('admin.common.delete')}</Button>
            )}
          </div>
        )}
      </div>
      {error && <div className="mb-4"><Alert tone="danger" onDismiss={() => setError(null)}>{error}</Alert></div>}

      <Tabs
        active={tab}
        onChange={setTab}
        tabs={[
          { id: 'profile', label: t('admin.users.tabProfile') },
          { id: 'access', label: t('admin.users.tabAccess'), count: u.roles.length },
          ...(can('session:read') ? [{ id: 'sessions', label: t('admin.users.tabSessions'), count: sec?.activeSessions }, { id: 'logins', label: t('admin.users.tabLogins') }] : []),
          { id: 'security', label: t('admin.users.tabSecurity') },
        ]}
      />
      {tab === 'profile' && <ProfileTab user={u} canEdit={can('user:update')} onSaved={reloadAll} />}
      {tab === 'access' && <AccessTab uuid={uuid} canAssign={can('user:assign')} onChanged={reloadAll} />}
      {tab === 'sessions' && <SessionsTab uuid={uuid} canRevoke={can('session:revoke')} onChanged={security.reload} />}
      {tab === 'logins' && <LoginsTab uuid={uuid} />}
      {tab === 'security' && sec && <SecurityTab uuid={uuid} sec={sec} canEdit={can('user:update')} onChanged={reloadAll} />}

      <ConfirmDialog open={Boolean(dialog)} onClose={() => setConfirm(null)} onConfirm={dialog?.run || (() => {})}
        title={dialog?.title} message={dialog?.message} confirmLabel={dialog?.label} />
    </>
  )
}

function ProfileTab({ user, canEdit, onSaved }) {
  const { t } = useTranslation()
  const toast = useToast()
  const initial = {
    givenName: user.givenName || '', familyName: user.familyName || '', username: user.username || '',
    email: user.email || '', phoneNumber: user.phoneNumber || '', locale: user.locale || 'en',
  }
  const [form, setForm] = useState(initial)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState(null)
  const set = (key) => (event) => setForm((f) => ({ ...f, [key]: event.target.value }))

  const save = async (event) => {
    event.preventDefault()
    setBusy(true)
    setError(null)
    try {
      const changes = Object.fromEntries(Object.entries(form).filter(([k, v]) => v.trim() !== (initial[k] || '')))
      if (Object.keys(changes).length) await adminApi.updateUser(user.uuid, changes)
      toast(t('admin.users.saved'))
      onSaved()
    } catch (err) {
      setError(errorMessage(err, t))
    } finally {
      setBusy(false)
    }
  }

  return (
    <Card title={t('admin.users.tabProfile')}>
      <form onSubmit={save} className="flex flex-col gap-4">
        {error && <Alert tone="danger">{error}</Alert>}
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <Field label={t('profile.givenName')}>{(p) => <Input {...p} value={form.givenName} onChange={set('givenName')} disabled={!canEdit} />}</Field>
          <Field label={t('profile.familyName')}>{(p) => <Input {...p} value={form.familyName} onChange={set('familyName')} disabled={!canEdit} />}</Field>
          <Field label={t('profile.username')}>{(p) => <Input {...p} value={form.username} onChange={set('username')} disabled={!canEdit} />}</Field>
          <Field label={t('profile.email')}>{(p) => <Input {...p} type="email" value={form.email} onChange={set('email')} disabled={!canEdit} />}</Field>
          <Field label={t('profile.phone')} hint={t('profile.phoneHint')}>{(p) => <Input {...p} value={form.phoneNumber} onChange={set('phoneNumber')} disabled={!canEdit} />}</Field>
          <Field label={t('profile.locale')}>
            {(p) => (
              <Select {...p} value={form.locale} onChange={set('locale')} disabled={!canEdit}>
                <option value="en">English</option>
                <option value="km">ភាសាខ្មែរ</option>
              </Select>
            )}
          </Field>
        </div>
        <dl className="grid grid-cols-1 gap-2 rounded-lg bg-surface-2/60 p-3 text-xs text-muted sm:grid-cols-3">
          <div><dt className="text-subtle">{t('admin.users.created')}</dt><dd>{formatDateTime(user.createdAt)}</dd></div>
          <div><dt className="text-subtle">{t('admin.users.updated')}</dt><dd>{formatDateTime(user.updatedAt)}</dd></div>
          <div><dt className="text-subtle">UUID</dt><dd className="truncate font-mono">{user.uuid}</dd></div>
        </dl>
        {canEdit && (
          <div className="flex justify-end">
            <Button type="submit" loading={busy}>{t('common.save')}</Button>
          </div>
        )}
      </form>
    </Card>
  )
}

function AccessTab({ uuid, canAssign, onChanged }) {
  const { t } = useTranslation()
  const toast = useToast()
  const auth = useResource(() => adminApi.authorities(uuid), [uuid])
  const roles = useResource(() => adminApi.roles({ active: true }), [])
  const permissions = useResource(() => adminApi.permissions({ active: true }), [])
  const [addRole, setAddRole] = useState('')
  const [grant, setGrant] = useState({ permission: '', effect: 'GRANT' })
  const [error, setError] = useState(null)

  const run = (fn, message) => async () => {
    setError(null)
    try {
      await fn()
      toast(message)
      auth.reload()
      onChanged()
    } catch (err) {
      setError(errorMessage(err, t))
    }
  }

  if (auth.loading && !auth.data) return <LoadingBlock />
  const a = auth.data
  if (!a) return auth.error ? <Alert tone="danger">{errorMessage(auth.error, t)}</Alert> : null
  const assigned = new Set(a.roles.map((r) => r.uuid))
  const available = (roles.data?.content || []).filter((r) => !assigned.has(r.uuid))

  return (
    <div className="flex flex-col gap-6">
      {error && <Alert tone="danger" onDismiss={() => setError(null)}>{error}</Alert>}
      <Card
        title={t('admin.users.assignedRoles')}
        description={t('admin.users.assignedRolesHint')}
        actions={canAssign && available.length > 0 && (
          <div className="flex gap-2">
            <Select value={addRole} onChange={(e) => setAddRole(e.target.value)} className="h-8 w-44 py-0" aria-label={t('admin.users.addRole')}>
              <option value="">{t('admin.users.chooseRole')}</option>
              {available.map((r) => <option key={r.uuid} value={r.uuid}>{r.name}</option>)}
            </Select>
            <Button size="sm" disabled={!addRole} onClick={run(async () => { await adminApi.assignRoles(uuid, [addRole]); setAddRole('') }, t('admin.users.roleAssigned'))}>
              {t('admin.common.add')}
            </Button>
          </div>
        )}
      >
        {a.roles.length === 0 ? (
          <p className="text-sm text-muted">{t('admin.users.noRoles')}</p>
        ) : (
          <ul className="flex flex-wrap gap-2">
            {a.roles.map((r) => (
              <li key={r.uuid} className="flex items-center gap-2 rounded-lg border border-line py-1.5 pl-3 pr-1.5 text-sm">
                <TagIcon className="size-4 text-primary" />
                <span className="font-medium text-ink">{r.name}</span>
                {r.expiresAt && <span className="text-xs text-subtle">{t('admin.users.until', { date: formatDateTime(r.expiresAt) })}</span>}
                {canAssign && (
                  <button type="button" onClick={run(() => adminApi.revokeRole(uuid, r.uuid), t('admin.users.roleRevoked'))}
                    className="rounded px-1.5 py-0.5 text-xs text-danger hover:bg-danger-soft">
                    {t('admin.common.revoke')}
                  </button>
                )}
              </li>
            ))}
          </ul>
        )}
      </Card>

      <Card
        title={t('admin.users.directPermissions')}
        description={t('admin.users.directPermissionsHint')}
        actions={canAssign && (
          <div className="flex flex-wrap gap-2">
            <Select value={grant.permission} onChange={(e) => setGrant((g) => ({ ...g, permission: e.target.value }))} className="h-8 w-52 py-0" aria-label={t('admin.users.permission')}>
              <option value="">{t('admin.users.choosePermission')}</option>
              {(permissions.data?.content || []).map((p) => <option key={p.uuid} value={p.uuid}>{p.name}</option>)}
            </Select>
            <Select value={grant.effect} onChange={(e) => setGrant((g) => ({ ...g, effect: e.target.value }))} className="h-8 w-28 py-0" aria-label={t('admin.users.effect')}>
              <option value="GRANT">{t('admin.users.grant')}</option>
              <option value="DENY">{t('admin.users.deny')}</option>
            </Select>
            <Button size="sm" disabled={!grant.permission}
              onClick={run(async () => { await adminApi.grantPermissions(uuid, [grant.permission], grant.effect); setGrant({ permission: '', effect: 'GRANT' }) }, t('admin.users.permissionGranted'))}>
              {t('admin.common.add')}
            </Button>
          </div>
        )}
      >
        {a.directPermissions.length === 0 ? (
          <p className="text-sm text-muted">{t('admin.users.noDirect')}</p>
        ) : (
          <ul className="flex flex-col divide-y divide-line">
            {a.directPermissions.map((p) => (
              <li key={p.uuid} className="flex items-center justify-between gap-3 py-2 text-sm">
                <span className="flex items-center gap-2">
                  <Badge tone={p.effect === 'DENY' ? 'danger' : 'success'}>{p.effect === 'DENY' ? t('admin.users.deny') : t('admin.users.grant')}</Badge>
                  <code className="font-mono text-ink">{p.name}</code>
                </span>
                {canAssign && (
                  <button type="button" onClick={run(() => adminApi.revokePermission(uuid, p.uuid), t('admin.users.permissionRevoked'))}
                    className="rounded px-2 py-1 text-xs text-danger hover:bg-danger-soft">
                    {t('admin.common.revoke')}
                  </button>
                )}
              </li>
            ))}
          </ul>
        )}
      </Card>

      <Card title={t('admin.users.effective')} description={t('admin.users.effectiveHint')}>
        {a.effectivePermissions.length === 0 ? (
          <p className="text-sm text-muted">{t('admin.users.noEffective')}</p>
        ) : (
          <div className="flex flex-wrap gap-1.5">
            {[...a.effectivePermissions].sort().map((p) => <code key={p} className="rounded bg-surface-2 px-2 py-0.5 font-mono text-xs text-ink">{p}</code>)}
          </div>
        )}
      </Card>
    </div>
  )
}

function SessionsTab({ uuid, canRevoke, onChanged }) {
  const { t, i18n } = useTranslation()
  const toast = useToast()
  const { data, loading, error, reload } = useResource(() => adminApi.userSessions(uuid), [uuid])
  const [confirm, setConfirm] = useState(null)

  const terminate = async () => {
    if (confirm === 'all') {
      const { revoked } = await adminApi.revokeUserSessions(uuid)
      toast(t('admin.sessions.allTerminated', { count: revoked }))
    } else {
      await adminApi.revokeUserSession(uuid, confirm.id)
      toast(t('admin.sessions.terminated'))
    }
    reload()
    onChanged()
  }

  return (
    <Card
      padded={false}
      title={t('admin.sessions.title', { count: data?.length ?? 0 })}
      description={t('admin.sessions.hint')}
      actions={canRevoke && data?.length > 0 && <Button variant="danger" size="sm" onClick={() => setConfirm('all')}>{t('admin.sessions.terminateAll')}</Button>}
    >
      {error && <div className="p-4"><Alert tone="danger">{errorMessage(error, t)}</Alert></div>}
      {loading && !data ? <LoadingBlock /> : data?.length === 0 ? (
        <EmptyState icon={<DevicesIcon />} title={t('admin.sessions.empty')} />
      ) : (
        <ul className="divide-y divide-line">
          {(data || []).map((s) => {
            const Icon = DEVICE_ICONS[s.deviceType] || DevicesIcon
            return (
              <li key={s.id} className="flex flex-col gap-3 px-5 py-4 sm:flex-row sm:items-center">
                <span className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-surface-2 text-muted"><Icon /></span>
                <div className="min-w-0 flex-1 text-sm">
                  <p className="font-medium text-ink">{deviceLabel(t, s)}</p>
                  <p className="text-muted">
                    {t('sessions.lastActive', { when: relativeTime(s.lastActivityAt, i18n.language) })} · {t(`methods.${s.loginMethod}`, { defaultValue: s.loginMethod })}
                  </p>
                  <p className="text-xs text-subtle">
                    <span className="font-mono">{s.ipAddress || '—'}</span> · {s.location || t('common.unknown')} · {t('sessions.signedIn', { date: formatDateTime(s.createdAt, i18n.language) })}
                  </p>
                </div>
                {canRevoke && <Button variant="danger-outline" size="sm" onClick={() => setConfirm(s)}>{t('admin.sessions.terminate')}</Button>}
              </li>
            )
          })}
        </ul>
      )}
      <ConfirmDialog open={Boolean(confirm)} onClose={() => setConfirm(null)} onConfirm={terminate}
        title={confirm === 'all' ? t('admin.sessions.terminateAll') : t('admin.sessions.terminate')}
        message={confirm === 'all' ? t('admin.sessions.terminateAllMessage') : t('admin.sessions.terminateMessage')}
        confirmLabel={t('admin.sessions.terminate')} />
    </Card>
  )
}

function LoginsTab({ uuid }) {
  const { t, i18n } = useTranslation()
  const [page, setPage] = useState(0)
  const { data, loading } = useResource(() => adminApi.loginHistory({ user: uuid, page, size: 15 }), [uuid, page])
  return (
    <Card padded={false}>
      <LoginTable data={data} loading={loading} t={t} lang={i18n.language} showUser={false} />
      {data && data.totalPages > 1 && (
        <div className="flex justify-end gap-2 border-t border-line px-4 py-3">
          <Button size="sm" variant="secondary" disabled={page === 0} onClick={() => setPage(page - 1)}>{t('common.previous')}</Button>
          <Button size="sm" variant="secondary" disabled={page + 1 >= data.totalPages} onClick={() => setPage(page + 1)}>{t('common.next')}</Button>
        </div>
      )}
    </Card>
  )
}

/** Login attempts table, shared with the login monitor page. */
export function LoginTable({ data, loading, t, lang, showUser = true }) {
  return (
    <Table
      head={[...(showUser ? [t('admin.users.user')] : []), t('admin.logins.result'), t('admin.logins.method'), t('admin.logins.device'), t('sessions.ip'), t('sessions.location'), t('admin.logins.when')]}
      loading={loading && !data}
      empty={data && data.content.length === 0 && <EmptyState icon={<HistoryIcon />} title={t('admin.logins.empty')} />}
    >
      {(data?.content || []).map((l) => (
        <tr key={l.id}>
          {showUser && (
            <Td>
              {l.userUuid ? <Link className="font-medium text-ink hover:text-primary" to={`/admin/users/${l.userUuid}`}>{l.username || l.identifier}</Link>
                : <span className="text-muted">{l.identifier || '—'}</span>}
            </Td>
          )}
          <Td>
            {l.success ? <Badge tone="success">{t('admin.logins.success')}</Badge> : <Badge tone="danger">{t('admin.logins.failure')}</Badge>}
            {!l.success && l.failureReason && (
              <span className="ml-1.5 text-xs text-muted">{t(`failure.${l.failureReason.split(':')[0]}`, { defaultValue: l.failureReason })}</span>
            )}
          </Td>
          <Td className="text-muted">{t(`methods.${l.method}`, { defaultValue: l.method })}</Td>
          <Td className="text-muted">{deviceLabel(t, l)}</Td>
          <Td className="font-mono text-xs text-muted">{l.ipAddress || '—'}</Td>
          <Td className="text-muted">{l.location || t('common.unknown')}</Td>
          <Td className="whitespace-nowrap text-muted">{formatDateTime(l.attemptedAt, lang)}</Td>
        </tr>
      ))}
    </Table>
  )
}

function SecurityTab({ uuid, sec, canEdit, onChanged }) {
  const { t, i18n } = useTranslation()
  const toast = useToast()
  const identities = useResource(() => adminApi.identities(uuid), [uuid])
  const [password, setPassword] = useState(null)
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(false)
  useEffect(() => setError(null), [password])

  const run = (fn, message) => async () => {
    setError(null)
    try {
      await fn()
      toast(message)
      onChanged()
      identities.reload()
    } catch (err) {
      setError(errorMessage(err, t))
    }
  }

  const savePassword = async (event) => {
    event.preventDefault()
    setBusy(true)
    try {
      await adminApi.setPassword(uuid, password)
      toast(t('admin.security.passwordSet'))
      setPassword(null)
      onChanged()
    } catch (err) {
      setError(errorMessage(err, t))
    } finally {
      setBusy(false)
    }
  }

  const rows = [
    [t('admin.common.status'), <UserStatus key="s" user={sec} />],
    [t('admin.security.lockedUntil'), sec.locked ? (sec.lockExpiresAt ? formatDateTime(sec.lockExpiresAt, i18n.language) : t('admin.security.untilUnlocked')) : '—'],
    [t('admin.security.failedAttempts'), `${sec.failedLoginAttempts} · ${t('admin.security.failed24h', { count: sec.failedLogins24h })}`],
    [t('admin.security.password'), sec.hasPassword ? t('admin.security.passwordChanged', { when: formatDateTime(sec.passwordChangedAt, i18n.language) }) : t('admin.security.noPassword')],
    [t('admin.users.lastLogin'), sec.lastLoginAt ? formatDateTime(sec.lastLoginAt, i18n.language) : t('common.never')],
    [t('admin.security.activeSessions'), sec.activeSessions],
  ]

  return (
    <div className="flex flex-col gap-6">
      {error && !password && <Alert tone="danger" onDismiss={() => setError(null)}>{error}</Alert>}
      <Card title={t('admin.security.title')}>
        <dl className="grid grid-cols-1 gap-x-6 gap-y-3 text-sm sm:grid-cols-2">
          {rows.map(([label, value]) => (
            <div key={label} className="flex justify-between gap-3 border-b border-line pb-2">
              <dt className="text-muted">{label}</dt>
              <dd className="text-right text-ink">{value}</dd>
            </div>
          ))}
        </dl>
        {canEdit && (
          <div className="mt-5 flex flex-wrap gap-2">
            {(sec.locked || sec.failedLoginAttempts > 0) && (
              <Button variant="secondary" onClick={run(() => adminApi.unlockUser(uuid), t('admin.users.unlocked'))}>{t('admin.security.unlockReset')}</Button>
            )}
            <Button variant="secondary" onClick={() => setPassword('')}>{t('admin.security.setPassword')}</Button>
            <Button variant="secondary" onClick={run(() => adminApi.sendResetLink(uuid), t('admin.security.linkSent'))}>{t('admin.security.sendLink')}</Button>
          </div>
        )}
      </Card>

      <Card title={t('admin.security.linked')} padded={false}>
        {identities.loading && !identities.data ? <LoadingBlock /> : (identities.data || []).length === 0 ? (
          <EmptyState icon={<ShieldIcon />} title={t('admin.security.noLinked')} />
        ) : (
          <ul className="divide-y divide-line">
            {identities.data.map((i) => (
              <li key={i.uuid} className="flex items-center gap-3 px-5 py-3 text-sm">
                <ProviderIcon provider={i.provider.toLowerCase()} />
                <div className="min-w-0 flex-1">
                  <p className="font-medium text-ink">{i.provider}</p>
                  <p className="truncate text-xs text-muted">{i.email || i.username} · {t('admin.security.linkedAt', { when: formatDateTime(i.linkedAt, i18n.language) })}</p>
                </div>
                {canEdit && (
                  <Button size="sm" variant="danger-outline" onClick={run(() => adminApi.unlinkIdentity(uuid, i.uuid), t('admin.security.unlinked'))}>
                    {t('linked.unlink')}
                  </Button>
                )}
              </li>
            ))}
          </ul>
        )}
      </Card>

      <Dialog
        open={password !== null}
        onClose={() => setPassword(null)}
        title={t('admin.security.setPassword')}
        footer={
          <>
            <Button variant="secondary" onClick={() => setPassword(null)} disabled={busy}>{t('common.cancel')}</Button>
            <Button type="submit" form="set-password" loading={busy}>{t('admin.security.setPassword')}</Button>
          </>
        }
      >
        <form id="set-password" onSubmit={savePassword} className="flex flex-col gap-3 pt-2 text-ink">
          <p className="text-muted">{t('admin.security.setPasswordHint')}</p>
          {error && <Alert tone="danger">{error}</Alert>}
          <Field label={t('password.new')}>{(p) => <PasswordInput {...p} required value={password || ''} onChange={(e) => setPassword(e.target.value)} autoComplete="new-password" />}</Field>
        </form>
      </Dialog>
    </div>
  )
}
