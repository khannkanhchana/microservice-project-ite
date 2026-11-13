import { useTranslation } from 'react-i18next'
import { useAuth } from './AuthContext'
import { EmptyState } from '../components/ui'
import { ShieldIcon } from '../components/icons'
import { hasPermission } from '../lib/permissions'

/** Renders children only for users holding `permission`; others see a "no access" message. */
export default function RequirePermission({ permission, children }) {
  const { t } = useTranslation()
  const { user } = useAuth()
  if (hasPermission(user, permission)) return children
  return <EmptyState icon={<ShieldIcon />} title={t('admin.noAccess')} description={t('admin.noAccessHint', { permission })} />
}
