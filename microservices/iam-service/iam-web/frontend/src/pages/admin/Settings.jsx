import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { adminApi } from '../../api/admin'
import { useAuth } from '../../auth/AuthContext'
import { Alert, Badge, Button, Card, Checkbox, Field, Input, PageHeader, useToast } from '../../components/ui'
import { LoadingBlock } from '../../components/admin'
import { ProviderIcon } from '../../components/icons'
import { errorMessage, formatDateTime } from '../../lib/format'
import { hasPermission } from '../../lib/permissions'
import { useResource } from '../../lib/useResource'

const PROVIDERS = ['google', 'github', 'facebook', 'telegram']
const SECTIONS = ['authentication', 'password', 'session', 'tokens']
// Sections served by /api/admin/settings/security; token lifetimes are only in /api/admin/settings.
const SECURITY_SECTIONS = ['authentication', 'password', 'session']

/** The sections whose values differ from the saved settings, e.g. { password: { minLength: 10 } }. */
function changedSections(saved, form) {
  const changes = {}
  for (const section of SECTIONS) {
    for (const [key, value] of Object.entries(form[section] || {})) {
      if (JSON.stringify(value) !== JSON.stringify(saved[section]?.[key])) {
        changes[section] = { ...changes[section], [key]: value }
      }
    }
  }
  return changes
}

function NumberField({ label, hint, value, onChange, min, max, disabled, suffix }) {
  return (
    <Field label={label} hint={hint}>
      {(p) => (
        <div className="flex items-center gap-2">
          <Input {...p} type="number" min={min} max={max} required value={value} disabled={disabled}
            onChange={(e) => onChange(e.target.value === '' ? '' : Number(e.target.value))} className="w-32" />
          {suffix && <span className="text-sm text-muted">{suffix}</span>}
        </div>
      )}
    </Field>
  )
}

