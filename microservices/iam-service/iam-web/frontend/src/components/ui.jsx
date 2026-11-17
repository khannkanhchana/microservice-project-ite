import { createContext, useCallback, useContext, useEffect, useId, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { cva } from 'class-variance-authority'
import { ChevronDown, ChevronLeft, ChevronRight, CircleAlert, CircleCheck, Info, LoaderCircle, TriangleAlert, X } from 'lucide-react'
import { EyeIcon, EyeOffIcon } from './icons'
import { cn } from '../lib/utils'

/*
 * Core UI primitives in the style of shadcn/ui: variants with class-variance-authority, classes merged with cn(),
 * Lucide icons, quiet surfaces (1px borders, no decorative shadows) and a visible focus ring on every control.
 */

const focusRing = 'outline-none focus-visible:ring-[3px] focus-visible:ring-ring'

// ---- Button -------------------------------------------------------------------------------------

export const buttonVariants = cva(
  cn(
    'inline-flex shrink-0 items-center justify-center gap-2 rounded-lg font-medium whitespace-nowrap select-none',
    'transition-[color,background-color,border-color,box-shadow] duration-150',
    'disabled:pointer-events-none disabled:opacity-50 [&_svg]:pointer-events-none [&_svg]:shrink-0',
    focusRing,
  ),
  {
    variants: {
      variant: {
        primary: 'bg-primary text-on-primary hover:bg-primary-hover',
        secondary: 'border border-line bg-surface text-ink hover:bg-surface-2',
        outline: 'border border-line bg-transparent text-ink hover:bg-surface-2',
        subtle: 'bg-surface-2 text-ink hover:bg-line',
        danger: 'bg-danger text-white hover:bg-danger-hover focus-visible:ring-danger/30',
        'danger-outline': 'border border-line bg-surface text-danger hover:border-danger/40 hover:bg-danger-soft',
        ghost: 'text-muted hover:bg-surface-2 hover:text-ink',
        link: 'h-auto px-0 text-primary underline-offset-4 hover:underline',
      },
      size: {
        sm: 'h-8 px-3 text-[13px] [&_svg:not([class*=size-])]:size-3.5',
        md: 'h-9 px-4 text-sm [&_svg:not([class*=size-])]:size-4',
        lg: 'h-11 px-5 text-sm [&_svg:not([class*=size-])]:size-4',
        icon: 'size-9 [&_svg:not([class*=size-])]:size-4',
      },
    },
    defaultVariants: { variant: 'primary', size: 'md' },
  },
)

export function Button({ variant, size, loading, disabled, className, children, ...props }) {
  return (
    <button
      type="button"
      disabled={disabled || loading}
      aria-busy={loading || undefined}
      className={cn(buttonVariants({ variant, size }), className)}
      {...props}
    >
      {loading && <Spinner className="size-4" />}
      {children}
    </button>
  )
}

export function Spinner({ className = 'size-5' }) {
  return <LoaderCircle aria-hidden="true" className={cn('animate-spin text-current', className)} />
}

/** Placeholder block shown while content loads. */
export function Skeleton({ className }) {
  return <div aria-hidden="true" className={cn('animate-pulse rounded-md bg-surface-2', className)} />
}

// ---- Layout pieces ------------------------------------------------------------------------------

export function PageHeader({ title, description, actions }) {
  return (
    <div className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
      <div className="min-w-0">
        <h1 className="text-2xl font-semibold tracking-tight text-ink">{title}</h1>
        {description && <p className="mt-1.5 max-w-2xl text-sm leading-relaxed text-muted">{description}</p>}
      </div>
      {actions && <div className="flex flex-wrap items-center gap-2">{actions}</div>}
    </div>
  )
}

/**
 * Content section. Padded cards hold forms and text; unpadded cards hold lists and tables, which run edge to
 * edge under a header separated by a hairline.
 */
export function Card({ title, description, actions, children, className, padded = true }) {
  const hasHeader = Boolean(title || actions)
  return (
    <section className={cn('rounded-xl border border-line bg-surface', className)}>
      {hasHeader && (
        <header
          className={cn(
            'flex flex-col gap-3 px-5 sm:flex-row sm:items-center sm:justify-between sm:px-6',
            padded ? 'pt-5 sm:pt-6' : 'border-b border-line py-4',
          )}
        >
          <div className="min-w-0">
            {title && <h2 className="text-[15px] font-semibold leading-6 text-ink">{title}</h2>}
            {description && <p className="mt-0.5 text-sm text-muted">{description}</p>}
          </div>
          {actions && <div className="flex flex-wrap items-center gap-2">{actions}</div>}
        </header>
      )}
      <div className={padded ? cn('px-5 pb-5 sm:px-6 sm:pb-6', hasHeader ? 'pt-5' : 'pt-5 sm:pt-6') : ''}>{children}</div>
    </section>
  )
}

export function EmptyState({ icon, title, description, action }) {
  return (
    <div className="flex flex-col items-center px-6 py-14 text-center">
      {icon && (
        <div className="mb-4 flex size-11 items-center justify-center rounded-xl border border-line bg-surface-2 text-subtle [&_svg]:size-5">
          {icon}
        </div>
      )}
      <p className="text-sm font-medium text-ink">{title}</p>
      {description && <p className="mt-1 max-w-sm text-sm text-muted">{description}</p>}
      {action && <div className="mt-5">{action}</div>}
    </div>
  )
}

const badgeVariants = cva(
  'inline-flex items-center gap-1 rounded-md px-2 py-0.5 text-xs font-medium whitespace-nowrap ring-1 ring-inset [&_svg]:size-3',
  {
    variants: {
      tone: {
        neutral: 'bg-surface-2 text-muted ring-line',
        primary: 'bg-primary-soft text-primary ring-primary/20',
        success: 'bg-success-soft text-success ring-success/20',
        danger: 'bg-danger-soft text-danger ring-danger/20',
        warning: 'bg-warning-soft text-warning ring-warning/25',
      },
    },
    defaultVariants: { tone: 'neutral' },
  },
)

export function Badge({ tone, className, children }) {
  return <span className={cn(badgeVariants({ tone }), className)}>{children}</span>
}

const ALERT_TONES = {
  info: ['border-primary/20 bg-primary-soft/60', 'text-primary', Info],
  success: ['border-success/20 bg-success-soft', 'text-success', CircleCheck],
  danger: ['border-danger/25 bg-danger-soft', 'text-danger', CircleAlert],
  warning: ['border-warning/25 bg-warning-soft', 'text-warning', TriangleAlert],
}

export function Alert({ tone = 'info', title, children, onDismiss }) {
  const { t } = useTranslation()
  const [surface, accent, Icon] = ALERT_TONES[tone]
  return (
    <div role={tone === 'danger' ? 'alert' : 'status'} className={cn('flex gap-3 rounded-lg border px-4 py-3 text-sm text-ink', surface)}>
      <Icon aria-hidden="true" className={cn('mt-0.5 size-4 shrink-0', accent)} />
      <div className="min-w-0 flex-1 leading-relaxed">
        {title && <p className={cn('font-medium', accent)}>{title}</p>}
        {children && <div className={cn(title ? 'mt-0.5 text-muted' : '')}>{children}</div>}
      </div>
      {onDismiss && (
        <button
          type="button"
          onClick={onDismiss}
          aria-label={t('common.dismiss')}
          className={cn('-m-1 shrink-0 rounded-md p-1 text-subtle transition-colors hover:bg-surface/60 hover:text-ink', focusRing)}
        >
          <X aria-hidden="true" className="size-4" />
        </button>
      )}
    </div>
  )
}

export function Avatar({ src, name, size = 'md' }) {
  const [failed, setFailed] = useState(false)
  useEffect(() => setFailed(false), [src])
  const sizes = { sm: 'size-8 text-xs', md: 'size-10 text-sm', lg: 'size-16 text-xl', xl: 'size-20 text-2xl' }
  const initials = (name || '?')
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase())
    .join('')
  return src && !failed ? (
    <img src={src} alt="" onError={() => setFailed(true)} className={cn('shrink-0 rounded-full object-cover ring-1 ring-line', sizes[size])} />
  ) : (
    <span
      aria-hidden="true"
      className={cn('flex shrink-0 items-center justify-center rounded-full bg-surface-2 font-medium text-muted ring-1 ring-line', sizes[size])}
    >
      {initials || '?'}
    </span>
  )
}

