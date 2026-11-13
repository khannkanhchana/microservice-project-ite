import { useTranslation } from 'react-i18next'
import { accountApi } from '../api/account'
import { useAuth } from '../auth/AuthContext'
import { applyLanguage, LANGUAGES } from '../i18n'
import { GlobeIcon } from './icons'

/** EN / ខ្មែរ toggle. When signed in, the choice is also saved as the profile's preferred language. */
export default function LanguageSwitcher() {
  const { t, i18n } = useTranslation()
  const { user, setUser } = useAuth()

  const choose = (code) => {
    if (code === i18n.language) return
    applyLanguage(code)
    if (user && user.locale !== code) {
      accountApi
        .updateMe({ locale: code })
        .then(setUser)
        .catch(() => {})
    }
  }

  return (
    <div role="group" aria-label={t('common.language')} className="flex items-center gap-1 rounded-lg border border-line bg-surface p-0.5">
      <GlobeIcon className="ml-1.5 size-4 text-subtle" />
      {LANGUAGES.map((language) => (
        <button
          key={language.code}
          type="button"
          lang={language.code}
          aria-pressed={i18n.language === language.code}
          onClick={() => choose(language.code)}
          className={
            'rounded-md px-2 py-1 text-xs font-medium transition-colors ' +
            (i18n.language === language.code ? 'bg-primary-soft text-primary' : 'text-muted hover:text-ink')
          }
        >
          {language.short}
        </button>
      ))}
    </div>
  )
}
