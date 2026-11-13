import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { ShieldIcon } from './icons'

export default function Logo({ to = '/' }) {
  const { t } = useTranslation()
  return (
    <Link to={to} className="flex items-center gap-2.5 font-semibold text-ink">
      <span className="flex size-8 items-center justify-center rounded-lg bg-primary text-on-primary">
        <ShieldIcon className="size-5" />
      </span>
      <span className="leading-tight">
        <span className="block text-sm">{t('app.name')}</span>
        <span className="block text-[11px] font-normal text-subtle">{t('app.tagline')}</span>
      </span>
    </Link>
  )
}