// ---- Forms --------------------------------------------------------------------------------------

export function Field({ label, hint, error, children, className }) {
  const id = useId()
  const describedBy = error ? `${id}-error` : hint ? `${id}-hint` : undefined
  return (
    <div className={className}>
      {label && (
        <label htmlFor={id} className="mb-2 block text-sm font-medium leading-none text-ink">
          {label}
        </label>
      )}
      {children({ id, 'aria-describedby': describedBy, 'aria-invalid': error ? true : undefined })}
      {error ? (
        <p id={`${id}-error`} className="mt-1.5 flex items-center gap-1.5 text-[13px] text-danger">
          <CircleAlert aria-hidden="true" className="size-3.5 shrink-0" />
          {error}
        </p>
      ) : (
        hint && (
          <p id={`${id}-hint`} className="mt-1.5 text-xs leading-relaxed text-subtle">
            {hint}
          </p>
        )
      )}
    </div>
  )
}

const inputClass = cn(
  'block w-full min-w-0 rounded-lg border border-line bg-surface px-3 text-sm text-ink',
  'placeholder:text-subtle transition-[border-color,box-shadow] duration-150',
  'hover:border-subtle/50 focus-visible:border-primary',
  'disabled:cursor-not-allowed disabled:bg-surface-2 disabled:text-muted disabled:hover:border-line',
  'aria-[invalid=true]:border-danger aria-[invalid=true]:focus-visible:ring-danger/25',
  focusRing,
)

