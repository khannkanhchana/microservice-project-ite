import { useEffect, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Search, X } from 'lucide-react'
import { Badge, Input, Skeleton } from './ui'
import { cn } from '../lib/utils'

/** Responsive table: scrolls horizontally on small screens instead of overflowing the page. */
export function Table({ head, children, empty, loading }) {
  return (
    <div className="overflow-x-auto">
      <table className="w-full min-w-[640px] text-left text-sm">
        <thead className="border-b border-line">
          <tr>
            {head.map((label, i) => (
              <th key={i} scope="col" className="h-10 px-5 text-xs font-medium whitespace-nowrap text-muted">
                {label}
              </th>
            ))}
          </tr>
        </thead>
        {!loading && (
          <tbody className="divide-y divide-line [&>tr]:transition-colors [&>tr:hover]:bg-surface-2/60">{children}</tbody>
        )}
      </table>
      {loading && (
        <div className="divide-y divide-line" aria-busy="true">
          {Array.from({ length: 5 }, (_, i) => (
            <div key={i} className="flex items-center gap-4 px-5 py-4">
              <Skeleton className="size-8 rounded-full" />
              <div className="flex-1 space-y-2">
                <Skeleton className="h-3.5 w-1/3" />
                <Skeleton className="h-3 w-1/5" />
              </div>
              <Skeleton className="h-5 w-16" />
            </div>
          ))}
        </div>
      )}
      {!loading && empty}
    </div>
  )
}

export const Td = ({ className, children, ...props }) => (
  <td className={cn('px-5 py-3.5 align-middle text-ink', className)} {...props}>
    {children}
  </td>
)

/** Page-level loading placeholder shaped like a header, a row of tiles and a content block. */
export function LoadingBlock() {
  return (
    <div className="space-y-6" aria-busy="true" aria-live="polite">
      <div className="space-y-2">
        <Skeleton className="h-7 w-56" />
        <Skeleton className="h-4 w-80 max-w-full" />
      </div>
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {Array.from({ length: 4 }, (_, i) => <Skeleton key={i} className="h-[104px] rounded-xl" />)}
      </div>
      <Skeleton className="h-64 rounded-xl" />
    </div>
  )
}

/** Search field that applies on Enter or after typing stops. */
export function SearchBox({ value, onChange, placeholder, className }) {
  const { t } = useTranslation()
  const [draft, setDraft] = useState(value || '')
  const timer = useRef(null)
  useEffect(() => () => clearTimeout(timer.current), [])
  const update = (next, immediate) => {
    setDraft(next)
    clearTimeout(timer.current)
    if (immediate) onChange(next)
    else timer.current = setTimeout(() => onChange(next), 350)
  }
  return (
    <div className={cn('relative w-full sm:max-w-xs', className)}>
      <Search aria-hidden="true" className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-subtle" />
      <Input
        type="search"
        value={draft}
        placeholder={placeholder}
        aria-label={placeholder}
        onChange={(event) => update(event.target.value)}
        onKeyDown={(event) => event.key === 'Enter' && update(draft, true)}
        className="pl-9 pr-8 [&::-webkit-search-cancel-button]:hidden"
      />
      {draft && (
        <button
          type="button"
          onClick={() => update('', true)}
          aria-label={t('common.clear', { defaultValue: 'Clear' })}
          className="absolute top-1/2 right-2 -translate-y-1/2 rounded p-0.5 text-subtle transition-colors hover:text-ink"
        >
          <X aria-hidden="true" className="size-3.5" />
        </button>
      )}
    </div>
  )
}

export function Toolbar({ children }) {
  return <div className="flex flex-col gap-2 border-b border-line px-5 py-3 sm:flex-row sm:flex-wrap sm:items-center">{children}</div>
}

const STAT_TONES = {
  neutral: 'text-subtle',
  success: 'text-success',
  danger: 'text-danger',
  warning: 'text-warning',
  primary: 'text-primary',
}

/** A number with a label. Colour is used only for the icon and for values that need attention. */
export function StatTile({ label, value, hint, tone = 'neutral', icon: Icon }) {
  const alarming = tone === 'danger' || tone === 'warning'
  return (
    <div className="h-full rounded-xl border border-line bg-surface p-5 transition-colors hover:border-subtle/40">
      <div className="flex items-center justify-between gap-3">
        <p className="text-sm text-muted">{label}</p>
        {Icon && <Icon className={cn('size-4 shrink-0', STAT_TONES[tone])} />}
      </div>
      <p className={cn('mt-3 text-[28px] leading-none font-semibold tracking-tight tabular-nums', alarming ? STAT_TONES[tone] : 'text-ink')}>
        {value ?? '—'}
      </p>
      {hint && <p className="mt-2 text-xs text-subtle">{hint}</p>}
    </div>
  )
}

