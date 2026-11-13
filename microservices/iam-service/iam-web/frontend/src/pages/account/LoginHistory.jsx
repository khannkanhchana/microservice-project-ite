import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { accountApi } from '../../api/account'
import { Alert, Badge, Button, Card, EmptyState, PageHeader, Pagination, Spinner } from '../../components/ui'
import { CheckIcon, CloseIcon, HistoryIcon, RefreshIcon } from '../../components/icons'
import { errorMessage, formatDateTime } from '../../lib/format'
import { useResource } from '../../lib/useResource'
import { deviceLabel } from '../../lib/devices'

const FILTERS = ['all', 'success', 'failed']
const PAGE_SIZE = 15

function failureReason(t, reason) {
  if (!reason) return null
  const [code, ...rest] = reason.split(': ')
  const label = t(`failure.${code}`, { defaultValue: '' }) || t('failure.other')
  return rest.length ? `${label}: ${rest.join(': ')}` : label
}

export default function LoginHistory() {
  const { t, i18n } = useTranslation()
  const [status, setStatus] = useState('all')
  const [page, setPage] = useState(0)
  const { data, loading, error, reload } = useResource(
    () => accountApi.loginHistory({ status, page, size: PAGE_SIZE }),
    [status, page],
  )
  const rows = data?.content || []
  const hasFailures = rows.some((row) => !row.success)

  const method = (row) => t(`methods.${row.method}`, { defaultValue: row.method })

  return (
    <>
      <PageHeader
        title={t('history.title')}
        description={t('history.description')}
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
      {hasFailures && status !== 'success' && (
        <div className="mb-4">
          <Alert tone="warning">{t('history.failedNotice')}</Alert>
        </div>
      )}

      <Card padded={false}>
        <div role="tablist" className="flex gap-1 border-b border-line px-3 pt-3">
          {FILTERS.map((filter) => (
            <button
              key={filter}
              role="tab"
              type="button"
              aria-selected={status === filter}
              onClick={() => {
                setStatus(filter)
                setPage(0)
              }}
              className={
                '-mb-px border-b-2 px-3 pb-2.5 pt-1 text-sm font-medium transition-colors ' +
                (status === filter ? 'border-primary text-primary' : 'border-transparent text-muted hover:text-ink')
              }
            >
              {t(`history.${filter}`)}
            </button>
          ))}
        </div>

        {loading && !data ? (
          <div className="flex justify-center py-12 text-primary">
            <Spinner className="size-6" />
          </div>
        ) : rows.length === 0 ? (
          <EmptyState icon={<HistoryIcon />} title={status === 'all' ? t('history.empty') : t('history.emptyFiltered')} />
        ) : (
          <>
            {/* Wide screens: table */}
            <div className="hidden overflow-x-auto md:block">
              <table className="w-full text-left text-sm">
                <thead className="bg-surface-2/60 text-xs uppercase tracking-wide text-subtle">
                  <tr>
                    <th className="px-5 py-3 font-medium">{t('history.status')}</th>
                    <th className="px-3 py-3 font-medium">{t('history.time')}</th>
                    <th className="px-3 py-3 font-medium">{t('history.method')}</th>
                    <th className="px-3 py-3 font-medium">{t('history.device')}</th>
                    <th className="px-3 py-3 font-medium">{t('history.ip')}</th>
                    <th className="px-5 py-3 font-medium">{t('history.location')}</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-line">
                  {rows.map((row) => (
                    <tr key={row.id} className="align-top">
                      <td className="px-5 py-3">
                        <StatusBadge success={row.success} t={t} />
                        {!row.success && <p className="mt-1 max-w-48 text-xs text-danger">{failureReason(t, row.failureReason)}</p>}
                      </td>
                      <td className="whitespace-nowrap px-3 py-3 text-ink">{formatDateTime(row.attemptedAt, i18n.language)}</td>
                      <td className="px-3 py-3 text-muted">{method(row)}</td>
                      <td className="px-3 py-3 text-muted">{deviceLabel(t, row)}</td>
                      <td className="px-3 py-3 font-mono text-xs text-muted">{row.ipAddress || '—'}</td>
                      <td className="px-5 py-3 text-muted">{row.location || t('common.unknown')}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {/* Narrow screens: stacked list */}
            <ul className="divide-y divide-line md:hidden">
              {rows.map((row) => (
                <li key={row.id} className="px-5 py-4">
                  <div className="flex items-center justify-between gap-3">
                    <StatusBadge success={row.success} t={t} />
                    <span className="text-xs text-subtle">{formatDateTime(row.attemptedAt, i18n.language)}</span>
                  </div>
                  {!row.success && <p className="mt-1.5 text-sm text-danger">{failureReason(t, row.failureReason)}</p>}
                  <p className="mt-1.5 text-sm text-ink">
                    {deviceLabel(t, row)} · {method(row)}
                  </p>
                  <p className="mt-0.5 text-xs text-muted">
                    <span className="font-mono">{row.ipAddress || '—'}</span> · {row.location || t('common.unknown')}
                  </p>
                </li>
              ))}
            </ul>
          </>
        )}
        <Pagination page={data?.page ?? 0} totalPages={data?.totalPages} totalElements={data?.totalElements} onChange={setPage} />
      </Card>
    </>
  )
}

function StatusBadge({ success, t }) {
  return success ? (
    <Badge tone="success">
      <CheckIcon className="size-3" /> {t('history.success')}
    </Badge>
  ) : (
    <Badge tone="danger">
      <CloseIcon className="size-3" /> {t('history.failed')}
    </Badge>
  )
}
