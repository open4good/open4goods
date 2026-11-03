import { beforeEach, describe, expect, it, vi } from 'vitest'

const resolveMock = vi.hoisted(() => vi.fn())
const plainErrorMock = vi.hoisted(() => vi.fn())

const setHeaderMock = vi.hoisted(() => vi.fn())

vi.mock('h3', () => ({
  defineEventHandler: (fn: unknown) => fn,
  setHeader: setHeaderMock,
}))
vi.mock('../../utils/affiliation-redirect', () => ({
  resolveAffiliationRedirect: resolveMock,
  sendContribPlainError: plainErrorMock,
}))

describe('server/api/contrib/[token] handler', () => {
  beforeEach(() => vi.clearAllMocks())

  it('returns the redirect payload', async () => {
    resolveMock.mockResolvedValue({
      statusCode: 301,
      location: 'https://x.test',
    })
    const handler = (await import('./[token]')).default
    const event = {} as never
    await expect(handler(event)).resolves.toEqual({
      statusCode: 301,
      location: 'https://x.test',
    })
    expect(setHeaderMock).toHaveBeenCalledWith(
      event,
      'Cache-Control',
      'no-store'
    )
    expect(setHeaderMock).toHaveBeenCalledWith(
      event,
      'X-Robots-Tag',
      'noindex, nofollow'
    )
    expect(setHeaderMock).toHaveBeenCalledWith(
      event,
      'Referrer-Policy',
      'no-referrer'
    )
  })

  it('turns errors into plain text responses', async () => {
    const failure = { statusCode: 403 }
    resolveMock.mockRejectedValue(failure)
    const handler = (await import('./[token]')).default
    const event = {} as never
    await handler(event)
    expect(plainErrorMock).toHaveBeenCalledWith(event, failure)
    expect(setHeaderMock).not.toHaveBeenCalled()
  })
})
