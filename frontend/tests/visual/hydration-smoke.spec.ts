import { expect, test } from '@playwright/test'

const frenchBaseUrl =
  process.env.PLAYWRIGHT_FRENCH_BASE_URL ?? 'http://localhost:3000'
const englishBaseUrl =
  process.env.PLAYWRIGHT_ENGLISH_BASE_URL ?? 'http://127.0.0.1:3000'

const sharedRoutes = [
  '/',
  '/blog',
  '/contact',
  '/docs-browser',
  '/feedback',
  '/impact-score',
  '/metriks',
  '/opendata',
  '/opendata/gtin',
  '/opendata/isbn',
  '/opensource',
  '/prompt',
]

const routeGroups = [
  {
    label: 'fr',
    baseUrl: frenchBaseUrl,
    routes: [
      ...sharedRoutes,
      '/comparer',
      '/mentions-legales',
      '/partenaires',
      '/politique-confidentialite',
      '/versions',
      '/rechercher',
      '/equipe',
    ],
  },
  {
    label: 'en',
    baseUrl: englishBaseUrl,
    routes: [
      ...sharedRoutes,
      '/compare',
      '/legal-notice',
      '/partners',
      '/data-privacy',
      '/releases',
      '/search',
      '/team',
    ],
  },
]

const ignoredConsoleErrors = [
  /Failed to load resource: the server responded with a status of 404.*favicon/i,
]

for (const group of routeGroups) {
  for (const route of group.routes) {
    test(`hydrates ${group.label} ${route} without console errors`, async ({
      page,
    }) => {
      const consoleErrors: string[] = []
      const pageErrors: string[] = []
      const failedResponses: string[] = []

      page.on('console', message => {
        if (message.type() !== 'error') {
          return
        }

        const text = message.text()
        if (ignoredConsoleErrors.some(pattern => pattern.test(text))) {
          return
        }

        consoleErrors.push(text)
      })

      page.on('pageerror', error => {
        pageErrors.push(error.message)
      })

      page.on('response', response => {
        const status = response.status()
        if (status >= 400) {
          failedResponses.push(`${status} ${response.url()}`)
        }
      })

      await page.goto(new URL(route, group.baseUrl).toString(), {
        waitUntil: 'domcontentloaded',
      })
      await page.waitForTimeout(1500)

      expect(pageErrors, `page errors on ${group.label} ${route}`).toEqual([])
      expect(
        failedResponses,
        `failed responses on ${group.label} ${route}`
      ).toEqual([])
      expect(
        consoleErrors,
        `console errors on ${group.label} ${route}`
      ).toEqual([])
      await expect(page.locator('.v-icon svg path[d^="mdi-"]')).toHaveCount(0)
    })
  }
}
