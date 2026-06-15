export interface OidcProviderConfig {
  key: string
  authorizeEndpoint: string
  tokenEndpoint: string
  clientId: string
  clientSecret: string
  scopes: string
  redirectUri: string
}

function runtimeValue(configValue: unknown, ...envNames: string[]) {
  // Keep the explicit priority order: canonical NUXT_* names must beat legacy aliases.
  for (const name of envNames) {
    const value = process.env[name]
    if (value?.trim()) {
      return value
    }
  }

  if (typeof configValue === 'string' && configValue.trim()) {
    return configValue
  }

  return ''
}

function googleAuthorizeIssuer(value: string) {
  return value.includes('oauth2.googleapis.com') ? 'https://accounts.google.com' : value
}

function githubScopes(value: string) {
  return !value || value.includes('openid') ? 'read:user user:email' : value
}

function githubAuthorizeIssuer(value: string) {
  const issuer = value.trim()
  if (issuer.startsWith('https://github.com') && !issuer.includes('/login/oauth')) {
    return 'https://github.com/login/oauth'
  }
  return issuer
}

export function resolveOidcProviderConfig(key: string, config: ReturnType<typeof useRuntimeConfig>): OidcProviderConfig | null {
  switch (key) {
    case 'google':
      return {
        key,
        authorizeEndpoint: `${googleAuthorizeIssuer(runtimeValue(config.oidcGoogleIssuer, 'NUXT_OIDC_GOOGLE_ISSUER', 'OIDC_GOOGLE_ISSUER') || 'https://accounts.google.com')}/o/oauth2/v2/auth`,
        tokenEndpoint: `${runtimeValue(config.oidcGoogleTokenEndpoint, 'NUXT_OIDC_GOOGLE_TOKEN_ENDPOINT', 'OIDC_GOOGLE_TOKEN_ENDPOINT') || 'https://oauth2.googleapis.com/token'}`,
        clientId: runtimeValue(config.oidcGoogleClientId, 'NUXT_OIDC_GOOGLE_CLIENT_ID', 'OIDC_GOOGLE_CLIENT_ID', 'NUXT_PUBLIC_GOOGLE_OIDC_CLIENT_ID'),
        clientSecret: runtimeValue(config.oidcGoogleClientSecret, 'NUXT_OIDC_GOOGLE_CLIENT_SECRET', 'OIDC_GOOGLE_CLIENT_SECRET'),
        scopes: runtimeValue(config.oidcGoogleScopes, 'NUXT_OIDC_GOOGLE_SCOPES', 'OIDC_GOOGLE_SCOPES') || 'openid profile email',
        redirectUri: runtimeValue(config.oidcGoogleRedirectUri, 'NUXT_OIDC_GOOGLE_REDIRECT_URI', 'OIDC_GOOGLE_REDIRECT_URI')
      }
    case 'microsoft':
      return {
        key,
        authorizeEndpoint: `${runtimeValue(config.oidcMicrosoftIssuer, 'NUXT_OIDC_MICROSOFT_ISSUER', 'OIDC_MICROSOFT_ISSUER') || 'https://login.microsoftonline.com/common/oauth2/v2.0'}/authorize`,
        tokenEndpoint: `${runtimeValue(config.oidcMicrosoftTokenEndpoint, 'NUXT_OIDC_MICROSOFT_TOKEN_ENDPOINT', 'OIDC_MICROSOFT_TOKEN_ENDPOINT') || 'https://login.microsoftonline.com/common/oauth2/v2.0/token'}`,
        clientId: runtimeValue(config.oidcMicrosoftClientId, 'NUXT_OIDC_MICROSOFT_CLIENT_ID', 'OIDC_MICROSOFT_CLIENT_ID'),
        clientSecret: runtimeValue(config.oidcMicrosoftClientSecret, 'NUXT_OIDC_MICROSOFT_CLIENT_SECRET', 'OIDC_MICROSOFT_CLIENT_SECRET'),
        scopes: runtimeValue(config.oidcMicrosoftScopes, 'NUXT_OIDC_MICROSOFT_SCOPES', 'OIDC_MICROSOFT_SCOPES') || 'openid profile email',
        redirectUri: runtimeValue(config.oidcMicrosoftRedirectUri, 'NUXT_OIDC_MICROSOFT_REDIRECT_URI', 'OIDC_MICROSOFT_REDIRECT_URI')
      }
    case 'github':
      return {
        key,
        authorizeEndpoint: `${githubAuthorizeIssuer(runtimeValue(config.oidcGithubIssuer, 'NUXT_OIDC_GITHUB_ISSUER', 'OIDC_GITHUB_ISSUER') || 'https://github.com/login/oauth')}/authorize`,
        tokenEndpoint: `${runtimeValue(config.oidcGithubTokenEndpoint, 'NUXT_OIDC_GITHUB_TOKEN_ENDPOINT', 'OIDC_GITHUB_TOKEN_ENDPOINT') || 'https://github.com/login/oauth/access_token'}`,
        clientId: runtimeValue(config.oidcGithubClientId, 'NUXT_OIDC_GITHUB_CLIENT_ID', 'OIDC_GITHUB_CLIENT_ID'),
        clientSecret: runtimeValue(config.oidcGithubClientSecret, 'NUXT_OIDC_GITHUB_CLIENT_SECRET', 'OIDC_GITHUB_CLIENT_SECRET'),
        scopes: githubScopes(runtimeValue(config.oidcGithubScopes, 'NUXT_OIDC_GITHUB_SCOPES', 'OIDC_GITHUB_SCOPES')),
        redirectUri: runtimeValue(config.oidcGithubRedirectUri, 'NUXT_OIDC_GITHUB_REDIRECT_URI', 'OIDC_GITHUB_REDIRECT_URI')
      }
    case 'apple':
      return {
        key,
        authorizeEndpoint: `${runtimeValue(config.oidcAppleIssuer, 'NUXT_OIDC_APPLE_ISSUER', 'OIDC_APPLE_ISSUER') || 'https://appleid.apple.com'}/auth/authorize`,
        tokenEndpoint: `${runtimeValue(config.oidcAppleTokenEndpoint, 'NUXT_OIDC_APPLE_TOKEN_ENDPOINT', 'OIDC_APPLE_TOKEN_ENDPOINT') || 'https://appleid.apple.com/auth/token'}`,
        clientId: runtimeValue(config.oidcAppleClientId, 'NUXT_OIDC_APPLE_CLIENT_ID', 'OIDC_APPLE_CLIENT_ID'),
        clientSecret: runtimeValue(config.oidcAppleClientSecret, 'NUXT_OIDC_APPLE_CLIENT_SECRET', 'OIDC_APPLE_CLIENT_SECRET'),
        scopes: runtimeValue(config.oidcAppleScopes, 'NUXT_OIDC_APPLE_SCOPES', 'OIDC_APPLE_SCOPES') || 'openid name email',
        redirectUri: runtimeValue(config.oidcAppleRedirectUri, 'NUXT_OIDC_APPLE_REDIRECT_URI', 'OIDC_APPLE_REDIRECT_URI')
      }

    default:
      return null
  }
}
