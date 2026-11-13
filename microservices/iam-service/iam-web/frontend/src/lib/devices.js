import { DesktopIcon, PhoneIcon, TabletIcon, TerminalIcon } from '../components/icons'

export const DEVICE_ICONS = { desktop: DesktopIcon, mobile: PhoneIcon, tablet: TabletIcon, api: TerminalIcon }

/** "Chrome 131 on Windows 10/11", or whatever part of that is known. */
export function deviceLabel(t, { browser, os }) {
  const knownBrowser = browser && browser !== 'Unknown' ? browser : null
  const knownOs = os && os !== 'Unknown' ? os : null
  if (knownBrowser && knownOs) return t('sessions.on', { browser: knownBrowser, os: knownOs })
  return knownBrowser || knownOs || t('device.unknown')
}
