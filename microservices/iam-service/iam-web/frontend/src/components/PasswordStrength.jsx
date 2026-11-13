import { useTranslation } from 'react-i18next'

export function passwordScore(password) {
  if (!password) return 0
  let score = 0
  if (password.length >= 8) score++
  if (password.length >= 12) score++
  if (/[a-z]/.test(password) && /[A-Z]/.test(password)) score++
  if (/\d/.test(password)) score++
  if (/[^A-Za-z0-9]/.test(password)) score++
  return Math.min(4, Math.max(1, score - (password.length < 8 ? 1 : 0)))
}

const LEVELS = [
  { key: 'weak', bar: 'bg-danger', text: 'text-danger' },
  { key: 'fair', bar: 'bg-warning', text: 'text-warning' },
  { key: 'good', bar: 'bg-primary', text: 'text-primary' },
  { key: 'strong', bar: 'bg-success', text: 'text-success' },
]

export default function PasswordStrength({ password }) {
  const { t } = useTranslation()
  if (!password) return null
  const score = passwordScore(password)
  const level = LEVELS[score - 1]
  return (
    <div className="mt-2" aria-live="polite">
      <div className="flex gap-1">
        {LEVELS.map((item, index) => (
          <span key={item.key} className={'h-1.5 flex-1 rounded-full ' + (index < score ? level.bar : 'bg-surface-2')} />
        ))}
      </div>
      <p className={'mt-1 text-xs font-medium ' + level.text}>{t('password.strength', { label: t(`password.${level.key}`) })}</p>
    </div>
  )
}