export const Input = ({ className, ...props }) => <input className={cn(inputClass, 'h-9 py-1', className)} {...props} />

/** Native select (keeps keyboard and mobile pickers) styled like the other inputs. */
export const Select = ({ className, children, ...props }) => (
  <div className={cn('relative', className)}>
    <select className={cn(inputClass, 'h-9 appearance-none py-1 pr-9')} {...props}>
      {children}
    </select>
    <ChevronDown aria-hidden="true" className="pointer-events-none absolute top-1/2 right-3 size-4 -translate-y-1/2 text-subtle" />
  </div>
)

export const Textarea = ({ className, ...props }) => (
  <textarea className={cn(inputClass, 'min-h-20 py-2 leading-relaxed', className)} rows={3} {...props} />
)

export function PasswordInput(props) {
  const { t } = useTranslation()
  const [visible, setVisible] = useState(false)
  return (
    <div className="relative">
      <Input type={visible ? 'text' : 'password'} className="pr-10" {...props} />
      <button
        type="button"
        onClick={() => setVisible((v) => !v)}
        className={cn('absolute inset-y-0 right-0 flex w-10 items-center justify-center rounded-r-lg text-subtle transition-colors hover:text-ink', focusRing)}
        aria-label={visible ? t('common.hidePassword') : t('common.showPassword')}
      >
        {visible ? <EyeOffIcon className="size-4" /> : <EyeIcon className="size-4" />}
      </button>
    </div>
  )
}

export function Checkbox({ label, description, ...props }) {
  const id = useId()
  return (
    <div className="flex gap-3">
      <input
        id={id}
        type="checkbox"
        className="mt-0.5 size-4 shrink-0 cursor-pointer rounded border-line accent-[var(--c-primary)] disabled:cursor-not-allowed"
        {...props}
      />
      <label htmlFor={id} className="cursor-pointer text-sm leading-snug">
        <span className="font-medium text-ink">{label}</span>
        {description && <span className="mt-0.5 block text-muted">{description}</span>}
      </label>
    </div>
  )
}

// ---- Dialog -------------------------------------------------------------------------------------

export function Dialog({ open, onClose, title, children, footer, size = 'md' }) {
  const { t } = useTranslation()
  const ref = useRef(null)
  const titleId = useId()
  useEffect(() => {
    const dialog = ref.current
    if (!dialog) return
    if (open && !dialog.open) dialog.showModal()
    if (!open && dialog.open) dialog.close()
  }, [open])
  return (
    <dialog
      ref={ref}
      onClose={onClose}
      onCancel={onClose}
      aria-labelledby={titleId}
      className={cn(
        'm-auto w-[calc(100%-2rem)] rounded-xl border border-line bg-surface p-0 text-ink shadow-xl shadow-black/10',
        { md: 'max-w-md', lg: 'max-w-2xl', xl: 'max-w-4xl' }[size],
      )}
    >
      {open && (
        <div className="animate-in">
          <div className="flex items-start justify-between gap-4 px-6 pt-5">
            <h2 id={titleId} className="text-base font-semibold tracking-tight">{title}</h2>
            <button
              type="button"
              onClick={onClose}
              aria-label={t('common.close', { defaultValue: 'Close' })}
              className={cn('-mt-0.5 -mr-2 rounded-md p-1.5 text-subtle transition-colors hover:bg-surface-2 hover:text-ink', focusRing)}
            >
              <X aria-hidden="true" className="size-4" />
            </button>
          </div>
          <div className="max-h-[70vh] overflow-y-auto px-6 pt-2 pb-1 text-sm leading-relaxed text-muted">{children}</div>
          {footer && <div className="mt-5 flex flex-col-reverse gap-2 border-t border-line px-6 py-4 sm:flex-row sm:justify-end">{footer}</div>}
        </div>
      )}
    </dialog>
  )
}

