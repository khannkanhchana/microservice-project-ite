import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { adminApi } from '../../api/admin'
import { useAuth } from '../../auth/AuthContext'
import { Alert, Badge, Button, Card, Checkbox, ConfirmDialog, Dialog, EmptyState, Field, Input, PageHeader, useToast } from '../../components/ui'
import { ListInput, LoadingBlock, SearchBox, Toolbar } from '../../components/admin'
import { AppIcon, ChevronRightIcon, GlobeIcon, KeyIcon, PhoneIcon, PlusIcon, RefreshIcon, TerminalIcon } from '../../components/icons'
import { errorMessage, formatDateTime } from '../../lib/format'
import { hasPermission } from '../../lib/permissions'
import { useResource } from '../../lib/useResource'

const GRANTS = ['authorization_code', 'refresh_token', 'client_credentials']
const STANDARD_SCOPES = ['openid', 'profile', 'email', 'phone', 'address']

/**
 * The three kinds of application an admin can register. Picking one fills in the technical settings
 * (client type, grants, PKCE) so the admin does not have to know OAuth2 to register a client.
 */
const KINDS = {
  web: { icon: GlobeIcon, type: 'confidential', grantTypes: ['authorization_code', 'refresh_token'], requireProofKey: true },
  spa: { icon: PhoneIcon, type: 'public', grantTypes: ['authorization_code', 'refresh_token'], requireProofKey: true },
  service: { icon: TerminalIcon, type: 'confidential', grantTypes: ['client_credentials'], requireProofKey: false },
}

const NEW_CLIENT = {
  kind: 'web', clientId: '', clientName: '', type: 'confidential', authMethod: 'client_secret_basic',
  grantTypes: ['authorization_code', 'refresh_token'], redirectUris: [], postLogoutRedirectUris: [],
  scopes: ['openid', 'profile', 'email'], requireProofKey: true, requireConsent: false,
  accessTokenMinutes: '', refreshTokenDays: '', reuseRefreshTokens: false, clientSecret: '',
}

/** 32 random bytes as URL-safe Base64 (43 chars), the same format the server generates. */
const generateSecret = () => {
  const bytes = crypto.getRandomValues(new Uint8Array(32))
  return btoa(String.fromCharCode(...bytes)).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')
}

const kindOf =(confidential, grantTypes) =>
  !confidential ? 'spa' : grantTypes.includes('authorization_code') ? 'web' : 'service'

const toForm = (c) => ({
  ...c,
  kind: kindOf(c.confidential, c.grantTypes),
  type: c.confidential ? 'confidential' : 'public',
  authMethod: c.authenticationMethods.find((m) => m !== 'none') || 'client_secret_basic',
  accessTokenMinutes: String(c.accessTokenMinutes),
  refreshTokenDays: String(c.refreshTokenDays),
  clientSecret: '',
})

