import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { adminApi } from '../../api/admin'
import { useAuth } from '../../auth/AuthContext'
import { Alert, Badge, Button, Card, Checkbox, ConfirmDialog, Dialog, EmptyState, Field, Input, PageHeader, useToast } from '../../components/ui'
import { ListInput, LoadingBlock, SearchBox, Toolbar } from '../../components/admin'
import { AppIcon, PlusIcon } from '../../components/icons'
import { errorMessage, formatDateTime } from '../../lib/format'
import { hasPermission } from '../../lib/permissions'
import { useResource } from '../../lib/useResource'

const GRANTS = ['authorization_code', 'refresh_token', 'client_credentials']
const STANDARD_SCOPES = ['openid', 'profile', 'email', 'phone', 'address']
const NEW_CLIENT = {
  clientId: '', clientName: '', type: 'confidential', authMethod: 'client_secret_basic',
  grantTypes: ['authorization_code', 'refresh_token'], redirectUris: [], postLogoutRedirectUris: [],
  scopes: ['openid', 'profile'], requireProofKey: true, requireConsent: true,
  accessTokenMinutes: '', refreshTokenDays: '', reuseRefreshTokens: false,
}

const toForm = (c) => ({
  ...c,
  type: c.confidential ? 'confidential' : 'public',
  authMethod: c.authenticationMethods.find((m) => m !== 'none') || 'client_secret_basic',
  accessTokenMinutes: String(c.accessTokenMinutes),
  refreshTokenDays: String(c.refreshTokenDays),
})

export default function Clients() {
  const { t, i18n } = useTranslation()
  const { user: me } = useAuth()
  const toast = useToast()
  const [search, setSearch] = useState('')
  const { data, loading, error, reload } = useResource(() => adminApi.clients(search), [search])
  const [editing, setEditing] = useState(null)
  const [secret, setSecret] = useState(null)
  const [confirm, setConfirm] = useState(null)
  const can = (p) => hasPermission(me, p)

  const confirmAction = async () => {
    if (confirm.kind === 'delete') {
      await adminApi.deleteClient(confirm.client.id)
      toast(t('admin.clients.deleted'))
    } else {
      const result = await adminApi.rotateSecret(confirm.client.id)
      setSecret(result)
    }
    reload()
  }

  return (
    <>
      <PageHeader
        title={t('admin.clients.title')}
        description={t('admin.clients.description')}
        actions={can('client:create') && <Button onClick={() => setEditing(NEW_CLIENT)}><PlusIcon className="size-4" />{t('admin.clients.new')}</Button>}
      />
      {error && <div className="mb-4"><Alert tone="danger">{errorMessage(error, t)}</Alert></div>}

      <Card padded={false}>
        <Toolbar><SearchBox value={search} onChange={setSearch} placeholder={t('admin.clients.search')} /></Toolbar>
        {loading && !data ? <LoadingBlock /> : data?.length === 0 ? (
          <EmptyState icon={<AppIcon />} title={t('admin.clients.empty')} />
        ) : (
          <ul className="divide-y divide-line">
            {(data || []).map((c) => (
              <li key={c.id} className="flex flex-col gap-3 px-5 py-4 lg:flex-row lg:items-start">
                <span className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-primary-soft text-primary"><AppIcon /></span>
                <div className="min-w-0 flex-1">
                  <p className="flex flex-wrap items-center gap-2 font-medium text-ink">
                    {c.clientName}
                    <code className="rounded bg-surface-2 px-1.5 py-0.5 font-mono text-xs text-muted">{c.clientId}</code>
                    <Badge tone={c.confidential ? 'primary' : 'neutral'}>{c.confidential ? t('admin.clients.confidential') : t('admin.clients.public')}</Badge>
                    {c.system && <Badge tone="warning">{t('admin.common.system')}</Badge>}
                  </p>
                  <div className="mt-1.5 flex flex-wrap gap-1">
                    {c.grantTypes.map((g) => <Badge key={g}>{t(`admin.clients.grants.${g}`, { defaultValue: g })}</Badge>)}
                    {c.scopes.map((s) => <code key={s} className="rounded bg-surface-2 px-1.5 py-0.5 font-mono text-[11px] text-muted">{s}</code>)}
                  </div>
                  <p className="mt-1.5 text-xs text-subtle">
                    {t('admin.clients.summary', { access: c.accessTokenMinutes, refresh: c.refreshTokenDays, authorizations: c.activeAuthorizations })}
                    {c.clientIdIssuedAt && ` · ${t('admin.clients.issued', { date: formatDateTime(c.clientIdIssuedAt, i18n.language) })}`}
                  </p>
                  {c.redirectUris.length > 0 && <p className="mt-1 truncate font-mono text-xs text-muted">{c.redirectUris.join('  ·  ')}</p>}
                </div>
                <div className="flex flex-wrap gap-1.5">
                  {can('client:update') && <Button size="sm" variant="secondary" onClick={() => setEditing(toForm(c))}>{t('admin.common.edit')}</Button>}
                  {can('client:update') && c.confidential && (
                    <Button size="sm" variant="ghost" onClick={() => setConfirm({ kind: 'rotate', client: c })}>{t('admin.clients.rotate')}</Button>
                  )}
                  {can('client:delete') && !c.system && (
                    <Button size="sm" variant="danger-outline" onClick={() => setConfirm({ kind: 'delete', client: c })}>{t('admin.common.delete')}</Button>
                  )}
                </div>
              </li>
            ))}
          </ul>
        )}
      </Card>

      {editing && (
        <ClientDialog
          initial={editing}
          onClose={() => setEditing(null)}
          onSaved={(result) => {
            setEditing(null)
            reload()
            if (result?.clientSecret) setSecret(result)
            else toast(t('admin.clients.saved'))
          }}
        />
      )}

      <Dialog open={Boolean(secret)} onClose={() => setSecret(null)} title={t('admin.clients.secretTitle')}
        footer={<Button onClick={() => setSecret(null)}>{t('admin.clients.secretDone')}</Button>}>
        {secret && (
          <div className="flex flex-col gap-3 pt-2 text-ink">
            <Alert tone="warning">{t('admin.clients.secretWarning')}</Alert>
            <dl className="flex flex-col gap-2 text-sm">
              <div><dt className="text-muted">{t('admin.clients.clientId')}</dt><dd className="font-mono">{secret.client.clientId}</dd></div>
              <div>
                <dt className="text-muted">{t('admin.clients.clientSecret')}</dt>
                <dd className="flex items-center gap-2">
                  <code className="min-w-0 flex-1 break-all rounded bg-surface-2 px-2 py-1 font-mono text-xs">{secret.clientSecret}</code>
                  <Button size="sm" variant="secondary" onClick={() => { navigator.clipboard?.writeText(secret.clientSecret); toast(t('admin.clients.copied')) }}>
                    {t('admin.clients.copy')}
                  </Button>
                </dd>
              </div>
            </dl>
          </div>
        )}
      </Dialog>

      <ConfirmDialog
        open={Boolean(confirm)}
        onClose={() => setConfirm(null)}
        onConfirm={confirmAction}
        tone={confirm?.kind === 'delete' ? 'danger' : 'primary'}
        title={confirm?.kind === 'delete' ? t('admin.clients.deleteTitle') : t('admin.clients.rotate')}
        message={confirm?.kind === 'delete'
          ? t('admin.clients.deleteMessage', { name: confirm?.client.clientId })
          : t('admin.clients.rotateMessage', { name: confirm?.client.clientId })}
        confirmLabel={confirm?.kind === 'delete' ? t('admin.common.delete') : t('admin.clients.rotate')}
      />
    </>
  )
}

