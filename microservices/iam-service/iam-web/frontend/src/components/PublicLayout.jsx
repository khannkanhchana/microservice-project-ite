import Logo from './Logo'
import LanguageSwitcher from './LanguageSwitcher'

/** Shell for signed-out pages: logo, language switch and a centered column. */
export default function PublicLayout({ children, wide = false }) {
  return (
    <div className="flex min-h-screen flex-col">
      <header className="mx-auto flex h-16 w-full max-w-6xl items-center justify-between px-4">
        <Logo />
        <LanguageSwitcher />
      </header>
      <main className="flex flex-1 items-start justify-center px-4 pb-16 pt-6 sm:items-center sm:pt-0">
        <div className={wide ? 'w-full max-w-5xl' : 'w-full max-w-md'}>{children}</div>
      </main>
    </div>
  )
}
