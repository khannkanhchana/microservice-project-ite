import { createContext, useCallback, useContext, useEffect, useId, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { AlertIcon, CheckIcon, CloseIcon, EyeIcon, EyeOffIcon, InfoIcon } from './icons'

const cx = (...classes) => classes.filter(Boolean).join(' ')

// ---- Buttons ------------------------------------------------------------------------------------

const variants = {
  primary: 'bg-primary text-on-primary hover:bg-primary-hover',
  secondary: 'bg-surface text-ink border border-line hover:bg-surface-2',
  danger: 'bg-danger text-white hover:bg-danger-hover',
  'danger-outline': 'bg-surface text-danger border border-line hover:bg-danger-soft',
  ghost: 'text-muted hover:text-ink hover:bg-surface-2',
}

export function Button({ variant = 'primary', size = 'md', loading, disabled, className, children, ...props }) {
  return (
    <button
      type="button"
      disabled={disabled || loading}
      className={cx(
        'inline-flex items-center justify-center gap-2 rounded-lg font-medium transition-colors',
        'disabled:cursor-not-allowed disabled:opacity-60 focus-visible:ring-4 focus-visible:ring-ring',
        size === 'sm' ? 'h-8 px-3 text-sm' : 'h-10 px-4 text-sm',
        variants[variant],
        className,
      )}
      {...props}
    >
      {loading && <Spinner className="size-4" />}
      {children}
    </button>
  )
}

export function Spinner({ className = 'size-5' }) {
  return (
    <svg className={cx('animate-spin', className)} viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <circle cx="12" cy="12" r="9" stroke="currentColor" strokeOpacity="0.25" strokeWidth="3" />
      <path d="M21 12a9 9 0 00-9-9" stroke="currentColor" strokeWidth="3" strokeLinecap="round" />
    </svg>
  )
}

// ---- Layout pieces ------------------------------------------------------------------------------

export function PageHeader({ title, description, actions }) {
  return (
    <div className="mb-6 flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight text-ink">{title}</h1>
        {description && <p className="mt-1 text-sm text-muted">{description}</p>}
      </div>
      {actions && <div className="flex flex-wrap gap-2">{actions}</div>}
    </div>
  )
}

export function Card({ title, description, actions, children, className, padded = true }) {
  return (
    <section className={cx('rounded-xl border border-line bg-surface shadow-sm', className)}>
      {(title || actions) && (
        <header className="flex flex-col gap-3 border-b border-line px-5 py-4 sm:flex-row sm:items-center sm:justify-between">
          <div>
            {title && <h2 className="text-base font-semibold text-ink">{title}</h2>}
            {description && <p className="mt-0.5 text-sm text-muted">{description}</p>}
          </div>
          {actions && <div className="flex flex-wrap gap-2">{actions}</div>}
        </header>
      )}
      <div className={padded ? 'p-5' : ''}>{children}</div>
    </section>
  )
}

export function EmptyState({ icon, title, description, action }) {
  return (
    <div className="flex flex-col items-center px-6 py-12 text-center">
      {icon && <div className="mb-3 rounded-full bg-surface-2 p-3 text-muted">{icon}</div>}
      <p className="font-medium text-ink">{title}</p>
      {description && <p className="mt-1 max-w-sm text-sm text-muted">{description}</p>}
      {action && <div className="mt-4">{action}</div>}
    </div>
  )
}

export function Badge({ tone = 'neutral', children }) {
  const tones = {
    neutral: 'bg-surface-2 text-muted',
    primary: 'bg-primary-soft text-primary',
    success: 'bg-success-soft text-success',
    danger: 'bg-danger-soft text-danger',
    warning: 'bg-warning-soft text-warning',
  }
  return (
    <span className={cx('inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-xs font-medium', tones[tone])}>
      {children}
    </span>
  )
}

export function Alert({ tone = 'info', title, children, onDismiss }) {
  const { t } = useTranslation()
  const tones = {
    info: ['bg-primary-soft text-primary', InfoIcon],
    success: ['bg-success-soft text-success', CheckIcon],
    danger: ['bg-danger-soft text-danger', AlertIcon],
    warning: ['bg-warning-soft text-warning', AlertIcon],
  }
  const [style, Icon] = tones[tone]
  return (
    <div role={tone === 'danger' ? 'alert' : 'status'} className={cx('flex gap-3 rounded-lg px-4 py-3 text-sm', style)}>
      <Icon className="mt-0.5 size-4 shrink-0" />
      <div className="min-w-0 flex-1">
        {title && <p className="font-semibold">{title}</p>}
        {children && <div className={title ? 'mt-0.5' : ''}>{children}</div>}
      </div>
      {onDismiss && (
        <button type="button" onClick={onDismiss} aria-label={t('common.dismiss')} className="shrink-0 opacity-70 hover:opacity-100">
          <CloseIcon className="size-4" />
        </button>
      )}
    </div>
  )
}

export function Avatar({ src, name, size = 'md' }) {
  const [failed, setFailed] = useState(false)
  useEffect(() => setFailed(false), [src])
  const sizes = { sm: 'size-8 text-xs', md: 'size-10 text-sm', lg: 'size-20 text-2xl', xl: 'size-24 text-3xl' }
  const initials = (name || '?')
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase())
    .join('')
  return src && !failed ? (
    <img
      src={src}
      alt=""
      onError={() => setFailed(true)}
      className={cx('shrink-0 rounded-full object-cover ring-1 ring-line', sizes[size])}
    />
  ) : (
    <span
      aria-hidden="true"
      className={cx('flex shrink-0 items-center justify-center rounded-full bg-primary-soft font-semibold text-primary', sizes[size])}
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
        <label htmlFor={id} className="mb-1.5 block text-sm font-medium text-ink">
          {label}
        </label>
      )}
      {children({ id, 'aria-describedby': describedBy, 'aria-invalid': error ? true : undefined })}
      {error ? (
        <p id={`${id}-error`} className="mt-1.5 text-sm text-danger">
          {error}
        </p>
      ) : (
        hint && (
          <p id={`${id}-hint`} className="mt-1.5 text-xs text-subtle">
            {hint}
          </p>
        )
      )}
    </div>
  )
}