function ClientDialog({ initial, onClose, onSaved }) {
  const { t } = useTranslation()
  const [form, setForm] = useState(initial)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState(null)
  const editing = Boolean(initial.id)
  const isPublic = form.type === 'public'
  const set = (patch) => setForm((f) => ({ ...f, ...patch }))
  const toggle = (key, value) => setForm((f) => ({ ...f, [key]: f[key].includes(value) ? f[key].filter((v) => v !== value) : [...f[key], value] }))
  const customScopes = form.scopes.filter((s) => !STANDARD_SCOPES.includes(s))

  const save = async (event) => {
    event.preventDefault()
    setBusy(true)
    setError(null)
    const body = {
      clientId: form.clientId,
      clientName: form.clientName,
      authenticationMethods: isPublic ? ['none'] : [form.authMethod],
      grantTypes: isPublic ? form.grantTypes.filter((g) => g !== 'client_credentials') : form.grantTypes,
      redirectUris: form.redirectUris,
      postLogoutRedirectUris: form.postLogoutRedirectUris,
      scopes: form.scopes,
      requireProofKey: isPublic || form.requireProofKey,
      requireConsent: form.requireConsent,
      accessTokenMinutes: form.accessTokenMinutes === '' ? null : Number(form.accessTokenMinutes),
      refreshTokenDays: form.refreshTokenDays === '' ? null : Number(form.refreshTokenDays),
      reuseRefreshTokens: form.reuseRefreshTokens,
    }
    try {
      const result = editing ? await adminApi.updateClient(initial.id, body) : await adminApi.createClient(body)
      onSaved(result)
    } catch (err) {
      setError(errorMessage(err, t))
    } finally {
      setBusy(false)
    }
  }

  return (
    <Dialog
      open
      size="xl"
      onClose={onClose}
      title={editing ? t('admin.clients.edit', { name: initial.clientId }) : t('admin.clients.new')}
      footer={<><Button variant="secondary" onClick={onClose} disabled={busy}>{t('common.cancel')}</Button><Button type="submit" form="client-form" loading={busy}>{t('common.save')}</Button></>}
    >
      <form id="client-form" onSubmit={save} className="flex flex-col gap-5 pt-2 text-ink">
        {error && <Alert tone="danger">{error}</Alert>}
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <Field label={t('admin.clients.clientId')} hint={editing ? t('admin.clients.clientIdFixed') : t('admin.clients.clientIdHint')}>
            {(p) => <Input {...p} required value={form.clientId} disabled={editing} onChange={(e) => set({ clientId: e.target.value })} className="font-mono" />}
          </Field>
          <Field label={t('admin.clients.clientName')}>{(p) => <Input {...p} required value={form.clientName} onChange={(e) => set({ clientName: e.target.value })} />}</Field>
        </div>

        <fieldset>
          <legend className="mb-2 text-sm font-medium">{t('admin.clients.credentials')}</legend>
          <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
            {['confidential', 'public'].map((type) => (
              <label key={type} className={'flex cursor-pointer gap-3 rounded-lg border p-3 text-sm ' + (form.type === type ? 'border-primary bg-primary-soft/40' : 'border-line')}>
                <input type="radio" name="client-type" className="mt-0.5 accent-[var(--c-primary)]" checked={form.type === type} onChange={() => set({ type })} />
                <span><span className="font-medium">{t(`admin.clients.${type}`)}</span><span className="block text-muted">{t(`admin.clients.${type}Hint`)}</span></span>
              </label>
            ))}
          </div>
          {!isPublic && (
            <div className="mt-3 flex flex-wrap gap-4 text-sm">
              {['client_secret_basic', 'client_secret_post'].map((m) => (
                <label key={m} className="flex items-center gap-2">
                  <input type="radio" name="auth-method" className="accent-[var(--c-primary)]" checked={form.authMethod === m} onChange={() => set({ authMethod: m })} />
                  <code className="font-mono text-xs">{m}</code>
                </label>
              ))}
            </div>
          )}
        </fieldset>

        <fieldset>
          <legend className="mb-2 text-sm font-medium">{t('admin.clients.grantTypes')}</legend>
          <div className="flex flex-wrap gap-4">
            {GRANTS.filter((g) => !(isPublic && g === 'client_credentials')).map((g) => (
              <Checkbox key={g} label={t(`admin.clients.grants.${g}`)} checked={form.grantTypes.includes(g)} onChange={() => toggle('grantTypes', g)} />
            ))}
          </div>
        </fieldset>

        {form.grantTypes.includes('authorization_code') && (
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <Field label={t('admin.clients.redirectUris')} hint={t('admin.clients.redirectHint')}>
              {(p) => <ListInput id={p.id} values={form.redirectUris} onChange={(redirectUris) => set({ redirectUris })} placeholder="https://app.example.com/callback" />}
            </Field>
            <Field label={t('admin.clients.logoutUris')}>
              {(p) => <ListInput id={p.id} values={form.postLogoutRedirectUris} onChange={(postLogoutRedirectUris) => set({ postLogoutRedirectUris })} placeholder="https://app.example.com/" />}
            </Field>
          </div>
        )}

        <fieldset>
          <legend className="mb-2 text-sm font-medium">{t('admin.clients.scopes')}</legend>
          <div className="mb-3 flex flex-wrap gap-4">
            {STANDARD_SCOPES.map((s) => (
              <Checkbox key={s} label={s} checked={form.scopes.includes(s)} onChange={() => toggle('scopes', s)} />
            ))}
          </div>
          <ListInput values={customScopes} placeholder={t('admin.clients.customScope')}
            onChange={(custom) => set({ scopes: [...form.scopes.filter((s) => STANDARD_SCOPES.includes(s)), ...custom] })} />
        </fieldset>

        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
          <Checkbox label={t('admin.clients.pkce')} description={t('admin.clients.pkceHint')} checked={isPublic || form.requireProofKey}
            disabled={isPublic} onChange={(e) => set({ requireProofKey: e.target.checked })} />
          <Checkbox label={t('admin.clients.consent')} description={t('admin.clients.consentHint')} checked={form.requireConsent}
            onChange={(e) => set({ requireConsent: e.target.checked })} />
        </div>

        <fieldset className="grid grid-cols-1 gap-4 sm:grid-cols-3">
          <legend className="mb-2 text-sm font-medium sm:col-span-3">{t('admin.clients.tokens')}</legend>
          <Field label={t('admin.clients.accessMinutes')} hint={t('admin.clients.defaultHint')}>
            {(p) => <Input {...p} type="number" min="1" max="1440" value={form.accessTokenMinutes} onChange={(e) => set({ accessTokenMinutes: e.target.value })} />}
          </Field>
          <Field label={t('admin.clients.refreshDays')} hint={t('admin.clients.defaultHint')}>
            {(p) => <Input {...p} type="number" min="1" max="365" value={form.refreshTokenDays} onChange={(e) => set({ refreshTokenDays: e.target.value })} />}
          </Field>
          <div className="pt-7">
            <Checkbox label={t('admin.clients.reuseRefresh')} checked={form.reuseRefreshTokens} onChange={(e) => set({ reuseRefreshTokens: e.target.checked })} />
          </div>
        </fieldset>
      </form>
    </Dialog>
  )
}
