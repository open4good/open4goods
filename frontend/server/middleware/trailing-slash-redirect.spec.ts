import { beforeEach, describe, expect, it, vi } from 'vitest'

const sendRedirectMock = vi.hoisted(() =>
  vi.fn((_e, to, code) => ({ to, code }))
)

vi.mock('h3', () => ({ sendRedirect: sendRedirectMock }))
vi.stubGlobal('defineEventHandler', (handler: unknown) => handler)

const run = async (path: string, method = 'GET') => {
  const handler = (await import('./trailing-slash-redirect')).default as (
    event: unknown
  ) => unknown
  return handler({ path, method, node: { req: { url: path } } })
}

describe('trailing-slash-redirect', () => {
  beforeEach(() => sendRedirectMock.mockClear())

  it('redirects /impact-score/ and /{vertical}/ecoscore/ with a 301', async () => {
    await run('/impact-score/')
    expect(sendRedirectMock).toHaveBeenLastCalledWith(
      expect.anything(),
      '/impact-score',
      301
    )
    await run('/televiseurs/ecoscore/?a=1')
    expect(sendRedirectMock).toHaveBeenLastCalledWith(
      expect.anything(),
      '/televiseurs/ecoscore?a=1',
      301
    )
  })

  it('ignores other paths and non-GET methods', async () => {
    await run('/impact-score')
    await run('/televiseurs/')
    await run('/impact-score/', 'POST')
    expect(sendRedirectMock).not.toHaveBeenCalled()
  })
})
