import { resolveOidcProviderConfig } from '~/server/utils/oidcProviders'

interface OidcProvidersAvailability {
  google: boolean
  microsoft: boolean
  github: boolean
  apple: boolean
}

/**
 * Exposes provider availability without leaking provider secrets to the client.
 */
export default defineEventHandler((event): OidcProvidersAvailability => {
  const config = useRuntimeConfig(event)

  const isConfigured = (provider: 'google' | 'microsoft' | 'github' | 'apple') => {
    const providerConfig = resolveOidcProviderConfig(provider, config)
    return Boolean(providerConfig?.clientId && providerConfig.clientSecret)
  }

  return {
    google: isConfigured('google'),
    microsoft: isConfigured('microsoft'),
    github: isConfigured('github'),
    apple: isConfigured('apple')
  }
})
