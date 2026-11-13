import { useEffect, useState } from 'react'
import { NavLink, Outlet, useLocation } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../auth/AuthContext'
import LanguageSwitcher from './LanguageSwitcher'
import Logo from './Logo'
import { Avatar } from './ui'
import { hasPermission } from '../lib/permissions'
import {
  ActivityIcon,
  AppIcon,
  CogIcon,
  GridIcon,
  ListIcon,
  PulseIcon,
  ShieldIcon,
  TagIcon,
  UsersIcon,
  CloseIcon,
  DevicesIcon,
  HistoryIcon,
  KeyIcon,
  LinkIcon,
  LogoutIcon,
  MenuIcon,
  PowerIcon,
  UserIcon,
} from './icons'

const NAV = [
  {
    group: 'nav.groupAccount',
    items: [
      { to: '/account/profile', label: 'nav.profile', icon: UserIcon },
      { to: '/account/security', label: 'nav.security', icon: KeyIcon },
      { to: '/account/linked-accounts', label: 'nav.linkedAccounts', icon: LinkIcon },
    ],
  },
  {
    group: 'nav.groupSecurity',
    items: [
      { to: '/account/sessions', label: 'nav.sessions', icon: DevicesIcon },
      { to: '/account/login-history', label: 'nav.loginHistory', icon: HistoryIcon },
      { to: '/account/activity', label: 'nav.activity', icon: ActivityIcon },
    ],
  },
  {
    group: 'nav.groupAdmin',
    items: [
      { to: '/admin', end: true, label: 'nav.adminOverview', icon: GridIcon, permission: 'dashboard:read' },
      { to: '/admin/users', label: 'nav.adminUsers', icon: UsersIcon, permission: 'user:read' },
      { to: '/admin/roles', label: 'nav.adminRoles', icon: TagIcon, permission: 'role:read' },
      { to: '/admin/permissions', label: 'nav.adminPermissions', icon: ShieldIcon, permission: 'permission:read' },
      { to: '/admin/clients', label: 'nav.adminClients', icon: AppIcon, permission: 'client:read' },
      { to: '/admin/login-monitor', label: 'nav.adminLogins', icon: PulseIcon, permission: 'session:read' },
      { to: '/admin/audit-logs', label: 'nav.adminAudit', icon: ListIcon, permission: 'audit:read' },
      { to: '/admin/settings', label: 'nav.adminSettings', icon: CogIcon, permission: 'settings:read' },
    ],
  },
]

function Navigation({ onNavigate }) {
  const { t } = useTranslation()
  const { user } = useAuth()
  const sections = NAV.map((section) => ({
    ...section,
    items: section.items.filter((item) => !item.permission || hasPermission(user, item.permission)),
  })).filter((section) => section.items.length > 0)
  const linkClass = ({ isActive }) =>
    'flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors ' +
    (isActive ? 'bg-primary-soft text-primary' : 'text-muted hover:bg-surface-2 hover:text-ink')
  return (
    <nav className="flex flex-col gap-6" aria-label={t('nav.groupAccount')}>
      {sections.map((section) => (
        <div key={section.group}>
          <p className="mb-2 px-3 text-xs font-semibold uppercase tracking-wide text-subtle">{t(section.group)}</p>
          <ul className="flex flex-col gap-0.5">
            {section.items.map(({ to, end, label, icon: Icon }) => (
              <li key={to}>
                <NavLink to={to} end={end} className={linkClass} onClick={onNavigate}>
                  <Icon className="size-[18px]" />
                  {t(label)}
                </NavLink>
              </li>
            ))}
          </ul>
        </div>
      ))}
      <div className="border-t border-line pt-4">
        <NavLink
          to="/account/deactivate"
          onClick={onNavigate}
          className={({ isActive }) =>
            'flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors ' +
            (isActive ? 'bg-danger-soft text-danger' : 'text-danger hover:bg-danger-soft')
          }
        >
          <PowerIcon className="size-[18px]" />
          {t('nav.deactivate')}
        </NavLink>
      </div>
    </nav>
  )
}

export default function AccountLayout() {
  const { t } = useTranslation()
  const { user, logout } = useAuth()
  const [menuOpen, setMenuOpen] = useState(false)
  const location = useLocation()
  useEffect(() => setMenuOpen(false), [location.pathname])

  return (
    <div className="min-h-screen">
      <header className="sticky top-0 z-30 border-b border-line bg-surface/90 backdrop-blur">
        <div className={'mx-auto flex h-16 items-center justify-between gap-3 px-4 ' + (location.pathname.startsWith('/admin') ? 'max-w-7xl' : 'max-w-6xl')}>
          <div className="flex items-center gap-2">
            <button
              type="button"
              className="rounded-lg p-2 text-muted hover:bg-surface-2 lg:hidden"
              onClick={() => setMenuOpen(true)}
              aria-label={t('common.openMenu')}
            >
              <MenuIcon />
            </button>
            <Logo to="/account" />
          </div>
          <div className="flex items-center gap-3">
            <LanguageSwitcher />
            <div className="hidden items-center gap-2.5 sm:flex">
              <Avatar src={user?.pictureUrl} name={user?.name || user?.username} size="sm" />
              <div className="max-w-40 leading-tight">
                <p className="truncate text-sm font-medium text-ink">{user?.name || user?.username}</p>
                <p className="truncate text-xs text-subtle">{user?.email}</p>
              </div>
            </div>
            <button
              type="button"
              onClick={logout}
              className="flex items-center gap-1.5 rounded-lg px-2.5 py-2 text-sm font-medium text-muted hover:bg-surface-2 hover:text-ink"
            >
              <LogoutIcon className="size-[18px]" />
              <span className="hidden md:inline">{t('common.signOut')}</span>
            </button>
          </div>
        </div>
      </header>

      {menuOpen && (
        <div className="fixed inset-0 z-40 lg:hidden">
          <div className="absolute inset-0 bg-black/40" onClick={() => setMenuOpen(false)} />
          <aside className="absolute inset-y-0 left-0 w-72 max-w-[85%] overflow-y-auto bg-surface p-4 shadow-xl">
            <div className="mb-6 flex items-center justify-between">
              <Logo to="/account" />
              <button
                type="button"
                className="rounded-lg p-2 text-muted hover:bg-surface-2"
                onClick={() => setMenuOpen(false)}
                aria-label={t('common.closeMenu')}
              >
                <CloseIcon />
              </button>
            </div>
            <Navigation onNavigate={() => setMenuOpen(false)} />
          </aside>
        </div>
      )}

      <div className={'mx-auto flex gap-8 px-4 py-8 ' + (location.pathname.startsWith('/admin') ? 'max-w-7xl' : 'max-w-6xl')}>
        <aside className="hidden w-60 shrink-0 lg:block">
          <div className="sticky top-24">
            <Navigation />
          </div>
        </aside>
        <main className="min-w-0 flex-1">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
