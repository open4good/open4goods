import { describe, expect, it } from 'vitest'

import {
  buildGoogleAuthorizationUrl,
  createGoogleSsoAttempt,
  parsePendingState,
  serializePendingState,
} from './google-sso'

describe('createGoogleSsoAttempt', () => {
  it('generates unique state, nonce and PKCE pair bound to the redirect target', () => {
    const first = createGoogleSsoAttempt('/account')
    const second = createGoogleSsoAttempt('/account')

    expect(first.pending.redirect).toBe('/account')
    expect(first.pending.state).not.toBe(second.pending.state)
    expect(first.pending.nonce).not.toBe(second.pending.nonce)
    expect(first.pending.codeVerifier).not.toBe(second.pending.codeVerifier)
    expect(first.codeChallenge).not.toBe(second.codeChallenge)
  })
})

describe('serializePendingState / parsePendingState', () => {
  it('round-trips a pending state', () => {
    const { pending } = createGoogleSsoAttempt('/account')

    const parsed = parsePendingState(serializePendingState(pending))

    expect(parsed).toEqual(pending)
  })

  it('rejects a missing cookie value', () => {
    expect(parsePendingState(undefined)).toBeNull()
    expect(parsePendingState(null)).toBeNull()
    expect(parsePendingState('')).toBeNull()
  })

  it('rejects an unparsable cookie value', () => {
    expect(parsePendingState('not-json')).toBeNull()
  })

  it('rejects a cookie value missing required fields', () => {
    expect(parsePendingState(JSON.stringify({ state: 'only-state' }))).toBeNull()
  })
})

describe('buildGoogleAuthorizationUrl', () => {
  it('builds an authorization URL carrying state, nonce and PKCE challenge', () => {
    const url = new URL(
      buildGoogleAuthorizationUrl({
        clientId: 'client-123',
        redirectUri: 'http://127.0.0.1:4100/auth/google/callback',
        state: 'state-abc',
        nonce: 'nonce-xyz',
        codeChallenge: 'challenge-456',
      })
    )

    expect(url.origin + url.pathname).toBe(
      'https://accounts.google.com/o/oauth2/v2/auth'
    )
    expect(url.searchParams.get('client_id')).toBe('client-123')
    expect(url.searchParams.get('redirect_uri')).toBe(
      'http://127.0.0.1:4100/auth/google/callback'
    )
    expect(url.searchParams.get('response_type')).toBe('code')
    expect(url.searchParams.get('scope')).toBe('openid email')
    expect(url.searchParams.get('state')).toBe('state-abc')
    expect(url.searchParams.get('nonce')).toBe('nonce-xyz')
    expect(url.searchParams.get('code_challenge')).toBe('challenge-456')
    expect(url.searchParams.get('code_challenge_method')).toBe('S256')
  })
})
