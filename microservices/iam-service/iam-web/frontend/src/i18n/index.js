import i18n from 'i18next'
import { initReactI18next } from 'react-i18next'
import en from './locales/en'
import km from './locales/km'
import adminEn, { activityTypesEn, navEn } from './locales/admin.en'
import adminKm, { activityTypesKm, navKm } from './locales/admin.km'

// The administration console's strings live in their own files; merge them into each language.
const withAdmin = (base, admin, nav, activityTypes) => ({
  ...base,
  admin,
  nav: { ...base.nav, ...nav },
  activityType: { ...base.activityType, ...activityTypes },
})

const STORAGE_KEY = 'iam.lang'
export const LANGUAGES = [
  { code: 'en', label: 'English', short: 'EN' },
  { code: 'km', label: 'ភាសាខ្មែរ', short: 'ខ្មែរ' },
]

function storedLanguage() {
  try {
    return localStorage.getItem(STORAGE_KEY)
  } catch {
    return null
  }
}

export const hasStoredLanguage = () => Boolean(storedLanguage())

function initialLanguage() {
  const stored = storedLanguage()
  if (stored === 'en' || stored === 'km') return stored
  return navigator.language?.toLowerCase().startsWith('km') ? 'km' : 'en'
}

/** Switches the UI language; `persist` remembers the choice on this browser. */
export function applyLanguage(code, persist = true) {
  i18n.changeLanguage(code)
  document.documentElement.lang = code
  if (persist) {
    try {
      localStorage.setItem(STORAGE_KEY, code)
    } catch {
      // Not persisted; the choice still applies to this page.
    }
  }
}

i18n.use(initReactI18next).init({
  resources: {
    en: { translation: withAdmin(en, adminEn, navEn, activityTypesEn) },
    km: { translation: withAdmin(km, adminKm, navKm, activityTypesKm) },
  },
  lng: initialLanguage(),
  fallbackLng: 'en',
  interpolation: { escapeValue: false },
})
document.documentElement.lang = i18n.language

export default i18n
