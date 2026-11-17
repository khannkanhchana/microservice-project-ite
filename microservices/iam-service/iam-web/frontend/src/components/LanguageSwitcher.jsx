import { useEffect, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Check, ChevronDown } from 'lucide-react'
import { accountApi } from '../api/account'
import { useAuth } from '../auth/AuthContext'
import { applyLanguage, LANGUAGES } from '../i18n'
import { cn } from '../lib/utils'

function Flag({ language, className }) {
  return (
    <img
      src={language.flag}
      alt=""
      aria-hidden="true"
      width="20"
      height="14"
      decoding="async"
      className={cn('h-3.5 w-5 shrink-0 rounded-[3px] object-cover ring-1 ring-black/10', className)}
    />
  )
}

/**
 * Language menu with flags (English / ខ្មែរ). When signed in, the choice is also saved as the profile's
 * preferred language. Keyboard: Enter/Space opens, arrows move, Escape closes.
 */
export default function LanguageSwitcher() {
  const { t, i18n } = useTranslation()
  const { user, setUser } = useAuth()
  const [open, setOpen] = useState(false)
  const root = useRef(null)
  const items = useRef([])
  const current = LANGUAGES.find((l) => l.code === i18n.language) || LANGUAGES[0]

  useEffect(() => {
    if (!open) return
    const onPointer = (event) => root.current && !root.current.contains(event.target) && setOpen(false)
    document.addEventListener('mousedown', onPointer)
    const active = LANGUAGES.findIndex((l) => l.code === current.code)
    items.current[active]?.focus()
    return () => document.removeEventListener('mousedown', onPointer)
  }, [open, current.code])

  const choose = (code) => {
    setOpen(false)
    if (code === i18n.language) return
    applyLanguage(code)
    if (user && user.locale !== code) {
      accountApi.updateMe({ locale: code }).then(setUser).catch(() => {})
    }
  }

  const onMenuKey = (event) => {
    const index = items.current.indexOf(document.activeElement)
    if (event.key === 'Escape') {
      setOpen(false)
      root.current?.querySelector('button')?.focus()
    } else if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
      event.preventDefault()
      const next = (index + (event.key === 'ArrowDown' ? 1 : -1) + LANGUAGES.length) % LANGUAGES.length
      items.current[next]?.focus()
    }
  }

  return (
    <div className="relative" ref={root}>
      <button
        type="button"
        onClick={() => setOpen((v) => !v)}
        aria-haspopup="menu"
        aria-expanded={open}
        aria-label={`${t('common.language')}: ${current.label}`}
        className="flex h-9 items-center gap-2 rounded-lg border border-line bg-surface px-2.5 text-sm font-medium text-ink transition-colors outline-none hover:bg-surface-2 focus-visible:ring-[3px] focus-visible:ring-ring"
      >
        <Flag language={current} />
        <span lang={current.code} className="hidden sm:inline">{current.short}</span>
        <ChevronDown aria-hidden="true" className={cn('size-3.5 text-subtle transition-transform', open && 'rotate-180')} />
      </button>

      {open && (
        <div
          role="menu"
          aria-label={t('common.language')}
          onKeyDown={onMenuKey}
          className="absolute right-0 z-50 mt-1.5 w-48 animate-in rounded-lg border border-line bg-surface p-1 shadow-lg shadow-black/5"
        >
          {LANGUAGES.map((language, i) => {
            const selected = language.code === current.code
            return (
              <button
                key={language.code}
                ref={(el) => (items.current[i] = el)}
                type="button"
                role="menuitemradio"
                aria-checked={selected}
                lang={language.code}
                onClick={() => choose(language.code)}
                className={cn(
                  'flex w-full items-center gap-2.5 rounded-md px-2.5 py-2 text-left text-sm text-ink outline-none transition-colors',
                  'hover:bg-surface-2 focus-visible:bg-surface-2',
                  selected && 'font-medium',
                )}
              >
                <Flag language={language} />
                <span className="flex-1">{language.label}</span>
                {selected && <Check aria-hidden="true" className="size-4 text-primary" />}
              </button>
            )
          })}
        </div>
      )}
    </div>
  )
}
