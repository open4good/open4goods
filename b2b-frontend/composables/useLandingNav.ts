/**
 * Provides top navigation links and outbound destinations used by landing pages.
 */
export interface LandingNavLink {
  key: string
  to: string
  activePrefix?: string
  parentClickable?: boolean
  children?: LandingNavLink[]
}

export interface LandingNavAction {
  key: string
  to: string
  variant: 'flat' | 'tonal'
  color: string
  icon: string
  emphasis: 'strong' | 'light'
}

export function useLandingNav() {
  const items: LandingNavLink[] = [
    { key: 'landing.nav.pricing', to: '/pricing' },
    { key: 'landing.nav.docs', to: '/docs', activePrefix: '/docs' },
    { key: 'landing.nav.faq', to: '/faq' },
    { key: 'landing.nav.contact', to: '/contact' }
  ]

  const primaryActions: LandingNavAction[] = [
    {
      key: 'landing.nav.createAccount',
      to: '/auth/login',
      variant: 'flat',
      color: 'primary',
      icon: 'mdi-login',
      emphasis: 'strong'
    },
    {
      key: 'nav.playground',
      to: '/docs/products/price/playground',
      variant: 'tonal',
      color: 'primary',
      icon: 'mdi-console-line',
      emphasis: 'light'
    }
  ]

  return {
    items,
    primaryActions,
    downloadTo: '/docs/products/price/documentation/java',
    cloudersContactTo: '/contact'
  }
}
