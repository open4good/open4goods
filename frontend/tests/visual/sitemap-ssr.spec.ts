import { expect, test } from '@playwright/test'

const ALLOWED_CONSOLE_ERROR_PATTERNS = [
  /Failed to fetch/i,
  /NetworkError/i,
  /ECONNREFUSED/i,
  /fetch failed/i,
  /TypeError: Failed to fetch/i,
]

const parseSitemapLocs = (xml: string): string[] =>
  Array.from(xml.matchAll(/<loc>(.*?)<\/loc>/g))
    .map(match => match[1].trim())
    .filter(Boolean)

const isAllowedConsoleError = (message: string): boolean =>
  ALLOWED_CONSOLE_ERROR_PATTERNS.some(pattern => pattern.test(message))

test('sitemap main-pages URLs return 200 without hydration errors', async (
  { page, request },
  testInfo,
) => {
  test.setTimeout(300000)

  const baseUrl = testInfo.project.use.baseURL ?? 'http://localhost:3000'
  const baseOrigin = new URL(baseUrl).origin

  const sitemapIndexResponse = await request.get('/sitemap.xml')
  expect(sitemapIndexResponse.status(), 'sitemap.xml should return 200').toBe(200)

  const sitemapIndexXml = await sitemapIndexResponse.text()
  const mainPagesUrl = new URL('/sitemap/main-pages.xml', baseUrl).toString()
  expect(
    sitemapIndexXml,
    'sitemap.xml should reference /sitemap/main-pages.xml',
  ).toContain(mainPagesUrl)

  const mainPagesResponse = await request.get('/sitemap/main-pages.xml')
  expect(mainPagesResponse.status(), 'main-pages sitemap should return 200').toBe(
    200,
  )

  const mainPagesXml = await mainPagesResponse.text()
  const sitemapUrls = parseSitemapLocs(mainPagesXml)
  expect(sitemapUrls.length, 'main-pages sitemap should list URLs').toBeGreaterThan(
    0,
  )

  const consoleErrors: string[] = []
  const pageErrors: string[] = []
  const requestFailures: string[] = []

  page.on('console', message => {
    if (message.type() !== 'error') {
      return
    }

    const text = message.text().trim()
    if (!text || isAllowedConsoleError(text)) {
      return
    }

    consoleErrors.push(text)
  })

  page.on('pageerror', error => {
    const message = error.message?.trim()
    if (message) {
      pageErrors.push(message)
    }
  })

  page.on('requestfailed', requestInfo => {
    const url = new URL(requestInfo.url())
    if (url.origin !== baseOrigin) {
      return
    }

    const failure = requestInfo.failure()?.errorText ?? 'unknown error'
    requestFailures.push(`${requestInfo.method()} ${requestInfo.url()} - ${failure}`)
  })

  for (const sitemapUrl of new Set(sitemapUrls)) {
    consoleErrors.length = 0
    pageErrors.length = 0
    requestFailures.length = 0

    const parsedUrl = new URL(sitemapUrl)
    const targetUrl = new URL(parsedUrl.pathname + parsedUrl.search, baseUrl).toString()

    const response = await page.goto(targetUrl, {
      waitUntil: 'domcontentloaded',
    })

    expect(response?.status(), `SSR response should be 200 for ${targetUrl}`).toBe(
      200,
    )

    await page.waitForTimeout(500)

    expect(
      requestFailures,
      `SSR requests should not fail for ${targetUrl}`,
    ).toEqual([])
    expect(pageErrors, `No page errors expected for ${targetUrl}`).toEqual([])
    expect(
      consoleErrors,
      `Console errors detected for ${targetUrl}`,
    ).toEqual([])
  }
})
