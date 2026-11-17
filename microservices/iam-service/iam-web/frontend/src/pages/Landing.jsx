import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../auth/AuthContext'
import { iamPage } from '../auth/oauth'
import PublicLayout from '../components/PublicLayout'
import { Alert, Button, buttonVariants } from '../components/ui'
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
      <div className="grid items-center gap-12 lg:grid-cols-[1.1fr_1fr] lg:gap-16">
        <div>
          <p className="flex items-center gap-2 text-sm font-medium text-primary">
            <ShieldIcon className="size-4" /> OAuth 2.1 · OpenID Connect
          </p>
          <h1 className="mt-4 max-w-xl text-3xl font-semibold tracking-tight text-balance text-ink sm:text-[40px] sm:leading-[1.15]">
            {t('landing.title')}
          </h1>
          <p className="mt-4 max-w-lg text-base leading-relaxed text-muted">{t('landing.subtitle')}</p>
          <ul className="mt-10 grid max-w-lg grid-cols-1 gap-x-6 gap-y-4 sm:grid-cols-2">
            {features.map(([Icon, key]) => (
              <li key={key} className="flex items-center gap-3 text-sm text-ink">
                <Icon className="size-4 shrink-0 text-subtle" />
                {t(key)}
              </li>
            ))}
          </ul>
        </div>

        <div className="rounded-xl border border-line bg-surface p-6 sm:p-8">
          {notice && (
            <div className="mb-5">
              <Alert tone={notice[0]} onDismiss={() => setParams({}, { replace: true })}>
                {t(notice[1])}
              </Alert>
            </div>
          )}
          {status === 'authenticated' ? (
            <Button size="lg" className="w-full" onClick={() => navigate('/me')}>
              {t('landing.continue')}
            </Button>
          ) : (
            <div className="flex flex-col gap-3">
              <Button size="lg" className="w-full" onClick={() => login({ returnTo: '/me' })}>
                {t('landing.signIn')}
              </Button>
              {/* Registration is the IAM page /register (same server as /login), not a React route. */}
              <a
                href={iamPage('/register')}
                className={buttonVariants({ variant: 'secondary', size: 'lg', className: 'w-full' })}
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
