import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Badge, Input, Spinner } from './ui'
import { CloseIcon } from './icons'

const cx = (...classes) => classes.filter(Boolean).join(' ')

/** Responsive table: scrolls horizontally on small screens instead of overflowing the page. */
export function Table({ head, children, empty, loading }) {
  return (
    <div className="overflow-x-auto">
      <table className="w-full min-w-[640px] text-left text-sm">
        <thead className="border-b border-line bg-surface-2/60 text-xs font-semibold uppercase tracking-wide text-subtle">
          <tr>
            {head.map((label, i) => (
              <th key={i} scope="col" className="px-4 py-2.5 whitespace-nowrap">
                {label}
              </th>
            ))}
          </tr>
        </thead>
        <tbody className="divide-y divide-line">{children}</tbody>
      </table>
      {loading && (
        <div className="flex justify-center py-10 text-primary">
          <Spinner className="size-6" />
        </div>
      )}
      {!loading && empty}
    </div>
  )
}

export const Td = ({ className, children, ...props }) => (
  <td className={cx('px-4 py-3 align-middle text-ink', className)} {...props}>
    {children}
  </td>
)

export function LoadingBlock() {
  return (
    <div className="flex justify-center py-12 text-primary">
      <Spinner className="size-6" />
    </div>
  )
}

/** Search field that applies on Enter or after typing stops. */
export function SearchBox({ value, onChange, placeholder, className }) {
  const [draft, setDraft] = useState(value || '')
  const [timer, setTimer] = useState(null)
  const update = (next) => {
    setDraft(next)
    if (timer) clearTimeout(timer)
    setTimer(setTimeout(() => onChange(next), 350))
  }
  return (
    <Input
      type="search"
      value={draft}
      placeholder={placeholder}
      onChange={(event) => update(event.target.value)}
      onKeyDown={(event) => event.key === 'Enter' && onChange(draft)}
      className={cx('sm:max-w-xs', className)}
    />
  )
}

export function Toolbar({ children }) {
  return <div className="flex flex-col gap-2 border-b border-line px-4 py-3 sm:flex-row sm:flex-wrap sm:items-center">{children}</div>
}

export function StatTile({ label, value, hint, tone = 'neutral' }) {
  const tones = {
    neutral: 'text-ink',
    success: 'text-success',
    danger: 'text-danger',
    warning: 'text-warning',
    primary: 'text-primary',
  }
  return (
    <div className="rounded-xl border border-line bg-surface p-4 shadow-sm">
      <p className="text-xs font-medium uppercase tracking-wide text-subtle">{label}</p>
      <p className={cx('mt-1.5 text-2xl font-semibold tabular-nums', tones[tone])}>{value ?? '—'}</p>
      {hint && <p className="mt-0.5 text-xs text-muted">{hint}</p>}
    </div>
  )
}

export function Tabs({ tabs, active, onChange }) {
  return (
    <div className="mb-5 flex gap-1 overflow-x-auto border-b border-line" role="tablist">
      {tabs.map((tab) => (
        <button
          key={tab.id}
          type="button"
          role="tab"
          aria-selected={active === tab.id}
          onClick={() => onChange(tab.id)}
          className={cx(
            '-mb-px whitespace-nowrap border-b-2 px-3 py-2 text-sm font-medium transition-colors',
            active === tab.id ? 'border-primary text-primary' : 'border-transparent text-muted hover:text-ink',
          )}
        >
          {tab.label}
          {tab.count != null && <span className="ml-1.5 text-xs text-subtle">{tab.count}</span>}
        </button>
      ))}
    </div>
  )
}

/** Account status badges for an admin user row. */
export function UserStatus({ user }) {
  const { t } = useTranslation()
  if (!user.enabled) return <Badge tone="neutral">{t('admin.status.disabled')}</Badge>
  if (user.locked) return <Badge tone="danger">{t('admin.status.locked')}</Badge>
  return <Badge tone="success">{t('admin.status.active')}</Badge>
}

