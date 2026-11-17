import {
  Activity, AppWindow, Camera, Check, ChevronRight, Eye, EyeOff, Globe, History, Info, KeyRound, LayoutDashboard,
  Link2, LogOut, Mail, Menu, Monitor, MonitorSmartphone, Plus, Power, Radar, RefreshCw, ScrollText, Settings,
  ShieldCheck, Smartphone, Tablet, Tag, Terminal, TriangleAlert, UserRound, Users, X,
} from 'lucide-react'

/**
 * App icons, all from Lucide (https://lucide.dev). Pages import these names, so the icon set can change in one
 * place. Every icon is decorative by default (aria-hidden) and 20px unless a className sets the size.
 */
const icon = (Component) => {
  const Wrapped = ({ className = 'size-5', strokeWidth = 1.75, ...props }) => (
    <Component aria-hidden="true" focusable="false" className={className} strokeWidth={strokeWidth} {...props} />
  )
  Wrapped.displayName = Component.displayName || 'Icon'
  return Wrapped
}

export const ShieldIcon = icon(ShieldCheck)
export const UserIcon = icon(UserRound)
export const KeyIcon = icon(KeyRound)
export const LinkIcon = icon(Link2)
export const DevicesIcon = icon(MonitorSmartphone)
export const HistoryIcon = icon(History)
export const ActivityIcon = icon(Activity)
export const PowerIcon = icon(Power)
export const LogoutIcon = icon(LogOut)
export const GlobeIcon = icon(Globe)
export const MenuIcon = icon(Menu)
export const CloseIcon = icon(X)
export const CameraIcon = icon(Camera)
export const CheckIcon = icon(Check)
export const AlertIcon = icon(TriangleAlert)
export const InfoIcon = icon(Info)
export const DesktopIcon = icon(Monitor)
export const PhoneIcon = icon(Smartphone)
export const TabletIcon = icon(Tablet)
export const TerminalIcon = icon(Terminal)
export const EyeIcon = icon(Eye)
export const EyeOffIcon = icon(EyeOff)
export const RefreshIcon = icon(RefreshCw)
export const MailIcon = icon(Mail)
export const ChevronRightIcon = icon(ChevronRight)
export const GridIcon = icon(LayoutDashboard)
export const UsersIcon = icon(Users)
export const TagIcon = icon(Tag)
export const AppIcon = icon(AppWindow)
export const PulseIcon = icon(Radar)
export const ListIcon = icon(ScrollText)
export const CogIcon = icon(Settings)
export const PlusIcon = icon(Plus)

/** Sign-in provider logos (brand marks, so not from Lucide). */
export const ProviderIcon = ({ provider, className = 'size-5' }) => {
  switch (provider) {
    case 'google':
      return (
        <svg viewBox="0 0 24 24" className={className} aria-hidden="true">
          <path fill="#4285F4" d="M21.6 12.2c0-.7-.1-1.4-.2-2H12v3.8h5.4a4.6 4.6 0 01-2 3v2.5h3.2c1.9-1.7 3-4.3 3-7.3z" />
          <path fill="#34A853" d="M12 22c2.7 0 5-.9 6.6-2.4l-3.2-2.5c-.9.6-2 1-3.4 1-2.6 0-4.8-1.8-5.6-4.1H3.1v2.6A10 10 0 0012 22z" />
          <path fill="#FBBC05" d="M6.4 14a6 6 0 010-3.9V7.5H3.1a10 10 0 000 9z" />
          <path fill="#EA4335" d="M12 6c1.5 0 2.8.5 3.8 1.5l2.9-2.9A10 10 0 003.1 7.5l3.3 2.6C7.2 7.8 9.4 6 12 6z" />
        </svg>
      )
    case 'github':
      return (
        <svg viewBox="0 0 24 24" className={className} fill="currentColor" aria-hidden="true">
          <path d="M12 2a10 10 0 00-3.2 19.5c.5.1.7-.2.7-.5v-1.7c-2.8.6-3.4-1.3-3.4-1.3-.4-1.2-1.1-1.5-1.1-1.5-.9-.6.1-.6.1-.6 1 .1 1.5 1 1.5 1 .9 1.5 2.3 1.1 2.9.8.1-.6.4-1.1.6-1.3-2.2-.3-4.6-1.1-4.6-5a3.9 3.9 0 011-2.7c-.1-.3-.4-1.3.1-2.7 0 0 .8-.3 2.8 1a9.6 9.6 0 015 0c1.9-1.3 2.8-1 2.8-1 .5 1.4.2 2.4.1 2.7a3.9 3.9 0 011 2.7c0 3.9-2.3 4.7-4.6 5 .4.3.7.9.7 1.9V21c0 .3.2.6.7.5A10 10 0 0012 2z" />
        </svg>
      )
    case 'facebook':
      return (
        <svg viewBox="0 0 24 24" className={className} aria-hidden="true">
          <circle cx="12" cy="12" r="10" fill="#1877F2" />
          <path fill="#fff" d="M13.4 21.9v-7h2.3l.4-2.8h-2.7v-1.8c0-.8.2-1.3 1.4-1.3h1.4V6.5c-.3 0-1.1-.1-2.1-.1-2.1 0-3.5 1.3-3.5 3.6v2.1H8.3v2.8h2.3v7z" />
        </svg>
      )
    case 'telegram':
      return (
        <svg viewBox="0 0 24 24" className={className} aria-hidden="true">
          <circle cx="12" cy="12" r="10" fill="#229ED9" />
          <path fill="#fff" d="M6.7 11.8l9.6-3.7c.4-.2.8.1.7.7l-1.6 7.7c-.1.6-.5.7-.9.4l-2.5-1.8-1.2 1.2c-.1.1-.3.2-.5.2l.2-2.6 4.7-4.3c.2-.2 0-.3-.3-.1l-5.8 3.7-2.5-.8c-.6-.2-.6-.5.1-.6z" />
        </svg>
      )
    default:
      return <KeyIcon className={className} />
  }
}
