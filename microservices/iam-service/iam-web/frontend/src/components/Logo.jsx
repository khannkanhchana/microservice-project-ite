import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'

export default function Logo({ to = '/' }) {
  const { t } = useTranslation()
  return (
    <Link to={to} className="flex items-center gap-2.5 rounded-lg font-semibold text-ink outline-none focus-visible:ring-[3px] focus-visible:ring-ring">
      <img src="/images/istad-logo.png" alt="ISTAD" className="size-9 shrink-0 rounded-full object-contain" />
      <span className="leading-tight">
        <span className="block text-sm tracking-tight">{t('app.name')}</span>
        <span className="block text-xs font-normal text-subtle">{t('app.tagline')}</span>
      </span>
    </Link>
  )
}
