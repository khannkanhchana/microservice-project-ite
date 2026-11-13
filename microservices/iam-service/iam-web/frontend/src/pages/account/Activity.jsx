import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { accountApi } from '../../api/account'
import { Alert, Button, Card, EmptyState, PageHeader, Pagination, Spinner } from '../../components/ui'
import {
  ActivityIcon,
  CameraIcon,
  DevicesIcon,
  KeyIcon,
  LinkIcon,
  MailIcon,
  PhoneIcon,
  PowerIcon,
  RefreshIcon,
  UserIcon,
} from '../../components/icons'
import { errorMessage, formatDateTime, relativeTime } from '../../lib/format'
import { useResource } from '../../lib/useResource'
import { deviceLabel } from '../../lib/devices'

const PAGE_SIZE = 20

const STYLE = {
  PROFILE_UPDATED: [UserIcon, 'bg-primary-soft text-primary'],
  USERNAME_CHANGED: [UserIcon, 'bg-primary-soft text-primary'],
  EMAIL_CHANGED: [MailIcon, 'bg-warning-soft text-warning'],
  PHONE_CHANGED: [PhoneIcon, 'bg-primary-soft text-primary'],
  PICTURE_UPDATED: [CameraIcon, 'bg-primary-soft text-primary'],
  PICTURE_REMOVED: [CameraIcon, 'bg-surface-2 text-muted'],
  PASSWORD_CHANGED: [KeyIcon, 'bg-warning-soft text-warning'],
  PASSWORD_RESET_REQUESTED: [MailIcon, 'bg-warning-soft text-warning'],
  PASSWORD_RESET: [KeyIcon, 'bg-warning-soft text-warning'],
  IDENTITY_LINKED: [LinkIcon, 'bg-success-soft text-success'],
  IDENTITY_UNLINKED: [LinkIcon, 'bg-surface-2 text-muted'],
  SESSION_REVOKED: [DevicesIcon, 'bg-surface-2 text-muted'],
  OTHER_SESSIONS_REVOKED: [DevicesIcon, 'bg-surface-2 text-muted'],
  ACCOUNT_DEACTIVATED: [PowerIcon, 'bg-danger-soft text-danger'],
}

const PROVIDERS = { google: 'Google', github: 'GitHub', facebook: 'Facebook', telegram: 'Telegram' }

/** Localized title and detail line for one activity record. */
function describe(t, item) {
  const d = item.description
  switch (item.type) {
    case 'PROFILE_UPDATED':
      return [
        t('activityType.PROFILE_UPDATED'),
        d && t('activityDetail.fields', { fields: d.split(', ').map((f) => t(`fields.${f}`, { defaultValue: f })).join(', ') }),
      ]
    case 'PASSWORD_CHANGED':
      return [d === 'set' ? t('activityType.PASSWORD_SET') : t('activityType.PASSWORD_CHANGED'), null]
    case 'PASSWORD_RESET':
      return [t('activityType.PASSWORD_RESET'), d === 'reset by administrator' ? t('activityDetail.byAdmin') : null]
    case 'IDENTITY_LINKED':
    case 'IDENTITY_UNLINKED':
      return [t(`activityType.${item.type}`), d && (PROVIDERS[d] || d)]
    case 'OTHER_SESSIONS_REVOKED':
      return [t('activityType.OTHER_SESSIONS_REVOKED'), d && t('activityDetail.devices', { count: Number(d) })]
    case 'PHONE_CHANGED':
      return [t('activityType.PHONE_CHANGED'), d === 'removed' ? t('activityDetail.removed') : d]
    case 'SESSION_REVOKED':
      return [t('activityType.SESSION_REVOKED'), null]
    default:
      return [t(`activityType.${item.type}`, { defaultValue: item.type }), d]
  }
}

export default function Activity() {
  const { t, i18n } = useTranslation()
  const [page, setPage] = useState(0)
  const { data, loading, error, reload } = useResource(() => accountApi.activity({ page, size: PAGE_SIZE }), [page])
  const items = data?.content || []

  return (
    <>
      <PageHeader
        title={t('activity.title')}
        description={t('activity.description')}
        actions={
          <Button variant="secondary" onClick={reload} disabled={loading}>
            <RefreshIcon className="size-4" />
            {t('common.refresh')}
          </Button>
        }
      />
      {error && (
        <div className="mb-4">
          <Alert tone="danger">{errorMessage(error, t)}</Alert>
        </div>
      )}
      <Card padded={false}>
        {loading && !data ? (
          <div className="flex justify-center py-12 text-primary">
            <Spinner className="size-6" />
          </div>
        ) : items.length === 0 ? (
          <EmptyState icon={<ActivityIcon />} title={t('activity.empty')} description={t('activity.emptyDescription')} />
        ) : (
          <ol className="px-5 py-2">
            {items.map((item, index) => {
              const [Icon, tone] = STYLE[item.type] || [ActivityIcon, 'bg-surface-2 text-muted']
              const [title, detail] = describe(t, item)
              return (
                <li key={item.id} className="relative flex gap-4 py-3">
                  {index < items.length - 1 && <span className="absolute left-[19px] top-14 bottom-0 w-px bg-line" aria-hidden="true" />}
                  <span className={'flex size-10 shrink-0 items-center justify-center rounded-full ' + tone}>
                    <Icon className="size-[18px]" />
                  </span>
                  <div className="min-w-0 flex-1 pt-0.5">
                    <div className="flex flex-col gap-0.5 sm:flex-row sm:items-baseline sm:justify-between sm:gap-4">
                      <p className="font-medium text-ink">{title}</p>
                      <time
                        dateTime={item.occurredAt}
                        title={formatDateTime(item.occurredAt, i18n.language)}
                        className="shrink-0 text-xs text-subtle"
                      >
                        {relativeTime(item.occurredAt, i18n.language)}
                      </time>
                    </div>
                    {detail && <p className="mt-0.5 break-words text-sm text-muted">{detail}</p>}
                    <p className="mt-1 text-xs text-subtle">
                      {t('activity.from', { device: deviceLabel(t, item), ip: item.ipAddress || '—' })}
                    </p>
                  </div>
                </li>
              )
            })}
          </ol>
        )}
        <Pagination page={data?.page ?? 0} totalPages={data?.totalPages} totalElements={data?.totalElements} onChange={setPage} />
      </Card>
    </>
  )
}