const inputClass =
  'block w-full rounded-lg border border-line bg-surface px-3 py-2 text-sm text-ink placeholder:text-subtle ' +
  'focus:border-primary focus:outline-none focus:ring-4 focus:ring-ring disabled:bg-surface-2 disabled:text-muted ' +
  'aria-[invalid=true]:border-danger'

export const Input = ({ className, ...props }) => <input className={cx(inputClass, 'h-10', className)} {...props} />

export const Select = ({ className, children, ...props }) => (
  <select className={cx(inputClass, 'h-10', className)} {...props}>
    {children}
  </select>
)

export const Textarea = ({ className, ...props }) => <textarea className={cx(inputClass, className)} rows={3} {...props} />

export function PasswordInput(props) {
  const { t } = useTranslation()
  const [visible, setVisible] = useState(false)
  return (
    <div className="relative">
      <Input type={visible ? 'text' : 'password'} className="pr-10" {...props} />
      <button
        type="button"
        onClick={() => setVisible((v) => !v)}
        className="absolute inset-y-0 right-0 flex w-10 items-center justify-center text-subtle hover:text-ink"
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
      <input id={id} type="checkbox" className="mt-0.5 size-4 rounded border-line accent-[var(--c-primary)]" {...props} />
      <label htmlFor={id} className="text-sm">
        <span className="font-medium text-ink">{label}</span>
        {description && <span className="block text-muted">{description}</span>}
      </label>
    </div>
  )
}

// ---- Dialog -------------------------------------------------------------------------------------

export function Dialog({ open, onClose, title, children, footer, size = 'md' }) {
  const ref = useRef(null)
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
      className={cx(
        'm-auto w-[calc(100%-2rem)] rounded-xl border border-line bg-surface p-0 text-ink shadow-xl backdrop:bg-black/40',
        { md: 'max-w-md', lg: 'max-w-2xl', xl: 'max-w-4xl' }[size],
      )}
    >
      {open && (
        <div>
          <div className="px-5 pt-5">
            <h2 className="text-lg font-semibold">{title}</h2>
            <div className="mt-2 max-h-[70vh] overflow-y-auto pb-1 text-sm text-muted">{children}</div>
          </div>
          <div className="mt-5 flex justify-end gap-2 border-t border-line bg-surface-2/60 px-5 py-3">{footer}</div>
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
    <div className="flex items-center justify-between gap-3 border-t border-line px-5 py-3 text-sm">
      <span className="text-muted">{t('common.pageOf', { page: page + 1, total: totalPages, count: totalElements })}</span>
      <div className="flex gap-2">
        <Button size="sm" variant="secondary" disabled={page <= 0} onClick={() => onChange(page - 1)}>
          {t('common.previous')}
        </Button>
        <Button size="sm" variant="secondary" disabled={page + 1 >= totalPages} onClick={() => onChange(page + 1)}>
          {t('common.next')}
        </Button>
      </div>
    </div>
  )
}

// ---- Toasts -------------------------------------------------------------------------------------

const ToastContext = createContext(() => {})

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([])
  const push = useCallback((message, tone = 'success') => {
    const id = Math.random().toString(36).slice(2)
    setToasts((current) => [...current, { id, message, tone }])
    setTimeout(() => setToasts((current) => current.filter((toast) => toast.id !== id)), 4000)
  }, [])
  return (
    <ToastContext.Provider value={push}>
      {children}
      <div aria-live="polite" className="pointer-events-none fixed inset-x-0 bottom-4 z-50 flex flex-col items-center gap-2 px-4">
        {toasts.map((toast) => (
          <div
            key={toast.id}
            className={cx(
              'pointer-events-auto flex max-w-md items-center gap-2 rounded-lg px-4 py-2.5 text-sm font-medium shadow-lg',
              toast.tone === 'danger' ? 'bg-danger text-white' : 'bg-ink text-bg',
            )}
          >
            {toast.tone === 'danger' ? <AlertIcon className="size-4" /> : <CheckIcon className="size-4" />}
            {toast.message}
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  )
}

export const useToast = () => useContext(ToastContext)
