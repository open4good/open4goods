import { beforeEach, describe, expect, it, vi } from 'vitest'

const resolveAffiliationRedirectMock = vi.hoisted(() => vi.fn())
const sendRedirectMock = vi.hoisted(() => vi.fn())
const sendContribPlainErrorMock = vi.hoisted(() => vi.fn())
const setHeaderMock = vi.hoisted(() => vi.fn())

vi.mock('h3', () => ({
  defineEventHandler: (fn: unknown) => fn,
  sendRedirect: sendRedirectMock,
  setHeader: setHeaderMock,
}))

vi.mock('../../utils/affiliation-redirect', () => ({
  resolveAffiliationRedirect: resolveAffiliationRedirectMock,
  sendContribPlainError: sendContribPlainErrorMock,
}))

describe('contrib redirect route', () => {
  beforeEach(() => {
    vi.resetModules()
    vi.clearAllMocks()
  })

  it('returns the backend redirect without rendering an HTML page', async () => {
    resolveAffiliationRedirectMock.mockResolvedValue({
      statusCode: 301,
      location: 'https://merchant.example/product/123',
    })
    const handler = (await import('./[...token]')).default
    const event = {} as Parameters<typeof handler>[0]

    await handler(event)

    expect(resolveAffiliationRedirectMock).toHaveBeenCalledWith(event)
    expect(setHeaderMock).toHaveBeenCalledWith(
      event,
      'X-Robots-Tag',
      'noindex, nofollow'
    )
    expect(sendRedirectMock).toHaveBeenCalledWith(
      event,
      'https://merchant.example/product/123',
      301
    )
    expect(setHeaderMock).toHaveBeenCalledWith(
      event,
      'Referrer-Policy',
      'no-referrer'
    )
    expect(setHeaderMock).toHaveBeenCalledWith(
      event,
      'Cache-Control',
      'no-store'
    )
  })

  it('sends only the origin to an Amazon search target (GOU-1396)', async () => {
    const amazon = 'https://www.amazon.fr/s?k=4006381333931&tag=nudger-21'
    resolveAffiliationRedirectMock.mockResolvedValue({
      statusCode: 302,
      location: amazon,
    })
    const handler = (await import('./[...token]')).default
    const event = {} as Parameters<typeof handler>[0]

    await handler(event)

    expect(sendRedirectMock).toHaveBeenCalledWith(event, amazon, 302)
    expect(setHeaderMock).toHaveBeenCalledWith(
      event,
      'Referrer-Policy',
      'strict-origin-when-cross-origin'
    )
    expect(setHeaderMock).not.toHaveBeenCalledWith(
      event,
      'Referrer-Policy',
      'no-referrer'
    )
    expect(setHeaderMock).toHaveBeenCalledWith(
      event,
      'Cache-Control',
      'no-store'
    )
  })

  it('answers errors (403 robot, 400 token) with a plain-text response', async () => {
    const failure = { statusCode: 403, statusMessage: 'Forbidden' }
    resolveAffiliationRedirectMock.mockRejectedValue(failure)
    sendContribPlainErrorMock.mockResolvedValue(undefined)
    const handler = (await import('./[...token]')).default
    const event = {} as Parameters<typeof handler>[0]

    await handler(event)

    expect(sendRedirectMock).not.toHaveBeenCalled()
    expect(setHeaderMock).not.toHaveBeenCalledWith(
      event,
      'Referrer-Policy',
      expect.anything()
    )
    expect(sendContribPlainErrorMock).toHaveBeenCalledWith(event, failure)
  })
})
