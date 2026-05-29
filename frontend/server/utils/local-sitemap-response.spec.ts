import { beforeEach, describe, expect, it, vi } from 'vitest'

const accessMock = vi.hoisted(() => vi.fn())
const createReadStreamMock = vi.hoisted(() => vi.fn(() => 'stream'))
const sendStreamMock = vi.hoisted(() => vi.fn(() => 'sent-stream'))
const setHeaderMock = vi.hoisted(() => vi.fn())
const createErrorMock = vi.hoisted(() =>
  vi.fn((input: { statusCode: number; statusMessage: string }) => input)
)
const getLocalSitemapFilePathMock = vi.hoisted(() => vi.fn())

vi.mock('node:fs/promises', () => ({
  __esModule: true,
  access: accessMock,
  default: { access: accessMock },
}))
vi.mock('node:fs', () => ({
  __esModule: true,
  createReadStream: createReadStreamMock,
  default: { createReadStream: createReadStreamMock },
}))
vi.mock('h3', () => ({
  createError: createErrorMock,
  sendStream: sendStreamMock,
  setHeader: setHeaderMock,
}))
vi.mock('~~/server/utils/sitemap-local-files', () => ({
  getLocalSitemapFilePath: getLocalSitemapFilePathMock,
}))

describe('local sitemap response', () => {
  beforeEach(() => {
    vi.resetModules()
    vi.clearAllMocks()
    accessMock.mockResolvedValue(undefined)
    getLocalSitemapFilePathMock.mockReturnValue('/tmp/product-pages.xml')
  })

  it('returns headers without a body for HEAD requests', async () => {
    const { handleLocalSitemapResponse } =
      await import('./local-sitemap-response')
    const event = {
      context: {
        params: { domainLanguage: 'fr', fileName: 'product-pages.xml' },
      },
    } as Parameters<typeof handleLocalSitemapResponse>[0]

    const response = await handleLocalSitemapResponse(event, {
      sendBody: false,
    })

    expect(response).toBeNull()
    expect(setHeaderMock).toHaveBeenCalledWith(
      event,
      'Content-Type',
      'application/xml'
    )
    expect(sendStreamMock).not.toHaveBeenCalled()
  })

  it('streams configured sitemap files for GET requests', async () => {
    const { handleLocalSitemapResponse } =
      await import('./local-sitemap-response')
    const event = {
      context: {
        params: { domainLanguage: 'fr', fileName: 'product-pages.xml' },
      },
    } as Parameters<typeof handleLocalSitemapResponse>[0]

    const response = await handleLocalSitemapResponse(event, {
      sendBody: true,
    })

    expect(createReadStreamMock).toHaveBeenCalledWith('/tmp/product-pages.xml')
    expect(sendStreamMock).toHaveBeenCalledWith(event, 'stream')
    expect(response).toBe('sent-stream')
  })
})
