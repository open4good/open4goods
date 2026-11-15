import { describe, expect, it } from 'vitest'
import { isLikelyAutomatedBrowser } from './automated-browser'

const win = (v: number) =>
  `Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/${v}.0.0.0 Safari/537.36`
const brands = (v: string, ...names: string[]) => ({
  brands: [
    { brand: 'Not A(Brand', version: '99' },
    ...names.map(brand => ({ brand, version: v })),
  ],
})

describe('isLikelyAutomatedBrowser', () => {
  const secure = { isSecureContext: true }

  it.each([
    ['Tencent', { userAgent: win(133) }],
    [
      'version mismatch',
      { userAgent: win(105), userAgentData: brands('150', 'Chromium') },
    ],
    ['webdriver', { userAgent: 'x', webdriver: true }],
    [
      'HeadlessChrome',
      { userAgent: win(120).replace('Chrome/', 'HeadlessChrome/') },
    ],
    ['Lightpanda', { userAgent: 'Lightpanda/1.0' }],
  ])('flags %s', (_n, s) => {
    expect(isLikelyAutomatedBrowser({ ...secure, ...s })).toBe(true)
  })

  it.each([
    [
      'Chrome 142',
      {
        userAgent: win(142),
        userAgentData: brands('142', 'Google Chrome', 'Chromium'),
      },
    ],
    [
      'Edge 142',
      {
        userAgent: win(142) + ' Edg/142.0.0.0',
        userAgentData: brands('142', 'Chromium', 'Microsoft Edge'),
      },
    ],
    [
      'Android WebView',
      {
        userAgent:
          'Mozilla/5.0 (Linux; Android 13; Pixel; wv) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36',
      },
    ],
    [
      'CriOS',
      {
        userAgent:
          'Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 CriOS/120.0.0.0 Mobile/15E148 Safari/604.1',
      },
    ],
    [
      'Firefox',
      {
        userAgent:
          'Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:130.0) Gecko/20100101 Firefox/130.0',
      },
    ],
    [
      'Safari',
      {
        userAgent:
          'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 Version/17.0 Safari/605.1.15',
      },
    ],
  ])('keeps %s', (_n, s) => {
    expect(isLikelyAutomatedBrowser({ ...secure, ...s })).toBe(false)
  })

  it('ignores rule 3 in insecure contexts', () => {
    expect(
      isLikelyAutomatedBrowser({ userAgent: win(133), isSecureContext: false })
    ).toBe(false)
  })
})
