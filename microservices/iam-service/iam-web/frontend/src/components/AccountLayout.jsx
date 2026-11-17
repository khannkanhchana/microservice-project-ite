import { useEffect, useRef, useState } from 'react'
import { Link, NavLink, Outlet, useLocation } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../auth/AuthContext'
import LanguageSwitcher from './LanguageSwitcher'
import { Avatar } from './ui'
import { canAdminister, hasPermission } from '../lib/permissions'
import {
  ActivityIcon,
  AppIcon,
  ChevronRightIcon,
  CloseIcon,
  CogIcon,
  DevicesIcon,
  GridIcon,
  HistoryIcon,
  KeyIcon,
  LinkIcon,
  ListIcon,
  LogoutIcon,
  MenuIcon,
  PowerIcon,
  PulseIcon,
  ShieldIcon,
  TagIcon,
  UserIcon,
  UsersIcon,
} from './icons'

const cx = (...classes) => classes.filter(Boolean).join(' ')

const ACCOUNT_NAV = [
  {
    group: 'nav.groupAccount',
    items: [
      { to: '/account', end: true, label: 'nav.overview', icon: GridIcon },
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
]

const ADMIN_NAV = [
  {
    group: 'nav.groupAdmin',
    items: [
      { to: '/admin', end: true, label: 'nav.adminOverview', icon: GridIcon, permission: 'dashboard:read' },
      { to: '/admin/users', label: 'nav.adminUsers', icon: UsersIcon, permission: 'user:read' },
      { to: '/admin/roles', label: 'nav.adminRoles', icon: TagIcon, permission: 'role:read' },
      { to: '/admin/permissions', label: 'nav.adminPermissions', icon: ShieldIcon, permission: 'permission:read' },
      { to: '/admin/clients', label: 'nav.adminClients', icon: AppIcon, permission: 'client:read' },
    ],
  },
  {
    group: 'nav.groupSecurity',
    items: [
      { to: '/admin/login-monitor', label: 'nav.adminLogins', icon: PulseIcon, permission: 'session:read' },
      { to: '/admin/audit-logs', label: 'nav.adminAudit', icon: ListIcon, permission: 'audit:read' },
      { to: '/admin/settings', label: 'nav.adminSettings', icon: CogIcon, permission: 'settings:read' },
    ],
  },
]

const ALL_ITEMS = [...ACCOUNT_NAV, ...ADMIN_NAV].flatMap((section) => section.items)

/** The nav item that matches the current path (longest match wins, so /admin/users beats /admin). */
function currentItem(pathname) {
  return ALL_ITEMS.filter((item) => (item.end ? pathname === item.to : pathname === item.to || pathname.startsWith(item.to + '/')))
    .sort((a, b) => b.to.length - a.to.length)[0]
}

function useClickOutside(ref, onOutside, active) {
  useEffect(() => {
    if (!active) return
    const onPointer = (event) => ref.current && !ref.current.contains(event.target) && onOutside()
    const onKey = (event) => event.key === 'Escape' && onOutside()
    document.addEventListener('mousedown', onPointer)
    document.addEventListener('keydown', onKey)
    return () => {
      document.removeEventListener('mousedown', onPointer)
      document.removeEventListener('keydown', onKey)
    }
  }, [ref, onOutside, active])
}

function Brand() {
  const { t } = useTranslation()
  return (
    <Link to="/account" className="flex items-center gap-2.5 rounded-lg outline-none focus-visible:ring-[3px] focus-visible:ring-ring">
      <span className="flex size-8 items-center justify-center rounded-lg bg-primary text-on-primary">
        <ShieldIcon className="size-[18px]" />
      </span>
      <span className="leading-tight">
        <span className="block text-sm font-semibold tracking-tight text-ink">{t('app.name')}</span>
        <span className="block text-[11px] text-subtle">{t('app.tagline')}</span>
      </span>
    </Link>
  )
}

function RoleBadge({ admin }) {
  const { t } = useTranslation()
  return (
    <span
      className={cx(
        'inline-flex items-center rounded-md px-1.5 py-px text-[11px] font-medium ring-1 ring-inset',
        admin ? 'bg-primary-soft text-primary ring-primary/20' : 'bg-surface-2 text-muted ring-line',
      )}
    >
      {t(admin ? 'nav.roleAdmin' : 'nav.roleMember')}
    </span>
  )
}

/** Segmented switch between the personal account area and the admin console (admins only). */
function WorkspaceSwitch({ inAdmin, onNavigate }) {
  const { t } = useTranslation()
  const tab = (active) =>
    cx(
      'flex min-w-0 flex-1 items-center justify-center gap-1.5 whitespace-nowrap rounded-md px-2 py-1.5 text-xs font-medium transition-colors outline-none focus-visible:ring-[3px] focus-visible:ring-ring',
      active ? 'bg-surface text-ink shadow-xs ring-1 ring-line' : 'text-muted hover:text-ink',
    )
  return (
    <div className="flex gap-1 rounded-lg bg-surface-2 p-1">
      <Link to="/account" onClick={onNavigate} className={tab(!inAdmin)} aria-current={!inAdmin ? 'page' : undefined}>
        <UserIcon className="size-3.5 shrink-0" />
        {t('nav.workspaceAccount')}
      </Link>
      <Link to="/admin" onClick={onNavigate} className={tab(inAdmin)} aria-current={inAdmin ? 'page' : undefined}>
        <GridIcon className="size-3.5 shrink-0" />
        {t('nav.workspaceAdmin')}
      </Link>
    </div>
  )
}

function Sidebar({ onNavigate }) {
  const { t } = useTranslation()
  const { user, logout } = useAuth()
  const { pathname } = useLocation()
  const isAdmin = canAdminister(user)
  const inAdmin = pathname.startsWith('/admin')

  const sections = (inAdmin ? ADMIN_NAV : ACCOUNT_NAV)
    .map((section) => ({
      ...section,
      items: section.items.filter((item) => !item.permission || hasPermission(user, item.permission)),
    }))
    .filter((section) => section.items.length > 0)

  const linkClass = ({ isActive }) =>
    cx(
      'group flex h-9 items-center gap-3 rounded-lg px-3 text-sm transition-colors outline-none focus-visible:ring-[3px] focus-visible:ring-ring',
      isActive ? 'bg-surface-2 font-medium text-ink' : 'text-muted hover:bg-surface-2/70 hover:text-ink',
    )

  return (
    <div className="flex h-full flex-col">
      <div className="flex h-14 items-center px-5">
        <Brand />
      </div>

      {isAdmin && (
        <div className="px-3 pt-2 pb-1">
          <WorkspaceSwitch inAdmin={inAdmin} onNavigate={onNavigate} />
        </div>
      )}

      <nav className="flex-1 overflow-y-auto px-3 py-3" aria-label={t(inAdmin ? 'nav.workspaceAdmin' : 'nav.workspaceAccount')}>
        <div className="flex flex-col gap-6">
          {sections.map((section) => (
            <div key={section.group}>
              <p className="mb-1 px-3 text-xs font-medium text-subtle">{t(section.group)}</p>
              <ul className="flex flex-col gap-0.5">
                {section.items.map(({ to, end, label, icon: Icon }) => (
                  <li key={to}>
                    <NavLink to={to} end={end} className={linkClass} onClick={onNavigate}>
                      {({ isActive }) => (
                        <>
                          <Icon className={cx('size-4', isActive ? 'text-primary' : 'text-subtle group-hover:text-ink')} />
                          {t(label)}
                        </>
                      )}
                    </NavLink>
                  </li>
                ))}
              </ul>
            </div>
          ))}

          {!inAdmin && (
            <div className="border-t border-line pt-4">
              <NavLink
                to="/account/deactivate"
                onClick={onNavigate}
                className={({ isActive }) =>
                  cx(
                    'flex h-9 items-center gap-3 rounded-lg px-3 text-sm transition-colors outline-none focus-visible:ring-[3px] focus-visible:ring-ring',
                    isActive ? 'bg-danger-soft font-medium text-danger' : 'text-muted hover:bg-danger-soft hover:text-danger',
                  )
                }
              >
                <PowerIcon className="size-4" />
                {t('nav.deactivate')}
              </NavLink>
            </div>
          )}
        </div>
      </nav>

      <div className="border-t border-line p-3">
        <div className="flex items-center gap-3 rounded-lg p-2">
          <Avatar src={user?.pictureUrl} name={user?.name || user?.username} size="sm" />
          <div className="min-w-0 flex-1 leading-tight">
            <p className="truncate text-sm font-semibold text-ink">{user?.name || user?.username}</p>
            <div className="mt-1">
              <RoleBadge admin={isAdmin} />
            </div>
          </div>
          <button
            type="button"
            onClick={logout}
            title={t('common.signOut')}
            aria-label={t('common.signOut')}
            className="rounded-lg p-2 text-subtle transition-colors outline-none hover:bg-danger-soft hover:text-danger focus-visible:ring-[3px] focus-visible:ring-ring"
          >
            <LogoutIcon className="size-4" />
          </button>
        </div>
      </div>
    </div>
  )
}

function UserMenu() {
  const { t } = useTranslation()
  const { user, logout } = useAuth()
  const [open, setOpen] = useState(false)
  const ref = useRef(null)
  const { pathname } = useLocation()
  useEffect(() => setOpen(false), [pathname])
  useClickOutside(ref, () => setOpen(false), open)
  const isAdmin = canAdminister(user)

  const item = 'flex w-full items-center gap-2.5 rounded-md px-2.5 py-2 text-sm text-ink transition-colors outline-none hover:bg-surface-2 focus-visible:bg-surface-2'

  return (
    <div className="relative" ref={ref}>
      <button
        type="button"
        onClick={() => setOpen((v) => !v)}
        aria-expanded={open}
        aria-haspopup="menu"
        aria-label={t('nav.userMenu')}
        className="flex items-center gap-1.5 rounded-full p-0.5 transition-colors outline-none hover:bg-surface-2 focus-visible:ring-[3px] focus-visible:ring-ring sm:pr-1.5"
      >
        <Avatar src={user?.pictureUrl} name={user?.name || user?.username} size="sm" />
        <ChevronRightIcon className={cx('hidden size-4 text-subtle transition-transform sm:block', open ? '-rotate-90' : 'rotate-90')} />
      </button>

      {open && (
        <div role="menu" className="absolute right-0 z-50 mt-1.5 w-64 animate-in rounded-lg border border-line bg-surface p-1 shadow-lg shadow-black/5">
          <div className="flex items-center gap-3 px-2.5 py-2.5">
            <Avatar src={user?.pictureUrl} name={user?.name || user?.username} size="md" />
            <div className="min-w-0 leading-tight">
              <p className="truncate text-sm font-semibold text-ink">{user?.name || user?.username}</p>
              <p className="truncate text-xs text-subtle">{user?.email}</p>
              <div className="mt-1.5">
                <RoleBadge admin={isAdmin} />
              </div>
            </div>
          </div>
          <div className="my-1 h-px bg-line" />
          <Link role="menuitem" to="/account/profile" className={item}>
            <UserIcon className="size-4 text-subtle" />
            {t('nav.profile')}
          </Link>
          <Link role="menuitem" to="/account/security" className={item}>
            <KeyIcon className="size-4 text-subtle" />
            {t('nav.security')}
          </Link>
          {isAdmin && (
            <Link role="menuitem" to="/admin" className={item}>
              <GridIcon className="size-4 text-subtle" />
              {t('nav.workspaceAdmin')}
            </Link>
          )}
          <div className="my-1 h-px bg-line" />
          <button role="menuitem" type="button" onClick={logout} className={cx(item, 'text-danger hover:bg-danger-soft focus-visible:bg-danger-soft')}>
            <LogoutIcon className="size-4" />
            {t('common.signOut')}
          </button>
        </div>
      )}
    </div>
  )
}

export default function AccountLayout() {
  const { t } = useTranslation()
  const [menuOpen, setMenuOpen] = useState(false)
  const { pathname } = useLocation()
  useEffect(() => setMenuOpen(false), [pathname])

  const inAdmin = pathname.startsWith('/admin')
  const page = currentItem(pathname)

  return (
    <div className="min-h-screen bg-bg">
      {/* Desktop sidebar */}
      <aside className="fixed inset-y-0 left-0 z-30 hidden w-72 border-r border-line bg-surface lg:block">
        <Sidebar />
      </aside>

      {/* Mobile drawer */}
      {menuOpen && (
        <div className="fixed inset-0 z-50 lg:hidden">
          <div className="absolute inset-0 bg-overlay" onClick={() => setMenuOpen(false)} aria-hidden="true" />
          <aside className="absolute inset-y-0 left-0 w-72 max-w-[85%] animate-in border-r border-line bg-surface shadow-xl">
            <button
              type="button"
              className="absolute top-2.5 right-2.5 rounded-lg p-2 text-muted outline-none hover:bg-surface-2 focus-visible:ring-[3px] focus-visible:ring-ring"
              onClick={() => setMenuOpen(false)}
              aria-label={t('common.closeMenu')}
            >
              <CloseIcon className="size-5" />
            </button>
            <Sidebar onNavigate={() => setMenuOpen(false)} />
          </aside>
        </div>
      )}

      <div className="lg:pl-72">
        <header className="sticky top-0 z-20 border-b border-line bg-bg/90 backdrop-blur supports-[backdrop-filter]:bg-bg/75">
          <div className="flex h-14 items-center justify-between gap-3 px-4 sm:px-6 lg:px-8">
            <div className="flex min-w-0 items-center gap-2">
              <button
                type="button"
                className="-ml-1 rounded-lg p-2 text-muted outline-none hover:bg-surface-2 focus-visible:ring-[3px] focus-visible:ring-ring lg:hidden"
                onClick={() => setMenuOpen(true)}
                aria-label={t('common.openMenu')}
              >
                <MenuIcon className="size-5" />
              </button>
              <nav aria-label="Breadcrumb" className="flex min-w-0 items-center gap-1.5 text-sm">
                <Link to={inAdmin ? '/admin' : '/account'} className="hidden shrink-0 whitespace-nowrap text-muted hover:text-ink sm:inline">
                  {t(inAdmin ? 'nav.workspaceAdmin' : 'nav.workspaceAccount')}
                </Link>
                {page && (
                  <>
                    <ChevronRightIcon className="hidden size-4 shrink-0 text-subtle sm:block" />
                    <span className="truncate font-medium text-ink">{t(page.label)}</span>
                  </>
                )}
              </nav>
            </div>
            <div className="flex items-center gap-2">
              <LanguageSwitcher />
              <UserMenu />
            </div>
          </div>
        </header>

        <main className={cx('mx-auto px-4 py-8 sm:px-6 lg:px-8 lg:py-10', inAdmin ? 'max-w-7xl' : 'max-w-5xl')}>
          <Outlet />
        </main>
      </div>
    </div>
  )
}
