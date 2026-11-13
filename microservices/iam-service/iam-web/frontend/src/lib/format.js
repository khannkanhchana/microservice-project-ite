const intlLocale = (lang) => (lang === 'km' ? 'km-KH' : 'en-US')

export function formatDateTime(value, lang) {
  if (!value) return '—'
  return new Intl.DateTimeFormat(intlLocale(lang), { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))
}

export function formatDate(value, lang) {
  if (!value) return '—'
  return new Intl.DateTimeFormat(intlLocale(lang), { dateStyle: 'long' }).format(new Date(value))
}

const UNITS = [
  ['year', 365 * 24 * 3600],
  ['month', 30 * 24 * 3600],
  ['week', 7 * 24 * 3600],
  ['day', 24 * 3600],
  ['hour', 3600],
  ['minute', 60],
]

export function relativeTime(value, lang) {
  if (!value) return '—'
  const seconds = (new Date(value).getTime() - Date.now()) / 1000
  const formatter = new Intl.RelativeTimeFormat(intlLocale(lang), { numeric: 'auto' })
  for (const [unit, size] of UNITS) {
    if (Math.abs(seconds) >= size) return formatter.format(Math.round(seconds / size), unit)
  }
  return formatter.format(0, 'minute')
}

/** Human message for an ApiError (or anything thrown), localized where the cause is generic. */
export function errorMessage(error, t) {
  if (!error) return ''
  if (error.status === 0) return t('errors.network')
  if (error.status === 401) return t('errors.unauthorized')
  if (error.status === 403) return t('errors.forbidden')
  if (error.status === 503) return t('errors.unavailable')
  if (error.status >= 500) return t('errors.server')
  return error.message || t('errors.generic')
}
