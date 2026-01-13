import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import {
  HOTJAR_RECORDING_COOKIE_MAX_AGE,
  HOTJAR_RECORDING_COOKIE_NAME,
  HOTJAR_RECORDING_COOKIE_VALUE,
} from '~~/shared/utils/hotjar-recording'

type RecordEntryHandler = (typeof import('./record-entry'))['default']

const getRequestURLMock = vi.hoisted(() => vi.fn())
const sendRedirectMock = vi.hoisted(() => vi.fn())
const setCookieMock = vi.hoisted(() => vi.fn())

vi.mock('h3', async importOriginal => ({
  ...(await importOriginal<typeof import('h3')>()),
  defineEventHandler: (fn: RecordEntryHandler) => fn,
  getRequestURL: getRequestURLMock,
  sendRedirect: sendRedirectMock,
  setCookie: setCookieMock,
}))

describe('record-entry middleware', () => {
  const event = {} as Parameters<RecordEntryHandler>[0]
  const originalEnv = process.env.NODE_ENV
  let handler: RecordEntryHandler

  beforeEach(async () => {
    vi.resetModules()
    getRequestURLMock.mockReset()
    sendRedirectMock.mockReset()
    setCookieMock.mockReset()
    process.env.NODE_ENV = 'test'
    handler = (await import('./record-entry')).default
  })

  afterEach(() => {
    process.env.NODE_ENV = originalEnv
  })

  it('sets the cookie and redirects when record query is present on home', () => {
    getRequestURLMock.mockReturnValue(new URL('https://nudger.fr/?record'))

    handler(event)

    expect(setCookieMock).toHaveBeenCalledWith(
      event,
      HOTJAR_RECORDING_COOKIE_NAME,
      HOTJAR_RECORDING_COOKIE_VALUE,
      {
        httpOnly: false,
        sameSite: 'lax',
        secure: false,
        path: '/',
        maxAge: HOTJAR_RECORDING_COOKIE_MAX_AGE,
      }
    )
    expect(sendRedirectMock).toHaveBeenCalledWith(event, '/', 302)
  })

  it('does nothing when the record query is missing', () => {
    getRequestURLMock.mockReturnValue(new URL('https://nudger.fr/'))

    handler(event)

    expect(setCookieMock).not.toHaveBeenCalled()
    expect(sendRedirectMock).not.toHaveBeenCalled()
  })

  it('does nothing on non-home paths', () => {
    getRequestURLMock.mockReturnValue(new URL('https://nudger.fr/about?record'))

    handler(event)

    expect(setCookieMock).not.toHaveBeenCalled()
    expect(sendRedirectMock).not.toHaveBeenCalled()
  })
})
