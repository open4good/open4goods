const localPathPattern = /^\/(?!\/)(?!\\)/

/**
 * Keeps post-auth redirects on the current frontend origin.
 */
export function normalizeLocalRedirectPath(value: unknown, fallback = '/') {
  if (typeof value !== 'string') {
    return fallback
  }

  const trimmed = value.trim()
  if (!localPathPattern.test(trimmed) || trimmed.includes('\u0000')) {
    return fallback
  }

  return trimmed
}

/**
 * OIDC callbacks must return to the fixed server-side callback route.
 */
export function normalizeOidcCallbackPath(value: unknown, provider: string) {
  const expected = `/auth/callback/${provider}`
  const candidate = normalizeLocalRedirectPath(value, expected)
  return candidate === expected ? candidate : expected
}