/** Edits a list of strings (redirect URIs, scopes). */
export function ListInput({ values, onChange, placeholder, id }) {
  const { t } = useTranslation()
  const [draft, setDraft] = useState('')
  const add = () => {
    const value = draft.trim()
    if (value && !values.includes(value)) onChange([...values, value])
    setDraft('')
  }
  return (
    <div>
      <div className="flex gap-2">
        <Input
          id={id}
          value={draft}
          placeholder={placeholder}
          onChange={(event) => setDraft(event.target.value)}
          onKeyDown={(event) => {
            if (event.key === 'Enter') {
              event.preventDefault()
              add()
            }
          }}
        />
        <button type="button" onClick={add} className="h-10 shrink-0 rounded-lg border border-line px-3 text-sm font-medium text-ink hover:bg-surface-2">
          {t('admin.common.add')}
        </button>
      </div>
      {values.length > 0 && (
        <ul className="mt-2 flex flex-wrap gap-1.5">
          {values.map((value) => (
            <li key={value} className="flex max-w-full items-center gap-1 rounded-md bg-surface-2 py-1 pl-2 pr-1 font-mono text-xs text-ink">
              <span className="truncate">{value}</span>
              <button
                type="button"
                onClick={() => onChange(values.filter((v) => v !== value))}
                className="rounded p-0.5 text-subtle hover:bg-surface hover:text-danger"
                aria-label={t('admin.common.remove', { value })}
              >
                <CloseIcon className="size-3.5" />
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}

/** Checkbox list grouped by a key (e.g. permissions by resource). */
export function CheckGroups({ items, groupBy, selected, onToggle, label, disabled }) {
  const groups = items.reduce((acc, item) => {
    const key = groupBy(item)
    ;(acc[key] ||= []).push(item)
    return acc
  }, {})
  return (
    <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
      {Object.entries(groups).map(([group, entries]) => (
        <fieldset key={group} className="rounded-lg border border-line p-3">
          <legend className="px-1 text-xs font-semibold uppercase tracking-wide text-subtle">{group}</legend>
          <div className="flex flex-col gap-1.5">
            {entries.map((item) => (
              <label key={item.uuid} className="flex items-start gap-2 text-sm text-ink">
                <input
                  type="checkbox"
                  className="mt-0.5 size-4 accent-[var(--c-primary)]"
                  checked={selected.includes(item.uuid)}
                  disabled={disabled?.(item)}
                  onChange={() => onToggle(item.uuid)}
                />
                <span>{label(item)}</span>
              </label>
            ))}
          </div>
        </fieldset>
      ))}
    </div>
  )
}

/** Small accessible bar chart: successful vs failed sign-ins per day. */
export function LoginBars({ data, labels }) {
  const max = Math.max(1, ...data.map((d) => d.successful + d.failed))
  return (
    <div>
      <div className="flex h-36 items-end gap-2" role="img" aria-label={labels.aria}>
        {data.map((day) => (
          <div key={day.date} className="flex h-full flex-1 flex-col justify-end" title={`${day.date}: ${day.successful} / ${day.failed}`}>
            <div className="w-full rounded-t-sm bg-danger" style={{ height: `${(day.failed / max) * 100}%` }} />
            <div className="w-full bg-primary" style={{ height: `${(day.successful / max) * 100}%` }} />
          </div>
        ))}
      </div>
      <div className="mt-1.5 flex gap-2 text-[11px] text-subtle">
        {data.map((day) => (
          <span key={day.date} className="flex-1 text-center tabular-nums">
            {day.date.slice(5)}
          </span>
        ))}
      </div>
      <div className="mt-3 flex gap-4 text-xs text-muted">
        <span className="flex items-center gap-1.5">
          <span className="size-2.5 rounded-sm bg-primary" />
          {labels.successful}
        </span>
        <span className="flex items-center gap-1.5">
          <span className="size-2.5 rounded-sm bg-danger" />
          {labels.failed}
        </span>
      </div>
    </div>
  )
}