/** "My Shop Web" -> "my-shop-web": a valid client ID (letters, digits, ".", "_", "-"; 3-100 chars). */
const slugify = (name, fallback) => {
  const slug = name.toLowerCase().trim().replace(/[^a-z0-9._-]+/g, '-').replace(/-{2,}/g, '-').replace(/^[-.]+|[-.]+$/g, '').slice(0, 100)
  return slug.length >= 3 ? slug : fallback
}

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
      await adminApi.deleteClient(confirm.client.clientId)
      toast(t('admin.clients.deleted'))
    } else {
      const result = await adminApi.rotateSecret(confirm.client.clientId)
      setSecret({ ...result, reason: 'rotated' })
    }
    reload()
  }

  // Opens the editor with the client's current settings from GET /api/clients/{clientId}.
  const [opening, setOpening] = useState(null)
  const openEditor = async (client) => {
    setOpening(client.clientId)
    try {
      setEditing(toForm(await adminApi.client(client.clientId)))
    } catch (err) {
      toast(errorMessage(err, t), 'danger')
    } finally {
      setOpening(null)
    }
  }

  const copy = (value) => {
    navigator.clipboard?.writeText(value)
    toast(t('admin.clients.copied'))
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
          <EmptyState icon={<AppIcon />} title={t('admin.clients.empty')} description={t('admin.clients.emptyHint')} />
        ) : (
          <ul className="divide-y divide-line">
            {(data || []).map((c) => {
              const kind = kindOf(c.confidential, c.grantTypes)
              const KindIcon = KINDS[kind].icon
              return (
                <li key={c.id} className="flex flex-col gap-3 px-5 py-4 sm:flex-row sm:items-center">
                  <span className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-primary-soft text-primary"><KindIcon /></span>
                  <div className="min-w-0 flex-1">
                    <p className="flex flex-wrap items-center gap-2 font-medium text-ink">
                      {c.clientName}
                      <Badge tone={c.confidential ? 'primary' : 'neutral'}>{t(`admin.clients.kinds.${kind}.short`)}</Badge>
                      {c.system && <Badge tone="warning">{t('admin.common.system')}</Badge>}
                    </p>
                    <p className="mt-1 flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-muted">
                      <span className="flex items-center gap-1">
                        <span className="text-subtle">{t('admin.clients.clientId')}:</span>
                        <code className="rounded bg-surface-2 px-1.5 py-0.5 font-mono text-ink">{c.clientId}</code>
                      </span>
                      <span>{t('admin.clients.scopeCount', { count: c.scopes.length })}</span>
                      <span>{t('admin.clients.activeLogins', { count: c.activeAuthorizations })}</span>
                      {c.clientIdIssuedAt && <span>{t('admin.clients.issued', { date: formatDateTime(c.clientIdIssuedAt, i18n.language) })}</span>}
                    </p>
                  </div>
                  <div className="flex flex-wrap gap-1.5">
                    {can('client:update') && <Button size="sm" variant="secondary" loading={opening === c.clientId} onClick={() => openEditor(c)}>{t('admin.common.edit')}</Button>}
                    {can('client:update') && c.confidential && (
                      <Button size="sm" variant="ghost" onClick={() => setConfirm({ kind: 'rotate', client: c })}>{t('admin.clients.rotate')}</Button>
                    )}
                    {can('client:delete') && !c.system && (
                      <Button size="sm" variant="danger-outline" onClick={() => setConfirm({ kind: 'delete', client: c })}>{t('admin.common.delete')}</Button>
                    )}
                  </div>
                </li>
              )
            })}
          </ul>
        )}
      </Card>

      {editing && (
        <ClientDialog
          initial={editing}
          onClose={() => setEditing(null)}
          onSaved={(result, created) => {
            setEditing(null)
            reload()
            if (result?.clientSecret) setSecret({ ...result, reason: created ? 'created' : 'issued' })
            else toast(created ? t('admin.clients.created') : t('admin.clients.saved'))
          }}
        />
      )}

      <Dialog open={Boolean(secret)} onClose={() => setSecret(null)}
        title={secret?.reason === 'created' ? t('admin.clients.createdTitle') : t('admin.clients.secretTitle')}
        footer={<Button onClick={() => setSecret(null)}>{t('admin.clients.secretDone')}</Button>}>
        {secret && (
          <div className="flex flex-col gap-4 pt-2 text-ink">
            <Alert tone="warning">{t('admin.clients.secretWarning')}</Alert>
            <CopyRow label={t('admin.clients.clientId')} value={secret.client.clientId} onCopy={copy} copyLabel={t('admin.clients.copy')} />
            <CopyRow label={t('admin.clients.clientSecret')} value={secret.clientSecret} onCopy={copy} copyLabel={t('admin.clients.copy')} />
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

function CopyRow({ label, value, onCopy, copyLabel }) {
  return (
    <div>
      <p className="mb-1 text-sm text-muted">{label}</p>
      <div className="flex items-center gap-2">
        <code className="min-w-0 flex-1 break-all rounded-lg border border-line bg-surface-2 px-3 py-2 font-mono text-xs">{value}</code>
        <Button size="sm" variant="secondary" onClick={() => onCopy(value)}>{copyLabel}</Button>
      </div>
    </div>
  )
}

/** A numbered section of the register/edit form. */
function Step({ n, title, hint, children }) {
  return (
    <section className="flex gap-3">
      <span className="flex size-6 shrink-0 items-center justify-center rounded-full bg-primary-soft text-xs font-semibold text-primary">{n}</span>
      <div className="min-w-0 flex-1">
        <h3 className="text-sm font-semibold text-ink">{title}</h3>
        {hint && <p className="mt-0.5 text-xs text-subtle">{hint}</p>}
        <div className="mt-3">{children}</div>
      </div>
    </section>
  )
}

function ClientDialog({ initial, onClose, onSaved }) {
  const { t } = useTranslation()
  const toast = useToast()
  const [form, setForm] = useState(initial)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState(null)
  const editing = Boolean(initial.id)
  // On create, the client ID follows the name until the admin types their own.
  const [idTouched, setIdTouched] = useState(editing)
  const [fallbackId] = useState(() => `app-${Math.random().toString(36).slice(2, 8)}`)
  const isPublic = form.type === 'public'
  const userFacing = form.grantTypes.includes('authorization_code')
  const set = (patch) => setForm((f) => ({ ...f, ...patch }))
  const toggle = (key, value) => setForm((f) => ({ ...f, [key]: f[key].includes(value) ? f[key].filter((v) => v !== value) : [...f[key], value] }))
  const customScopes = form.scopes.filter((s) => !STANDARD_SCOPES.includes(s))

  const setName = (clientName) => set(idTouched ? { clientName } : { clientName, clientId: clientName ? slugify(clientName, fallbackId) : '' })

  const chooseKind = (kind) => {
    const { type, grantTypes, requireProofKey } = KINDS[kind]
    set({ kind, type, grantTypes, requireProofKey })
  }

  // The secret is never typed in: the admin clicks Generate (and can generate again until saving).
  const hasStoredSecret = editing && initial.confidential
  const secretHint = form.clientSecret
    ? t('admin.clients.secretGenerated')
    : hasStoredSecret ? t('admin.clients.secretExisting') : t('admin.clients.secretClickGenerate')

  const save = async (event) => {
    event.preventDefault()
    setBusy(true)
    setError(null)
    const body = {
      clientId: form.clientId,
      clientName: form.clientName,
      authenticationMethods: isPublic ? ['none'] : [form.authMethod],
      grantTypes: isPublic ? form.grantTypes.filter((g) => g !== 'client_credentials') : form.grantTypes,
      redirectUris: userFacing ? form.redirectUris : [],
      postLogoutRedirectUris: userFacing ? form.postLogoutRedirectUris : [],
      // Sign-in scopes (openid, profile, ...) only make sense when a user signs in.
      scopes: userFacing ? form.scopes : customScopes,
      requireProofKey: isPublic || form.requireProofKey,
      requireConsent: form.requireConsent,
      accessTokenMinutes: form.accessTokenMinutes === '' ? null : Number(form.accessTokenMinutes),
      refreshTokenDays: form.refreshTokenDays === '' ? null : Number(form.refreshTokenDays),
      reuseRefreshTokens: form.reuseRefreshTokens,
      clientSecret: isPublic || !form.clientSecret ? null : form.clientSecret,
    }
    try {
      const result = editing ? await adminApi.updateClient(initial.clientId, body) : await adminApi.createClient(body)
      onSaved(result, !editing)
    } catch (err) {
      setError(errorMessage(err, t))
    } finally {
      setBusy(false)
    }
  }

  return (
    <Dialog
      open
      size="lg"
      onClose={onClose}
      title={editing ? t('admin.clients.edit', { name: initial.clientId }) : t('admin.clients.new')}
      footer={<>
        <Button variant="secondary" onClick={onClose} disabled={busy}>{t('common.cancel')}</Button>
        <Button type="submit" form="client-form" loading={busy}>{editing ? t('common.save') : t('admin.clients.registerButton')}</Button>
      </>}
    >
      <form id="client-form" onSubmit={save} className="flex flex-col gap-6 pt-2 text-ink">
        {error && <Alert tone="danger">{error}</Alert>}

        <Step n={1} title={t('admin.clients.stepKind')} hint={t('admin.clients.stepKindHint')}>
          <div className="grid grid-cols-1 gap-2 sm:grid-cols-3">
            {Object.entries(KINDS).map(([kind, { icon: Icon }]) => {
              const active = form.kind === kind
              return (
                <button key={kind} type="button" onClick={() => chooseKind(kind)} aria-pressed={active}
                  className={'flex flex-col gap-1.5 rounded-lg border p-3 text-left text-sm transition-colors '
                    + (active ? 'border-primary bg-primary-soft/40 ring-1 ring-primary' : 'border-line hover:bg-surface-2')}>
                  <Icon className={'size-5 ' + (active ? 'text-primary' : 'text-muted')} />
                  <span className="font-medium text-ink">{t(`admin.clients.kinds.${kind}.title`)}</span>
                  <span className="text-xs text-muted">{t(`admin.clients.kinds.${kind}.hint`)}</span>
                </button>
              )
            })}
          </div>
        </Step>

        <Step n={2} title={t('admin.clients.stepInfo')}>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <Field label={t('admin.clients.clientName')} hint={t('admin.clients.clientNameHint')}>
              {(p) => <Input {...p} required autoFocus value={form.clientName} placeholder={t('admin.clients.clientNamePlaceholder')} onChange={(e) => setName(e.target.value)} />}
            </Field>
            <Field label={t('admin.clients.clientId')} hint={editing ? t('admin.clients.clientIdFixed') : t('admin.clients.clientIdAuto')}>
              {(p) => <Input {...p} required value={form.clientId} disabled={editing} className="font-mono"
                onChange={(e) => { setIdTouched(true); set({ clientId: e.target.value }) }} />}
            </Field>
          </div>
          {isPublic ? (
            <div className="mt-4 flex items-start gap-2.5 rounded-lg border border-line bg-surface-2 px-3 py-2.5 text-xs text-muted">
              <KeyIcon className="mt-0.5 size-4 shrink-0 text-primary" />
              <span><span className="font-medium text-ink">{t('admin.clients.clientSecret')}: </span>{t('admin.clients.noSecret')}</span>
            </div>
          ) : (
            <Field className="mt-4" label={t('admin.clients.clientSecret')} hint={secretHint}>
              {(p) => (
                <div className="flex flex-col gap-2 sm:flex-row">
                  <Input {...p} readOnly value={form.clientSecret} className="font-mono text-xs"
                    placeholder={hasStoredSecret ? '••••••••••••••••••••••••' : t('admin.clients.secretPlaceholder')} />
                  <div className="flex shrink-0 gap-2">
                    <Button type="button" variant={form.clientSecret ? 'secondary' : 'primary'} onClick={() => set({ clientSecret: generateSecret() })}>
                      <RefreshIcon className="size-4" />
                      {form.clientSecret ? t('admin.clients.generateAgain') : hasStoredSecret ? t('admin.clients.generateNew') : t('admin.clients.generate')}
                    </Button>
                    {form.clientSecret && (
                      <Button type="button" variant="secondary" onClick={() => { navigator.clipboard?.writeText(form.clientSecret); toast(t('admin.clients.copied')) }}>
                        {t('admin.clients.copy')}
                      </Button>
                    )}
                  </div>
                </div>
              )}
            </Field>
          )}
        </Step>

        {userFacing && (
          <Step n={3} title={t('admin.clients.stepUrls')} hint={t('admin.clients.stepUrlsHint')}>
            <div className="flex flex-col gap-4">
              <Field label={t('admin.clients.redirectUris')} hint={t('admin.clients.redirectHint')}>
                {(p) => <ListInput id={p.id} values={form.redirectUris} onChange={(redirectUris) => set({ redirectUris })} placeholder="https://app.example.com/callback" />}
              </Field>
              <Field label={t('admin.clients.logoutUris')} hint={t('admin.clients.logoutHint')}>
                {(p) => <ListInput id={p.id} values={form.postLogoutRedirectUris} onChange={(postLogoutRedirectUris) => set({ postLogoutRedirectUris })} placeholder="https://app.example.com/" />}
              </Field>
            </div>
          </Step>
        )}

        <Step n={userFacing ? 4 : 3} title={t('admin.clients.stepScopes')}
          hint={userFacing ? t('admin.clients.stepScopesHint') : t('admin.clients.stepScopesServiceHint')}>
          {userFacing && (
            <div className="mb-3 grid grid-cols-1 gap-2.5 sm:grid-cols-2">
              {STANDARD_SCOPES.map((s) => (
                <Checkbox key={s} label={s} description={t(`admin.clients.scopeHints.${s}`)}
                  checked={form.scopes.includes(s)} onChange={() => toggle('scopes', s)} />
              ))}
            </div>
          )}
          <ListInput values={customScopes} placeholder={t('admin.clients.customScope')}
            onChange={(custom) => set({ scopes: [...form.scopes.filter((s) => STANDARD_SCOPES.includes(s)), ...custom] })} />
        </Step>

        <details className="group rounded-lg border border-line">
          <summary className="flex cursor-pointer list-none items-center gap-2 px-4 py-3 text-sm font-medium text-ink">
            <ChevronRightIcon className="size-4 text-muted transition-transform group-open:rotate-90" />
            {t('admin.clients.advanced')}
            <span className="font-normal text-subtle">— {t('admin.clients.advancedHint')}</span>
          </summary>
          <div className="flex flex-col gap-5 border-t border-line px-4 py-4">
            {!isPublic && (
              <fieldset>
                <legend className="mb-2 text-sm font-medium">{t('admin.clients.authMethod')}</legend>
                <div className="flex flex-wrap gap-4 text-sm">
                  {['client_secret_basic', 'client_secret_post'].map((m) => (
                    <label key={m} className="flex items-center gap-2">
                      <input type="radio" name="auth-method" className="accent-[var(--c-primary)]" checked={form.authMethod === m} onChange={() => set({ authMethod: m })} />
                      <code className="font-mono text-xs">{m}</code>
                    </label>
                  ))}
                </div>
              </fieldset>
            )}

            <fieldset>
              <legend className="mb-2 text-sm font-medium">{t('admin.clients.grantTypes')}</legend>
              <div className="flex flex-wrap gap-4">
                {GRANTS.filter((g) => !(isPublic && g === 'client_credentials')).map((g) => (
                  <Checkbox key={g} label={t(`admin.clients.grants.${g}`)} checked={form.grantTypes.includes(g)} onChange={() => toggle('grantTypes', g)} />
                ))}
              </div>
            </fieldset>

            {userFacing && (
              <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
                <Checkbox label={t('admin.clients.pkce')} description={t('admin.clients.pkceHint')} checked={isPublic || form.requireProofKey}
                  disabled={isPublic} onChange={(e) => set({ requireProofKey: e.target.checked })} />
                <Checkbox label={t('admin.clients.consent')} description={t('admin.clients.consentHint')} checked={form.requireConsent}
                  onChange={(e) => set({ requireConsent: e.target.checked })} />
              </div>
            )}

            <fieldset className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <legend className="mb-2 text-sm font-medium sm:col-span-2">{t('admin.clients.tokens')}</legend>
              <Field label={t('admin.clients.accessMinutes')} hint={t('admin.clients.defaultHint')}>
                {(p) => <Input {...p} type="number" min="1" max="1440" value={form.accessTokenMinutes} onChange={(e) => set({ accessTokenMinutes: e.target.value })} />}
              </Field>
              {form.grantTypes.includes('refresh_token') && (
                <Field label={t('admin.clients.refreshDays')} hint={t('admin.clients.defaultHint')}>
                  {(p) => <Input {...p} type="number" min="1" max="365" value={form.refreshTokenDays} onChange={(e) => set({ refreshTokenDays: e.target.value })} />}
                </Field>
              )}
              {form.grantTypes.includes('refresh_token') && (
                <div className="sm:col-span-2">
                  <Checkbox label={t('admin.clients.reuseRefresh')} checked={form.reuseRefreshTokens} onChange={(e) => set({ reuseRefreshTokens: e.target.checked })} />
                </div>
              )}
            </fieldset>
          </div>
        </details>
      </form>
    </Dialog>
  )
}
