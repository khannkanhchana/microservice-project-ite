import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import PublicLayout from '../components/PublicLayout'

export default function NotFound() {
  const { t } = useTranslation()
  return (
    <PublicLayout>
      <div className="text-center">
        <p className="text-5xl font-bold text-primary">404</p>
        <h1 className="mt-4 text-xl font-semibold text-ink">{t('errors.notFoundTitle')}</h1>
        <p className="mt-2 text-sm text-muted">{t('errors.notFoundBody')}</p>
        <Link to="/" className="mt-6 inline-block text-sm font-medium text-primary hover:underline">
          {t('common.backHome')}
        </Link>
      </div>
    </PublicLayout>
  )
}