/** Confirmation dialog for destructive actions; `onConfirm` may be async. */
export function ConfirmDialog({ open, onClose, onConfirm, title, message, confirmLabel, tone = 'danger' }) {
  const { t } = useTranslation()
  const [busy, setBusy] = useState(false)
  const confirm = async () => {
    setBusy(true)
    try {
      await onConfirm()
      onClose()
    } finally {
      setBusy(false)
    }
  }
  return (
    <Dialog
      open={open}
      onClose={busy ? () => {} : onClose}
      title={title}
      footer={
        <>
          <Button variant="secondary" onClick={onClose} disabled={busy}>
            {t('common.cancel')}
          </Button>
          <Button variant={tone === 'danger' ? 'danger' : 'primary'} onClick={confirm} loading={busy}>
            {confirmLabel}
          </Button>
        </>
      }
    >
      {message}
    </Dialog>
  )
}

// ---- Pagination ---------------------------------------------------------------------------------

export function Pagination({ page, totalPages, totalElements, onChange }) {
  const { t } = useTranslation()
  if (!totalPages || totalPages <= 1) return null
  return (
    <nav aria-label="Pagination" className="flex items-center justify-between gap-3 border-t border-line px-5 py-3 text-sm sm:px-6">
      <span className="text-muted tabular-nums">{t('common.pageOf', { page: page + 1, total: totalPages, count: totalElements })}</span>
      <div className="flex gap-1.5">
        <Button size="sm" variant="secondary" disabled={page <= 0} onClick={() => onChange(page - 1)}>
          <ChevronLeft aria-hidden="true" />
          <span className="hidden sm:inline">{t('common.previous')}</span>
        </Button>
        <Button size="sm" variant="secondary" disabled={page + 1 >= totalPages} onClick={() => onChange(page + 1)}>
          <span className="hidden sm:inline">{t('common.next')}</span>
          <ChevronRight aria-hidden="true" />
        </Button>
      </div>
    </nav>
  )
}

// ---- Toasts -------------------------------------------------------------------------------------

const ToastContext = createContext(() => {})

const TOAST_ICONS = {
  success: [CircleCheck, 'text-success'],
  danger: [CircleAlert, 'text-danger'],
  info: [Info, 'text-primary'],
  warning: [TriangleAlert, 'text-warning'],
}

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([])
  const dismiss = useCallback((id) => setToasts((current) => current.filter((toast) => toast.id !== id)), [])
  const push = useCallback(
    (message, tone = 'success') => {
      const id = Math.random().toString(36).slice(2)
      setToasts((current) => [...current.slice(-3), { id, message, tone }])
      setTimeout(() => dismiss(id), 4500)
    },
    [dismiss],
  )
  return (
    <ToastContext.Provider value={push}>
      {children}
      <div
        aria-live="polite"
        className="pointer-events-none fixed inset-x-0 bottom-0 z-[60] flex flex-col items-center gap-2 p-4 sm:items-end sm:p-6"
      >
        {toasts.map((toast) => {
          const [Icon, color] = TOAST_ICONS[toast.tone] || TOAST_ICONS.success
          return (
            <div
              key={toast.id}
              role={toast.tone === 'danger' ? 'alert' : 'status'}
              className="pointer-events-auto flex w-full max-w-sm animate-in items-start gap-3 rounded-lg border border-line bg-surface px-4 py-3 text-sm text-ink shadow-lg shadow-black/5"
            >
              <Icon aria-hidden="true" className={cn('mt-0.5 size-4 shrink-0', color)} />
              <p className="min-w-0 flex-1 leading-relaxed">{toast.message}</p>
              <button
                type="button"
                onClick={() => dismiss(toast.id)}
                aria-label="Dismiss"
                className={cn('-m-1 rounded-md p-1 text-subtle transition-colors hover:text-ink', focusRing)}
              >
                <X aria-hidden="true" className="size-3.5" />
              </button>
            </div>
          )
        })}
      </div>
    </ToastContext.Provider>
  )
}

export const useToast = () => useContext(ToastContext)
