import { useEffect, useState } from 'react'
import { basePath } from '../../config'
import { useSearchParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { accountApi } from '../../api/account'
import { useAuth } from '../../auth/AuthContext'
import { Alert, Badge, Button, Card, ConfirmDialog, EmptyState, PageHeader, Spinner, useToast } from '../../components/ui'
import { LinkIcon, ProviderIcon } from '../../components/icons'
import { errorMessage, formatDate, relativeTime } from '../../lib/format'
import { useResource } from '../../lib/useResource'

const NAMES = { google: 'Google', github: 'GitHub', facebook: 'Facebook', telegram: 'Telegram' }
const providerName = (id) => NAMES[id] || id

export default function LinkedAccounts() {
  const { t, i18n } = useTranslation()
  const toast = useToast()
  const { user } = useAuth()
  const [params, setParams] = useSearchParams()
  const [linking, setLinking] = useState(null)
  const [unlinkTarget, setUnlinkTarget] = useState(null)
  const [error, setError] = useState(null)

  const { data, loading, error: loadError, reload } = useResource(async () => {
    const [providers, identities] = await Promise.all([accountApi.providers(), accountApi.identities()])
    return { providers, identities }
  }, [])

  // Result of a link flow that returned from the IAM server.
  useEffect(() => {
    const linked = params.get('linked')
    const linkError = params.get('linkError')
    if (linked) toast(t('linked.linkedSuccess', { provider: providerName(linked) }))
    if (linkError) setError(t('linked.linkFailed', { message: linkError }))
    if (linked || linkError) setParams({}, { replace: true })
  }, [params, setParams, t, toast])

  const startLink = async (provider) => {
    setLinking(provider)
    setError(null)
    try {
      const returnUrl = `${window.location.origin}${basePath}/account/linked-accounts`
      const { authorizationUrl } = await accountApi.startLink(provider, returnUrl)
      window.location.assign(authorizationUrl)
    } catch (err) {
      setError(errorMessage(err, t))
      setLinking(null)
    }
  }

  const unlink = async () => {
    try {
      await accountApi.unlink(unlinkTarget.uuid)
      toast(t('linked.unlinked', { provider: providerName(unlinkTarget.provider) }))
      reload()
    } catch (err) {
      setError(errorMessage(err, t))
    }
  }

  const identities = data?.identities || []
  const providerIds = [...new Set([...(data?.providers || []).map((p) => p.id), ...identities.map((i) => i.provider)])]
  const configured = new Set((data?.providers || []).map((p) => p.id))
  const isLastMethod = !user.hasPassword && identities.length <= 1

  return (
    <>
      <PageHeader title={t('linked.title')} description={t('linked.description')} />
      {(error || loadError) && (
        <div className="mb-4">
          <Alert tone="danger" onDismiss={error ? () => setError(null) : undefined}>
            {error || errorMessage(loadError, t)}
          </Alert>
        </div>
      )}
      <Card padded={false}>
        {loading && !data ? (
          <div className="flex justify-center py-12 text-primary">
            <Spinner className="size-6" />
          </div>
        ) : providerIds.length === 0 ? (
          <EmptyState icon={<LinkIcon />} title={t('linked.noProviders')} />
        ) : (
          <ul className="divide-y divide-line">
            {providerIds.map((provider) => {
              const identity = identities.find((item) => item.provider === provider)
              return (
                <li key={provider} className="flex flex-col gap-4 px-5 py-4 sm:flex-row sm:items-center">
                  <div className="flex min-w-0 flex-1 items-center gap-4">
                    <span className="flex size-11 shrink-0 items-center justify-center rounded-xl border border-line bg-surface">
                      <ProviderIcon provider={provider} className="size-6" />
                    </span>
                    <div className="min-w-0">
                      <p className="flex flex-wrap items-center gap-2 font-medium text-ink">
                        {providerName(provider)}
                        {identity ? <Badge tone="success">{t('linked.linked')}</Badge> : <Badge>{t('linked.notLinked')}</Badge>}
                      </p>
                      {identity && (
                        <>
                          <p className="truncate text-sm text-muted">
                            {[identity.displayName, identity.email || identity.username].filter(Boolean).join(' · ')}
                          </p>
                          <p className="text-xs text-subtle">
                            {t('linked.linkedOn', { date: formatDate(identity.linkedAt, i18n.language) })}
                            {identity.lastLoginAt &&
                              ` · ${t('linked.lastUsed', { when: relativeTime(identity.lastLoginAt, i18n.language) })}`}
                          </p>
                        </>
                      )}
                    </div>
                  </div>
                  <div className="sm:shrink-0">
                    {identity ? (
                      <div className="flex flex-col items-start gap-1 sm:items-end">
                        <Button
                          variant="danger-outline"
                          size="sm"
                          disabled={isLastMethod}
                          onClick={() => setUnlinkTarget(identity)}
                        >
                          {t('linked.unlink')}
                        </Button>
                        {isLastMethod && <p className="max-w-56 text-xs text-subtle sm:text-right">{t('linked.lastMethod')}</p>}
                      </div>
                    ) : configured.has(provider) ? (
                      <Button variant="secondary" size="sm" loading={linking === provider} onClick={() => startLink(provider)}>
                        {t('linked.link')}
                      </Button>
                    ) : (
                      <Badge>{t('linked.unavailable')}</Badge>
                    )}
                  </div>
                </li>
              )
            })}
          </ul>
        )}
      </Card>

      <ConfirmDialog
        open={Boolean(unlinkTarget)}
        onClose={() => setUnlinkTarget(null)}
        onConfirm={unlink}
        title={t('linked.unlinkTitle', { provider: providerName(unlinkTarget?.provider) })}
        message={t('linked.unlinkMessage', { provider: providerName(unlinkTarget?.provider) })}
        confirmLabel={t('linked.unlink')}
      />
    </>
  )
}
