import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../auth/AuthContext'
import { iamPage } from '../auth/oauth'
import PublicLayout from '../components/PublicLayout'
import { Alert, Button } from '../components/ui'
import { DevicesIcon, HistoryIcon, KeyIcon, ShieldIcon, UserIcon } from '../components/icons'

const NOTICES = {
  1: ['info', 'landing.signedOut'],
  revoked: ['warning', 'landing.revoked'],
  deactivated: ['warning', 'landing.deactivated'],
  reset: ['success', 'landing.passwordReset'],
  registered: ['success', 'landing.registered'],
}

export default function Landing() {
  const { t } = useTranslation()
  const { status, login } = useAuth()
  const navigate = useNavigate()
  const [params, setParams] = useSearchParams()
  const notice = NOTICES[params.get('signedOut')] || NOTICES[params.get('notice')]

  const features = [
    [UserIcon, 'landing.featureProfile'],
    [KeyIcon, 'landing.featureSecurity'],
    [DevicesIcon, 'landing.featureSessions'],
    [HistoryIcon, 'landing.featureHistory'],
  ]

  return (
    <PublicLayout wide>
      <div className="grid items-center gap-10 lg:grid-cols-2">
        <div>
          <span className="inline-flex items-center gap-2 rounded-full bg-primary-soft px-3 py-1 text-xs font-medium text-primary">
            <ShieldIcon className="size-4" /> OAuth 2.1 · OpenID Connect
          </span>
          <h1 className="mt-4 text-3xl font-bold tracking-tight text-ink sm:text-4xl">{t('landing.title')}</h1>
          <p className="mt-4 max-w-lg text-base text-muted">{t('landing.subtitle')}</p>
          <ul className="mt-8 grid max-w-lg grid-cols-1 gap-3 sm:grid-cols-2">
            {features.map(([Icon, key]) => (
              <li key={key} className="flex items-center gap-3 text-sm text-ink">
                <span className="rounded-lg bg-surface p-2 text-primary ring-1 ring-line">
                  <Icon className="size-4" />
                </span>
                {t(key)}
              </li>
            ))}
          </ul>
        </div>

        <div className="rounded-2xl border border-line bg-surface p-6 shadow-sm sm:p-8">
          {notice && (
            <div className="mb-5">
              <Alert tone={notice[0]} onDismiss={() => setParams({}, { replace: true })}>
                {t(notice[1])}
              </Alert>
            </div>
          )}
          {status === 'authenticated' ? (
            <Button className="w-full" onClick={() => navigate('/account')}>
              {t('landing.continue')}
            </Button>
          ) : (
            <div className="flex flex-col gap-3">
              <Button className="h-11 w-full" onClick={() => login({ returnTo: '/account' })}>
                {t('landing.signIn')}
              </Button>
              {/* Registration is the IAM page /register (same server as /login), not a React route. */}
              <a
                href={iamPage('/register')}
                className="flex h-11 items-center justify-center rounded-lg border border-line text-sm font-medium text-ink hover:bg-surface-2"
              >
                {t('landing.create')}
              </a>
              <Link to="/forgot-password" className="mt-2 text-center text-sm font-medium text-primary hover:underline">
                {t('landing.forgot')}
              </Link>
            </div>
          )}
        </div>
      </div>
    </PublicLayout>
  )
}
