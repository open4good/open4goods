import { beforeEach, describe, expect, it, vi } from 'vitest'

const resolveAffiliationRedirectMock = vi.hoisted(() => vi.fn())
const sendRedirectMock = vi.hoisted(() => vi.fn())
const setHeaderMock = vi.hoisted(() => vi.fn())

vi.mock('h3', () => ({
  defineEventHandler: (fn: unknown) => fn,
  sendRedirect: sendRedirectMock,
  setHeader: setHeaderMock,
}))

vi.mock('../../utils/affiliation-redirect', () => ({
  resolveAffiliationRedirect: resolveAffiliationRedirectMock,
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
    const handler = (await import('./[token]')).default
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
  })
})