export function Tabs({ tabs, active, onChange }) {
  return (
    <div className="mb-6 flex gap-6 overflow-x-auto border-b border-line" role="tablist">
      {tabs.map((tab) => (
        <button
          key={tab.id}
          type="button"
          role="tab"
          aria-selected={active === tab.id}
          onClick={() => onChange(tab.id)}
          className={cn(
            '-mb-px flex items-center gap-2 border-b-2 pb-3 pt-1 text-sm font-medium whitespace-nowrap transition-colors',
            'outline-none focus-visible:text-ink focus-visible:ring-[3px] focus-visible:ring-ring rounded-t-sm',
            active === tab.id ? 'border-ink text-ink' : 'border-transparent text-muted hover:text-ink',
          )}
        >
          {tab.label}
          {tab.count != null && (
            <span className="rounded-md bg-surface-2 px-1.5 py-0.5 text-xs font-medium text-muted tabular-nums">{tab.count}</span>
          )}
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
        <button
          type="button"
          onClick={add}
          disabled={!draft.trim()}
          className="h-9 shrink-0 rounded-lg border border-line bg-surface px-3 text-sm font-medium text-ink transition-colors outline-none hover:bg-surface-2 focus-visible:ring-[3px] focus-visible:ring-ring disabled:opacity-50"
        >
          {t('admin.common.add')}
        </button>
      </div>
      {values.length > 0 && (
        <ul className="mt-2 flex flex-wrap gap-1.5">
          {values.map((value) => (
            <li key={value} className="flex max-w-full items-center gap-1 rounded-md border border-line bg-surface-2 py-0.5 pr-0.5 pl-2 font-mono text-xs text-ink">
              <span className="truncate">{value}</span>
              <button
                type="button"
                onClick={() => onChange(values.filter((v) => v !== value))}
                className="rounded p-0.5 text-subtle transition-colors hover:bg-surface hover:text-danger"
                aria-label={t('admin.common.remove', { value })}
              >
                <X aria-hidden="true" className="size-3.5" />
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
          <legend className="px-1 text-xs font-medium text-muted">{group}</legend>
          <div className="flex flex-col gap-2">
            {entries.map((item) => (
              <label key={item.uuid} className="flex cursor-pointer items-start gap-2.5 text-sm text-ink has-disabled:cursor-not-allowed has-disabled:opacity-60">
                <input
                  type="checkbox"
                  className="mt-0.5 size-4 shrink-0 accent-[var(--c-primary)]"
                  checked={selected.includes(item.uuid)}
                  disabled={disabled?.(item)}
                  onChange={() => onToggle(item.uuid)}
                />
                <span className="min-w-0 break-words">{label(item)}</span>
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
      <div className="flex h-40 items-end gap-2 border-b border-line" role="img" aria-label={labels.aria}>
        {data.map((day) => (
          <div
            key={day.date}
            className="group flex h-full flex-1 flex-col justify-end gap-px"
            title={`${day.date}: ${day.successful} ${labels.successful} · ${day.failed} ${labels.failed}`}
          >
            {day.failed > 0 && (
              <div className="w-full rounded-t-[3px] bg-danger/80 transition-opacity group-hover:opacity-100" style={{ height: `${(day.failed / max) * 100}%` }} />
            )}
            <div
              className={cn('w-full bg-primary/85 transition-colors group-hover:bg-primary', day.failed > 0 ? '' : 'rounded-t-[3px]')}
              style={{ height: `${(day.successful / max) * 100}%` }}
            />
          </div>
        ))}
      </div>
      <div className="mt-2 flex gap-2 text-xs text-subtle">
        {data.map((day) => (
          <span key={day.date} className="flex-1 text-center tabular-nums">
            {day.date.slice(5)}
          </span>
        ))}
      </div>
      <div className="mt-4 flex gap-5 text-xs text-muted">
        <span className="flex items-center gap-1.5">
          <span className="size-2 rounded-full bg-primary" />
          {labels.successful}
        </span>
        <span className="flex items-center gap-1.5">
          <span className="size-2 rounded-full bg-danger" />
          {labels.failed}
        </span>
      </div>
    </div>
  )
}