export default function Settings() {
  const { t, i18n } = useTranslation()
  const { user: me } = useAuth()
  const toast = useToast()
  const { data, loading, error, reload } = useResource(() => adminApi.settings(), [])
  const [form, setForm] = useState(null)
  const [busy, setBusy] = useState(false)
  const [saveError, setSaveError] = useState(null)
  const editable = hasPermission(me, 'settings:update')
  useEffect(() => { if (data) setForm(data) }, [data])

  const set = (section, key) => (value) => setForm((f) => ({ ...f, [section]: { ...f[section], [key]: value } }))
  const toggleProvider = (provider) => setForm((f) => {
    const list = f.authentication.enabledSocialProviders
    return { ...f, authentication: { ...f.authentication, enabledSocialProviders: list.includes(provider) ? list.filter((p) => p !== provider) : [...list, provider] } }
  })

  const save = async (event) => {
    event.preventDefault()
    setBusy(true)
    setSaveError(null)
    try {
      const changes = changedSections(data, form)
      const sections = Object.keys(changes)
      if (sections.length === 0) {
        toast(t('admin.settings.noChanges'), 'info')
        return
      }
      // Only the changed values are sent (PATCH); security-only edits go to the security policies endpoint.
      if (sections.every((s) => SECURITY_SECTIONS.includes(s))) await adminApi.patchSecurityPolicies(changes)
      else await adminApi.patchSettings(changes)
      toast(t('admin.settings.saved'))
      reload()
    } catch (err) {
      setSaveError(errorMessage(err, t))
    } finally {
      setBusy(false)
    }
  }

  if (loading && !data) return <LoadingBlock />
  if (error) return <Alert tone="danger">{errorMessage(error, t)}</Alert>
  if (!form) return null
  const { authentication: a, password: p, session: s, tokens: k } = form
  const dirty = Object.keys(changedSections(data, form)).length > 0

  return (
    <form onSubmit={save}>
      <PageHeader
        title={t('admin.settings.title')}
        description={t('admin.settings.description')}
        actions={editable && (
          <div className="flex items-center gap-2">
            {dirty && <Badge tone="warning">{t('admin.settings.unsaved')}</Badge>}
            {dirty && <Button type="button" variant="secondary" onClick={() => setForm(data)}>{t('admin.settings.discard')}</Button>}
            <Button type="submit" loading={busy} disabled={!dirty}>{t('common.save')}</Button>
          </div>
        )}
      />
      {saveError && <div className="mb-4"><Alert tone="danger" onDismiss={() => setSaveError(null)}>{saveError}</Alert></div>}
      {data.updatedAt && (
        <p className="mb-4 text-xs text-subtle">{t('admin.settings.lastUpdated', { when: formatDateTime(data.updatedAt, i18n.language), by: data.updatedBy })}</p>
      )}

      <div className="grid grid-cols-1 gap-6 xl:grid-cols-2">
        <Card title={t('admin.settings.authentication')} description={t('admin.settings.authenticationHint')}>
          <div className="flex flex-col gap-4">
            <Checkbox label={t('admin.settings.allowRegistration')} description={t('admin.settings.allowRegistrationHint')}
              checked={a.allowSelfRegistration} disabled={!editable} onChange={(e) => set('authentication', 'allowSelfRegistration')(e.target.checked)} />
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <NumberField label={t('admin.settings.maxFailed')} hint={t('admin.settings.maxFailedHint')} min={0} max={100}
                value={a.maxFailedAttempts} disabled={!editable} onChange={set('authentication', 'maxFailedAttempts')} />
              <NumberField label={t('admin.settings.lockout')} hint={t('admin.settings.lockoutHint')} min={0} max={10080}
                value={a.lockoutMinutes} disabled={!editable} onChange={set('authentication', 'lockoutMinutes')} suffix={t('admin.settings.minutes')} />
            </div>
            <fieldset>
              <legend className="mb-2 text-sm font-medium text-ink">{t('admin.settings.providers')}</legend>
              <div className="grid grid-cols-2 gap-2">
                {PROVIDERS.map((provider) => (
                  <label key={provider} className="flex items-center gap-2 rounded-lg border border-line px-3 py-2 text-sm text-ink">
                    <input type="checkbox" className="size-4 accent-[var(--c-primary)]" disabled={!editable}
                      checked={a.enabledSocialProviders.includes(provider)} onChange={() => toggleProvider(provider)} />
                    <ProviderIcon provider={provider} className="size-4" />
                    {t(`methods.${provider}`, { defaultValue: provider })}
                  </label>
                ))}
              </div>
            </fieldset>
          </div>
        </Card>

        <Card title={t('admin.settings.password')} description={t('admin.settings.passwordHint')}>
          <div className="flex flex-col gap-4">
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <NumberField label={t('admin.settings.minLength')} min={6} max={128} value={p.minLength} disabled={!editable} onChange={set('password', 'minLength')}
                suffix={t('admin.settings.characters')} />
              <NumberField label={t('admin.settings.maxAge')} hint={t('admin.settings.maxAgeHint')} min={0} max={3650} value={p.maxAgeDays}
                disabled={!editable} onChange={set('password', 'maxAgeDays')} suffix={t('admin.settings.days')} />
            </div>
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
              {[['requireUppercase', 'uppercase'], ['requireLowercase', 'lowercase'], ['requireDigit', 'digit'], ['requireSpecial', 'special']].map(([key, label]) => (
                <Checkbox key={key} label={t(`admin.settings.require.${label}`)} checked={p[key]} disabled={!editable}
                  onChange={(e) => set('password', key)(e.target.checked)} />
              ))}
            </div>
          </div>
        </Card>

        <Card title={t('admin.settings.session')} description={t('admin.settings.sessionHint')}>
          <NumberField label={t('admin.settings.idleTimeout')} hint={t('admin.settings.idleTimeoutHint')} min={5} max={43200}
            value={s.idleTimeoutMinutes} disabled={!editable} onChange={set('session', 'idleTimeoutMinutes')} suffix={t('admin.settings.minutes')} />
        </Card>

        <Card title={t('admin.settings.tokens')} description={t('admin.settings.tokensHint')}>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
            <NumberField label={t('admin.settings.accessToken')} min={1} max={1440} value={k.accessTokenMinutes} disabled={!editable}
              onChange={set('tokens', 'accessTokenMinutes')} suffix={t('admin.settings.minutes')} />
            <NumberField label={t('admin.settings.refreshToken')} min={1} max={365} value={k.refreshTokenDays} disabled={!editable}
              onChange={set('tokens', 'refreshTokenDays')} suffix={t('admin.settings.days')} />
            <NumberField label={t('admin.settings.authCode')} min={1} max={30} value={k.authorizationCodeMinutes} disabled={!editable}
              onChange={set('tokens', 'authorizationCodeMinutes')} suffix={t('admin.settings.minutes')} />
          </div>
          <div className="mt-4"><Alert tone="info">{t('admin.settings.tokensNote')}</Alert></div>
        </Card>
      </div>
    </form>
  )
}
