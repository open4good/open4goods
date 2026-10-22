import { readFileSync } from 'node:fs'
import path from 'node:path'
import { test, expect } from '@playwright/test'

test('metriks page loads without 500 error', async ({ page }) => {
  await page.goto('/metriks')

  // Check that we don't have a 500 error
  await expect(page.getByText('500', { exact: true })).not.toBeVisible()
  await expect(page.getByText('Server Error')).not.toBeVisible()
  await expect(page.getByText('ReferenceError')).not.toBeVisible()

  // If not logged in/admin, we expect the access denied state
  // "Login to access metrics" or similar (t('metriks.loginCta'))
  // or just check that the wrapper exists
  // We can check for "Métriques" or "Metriques" or whatever the translation for pageTitle is,
  // but access denied might show different text.
  // The code shows:
  // :headline="t('metriks.accessDenied')"
  // :title="t('metriks.pageSubtitle')"
  // v-btn to="/auth/login" with t('metriks.loginCta')

  // Let's check for the button to login if we assume we are not logged in
  // or check that the main container is present
})

const FIXTURE_DIR = path.resolve(
  process.cwd(),
  'server/fixtures/metriks/data/open4goods-api'
)

/** Unsigned JWT: only the client decodes it; the /api/metriks routes are mocked below. */
const adminToken = () => {
  const b64 = (o: object) =>
    Buffer.from(JSON.stringify(o)).toString('base64url')
  return `${b64({ alg: 'none' })}.${b64({ sub: 'admin', roles: ['ROLE_SITEEDITOR'] })}.sig`
}

test.describe('metriks dashboard — GSC tops, euros and errors', () => {
  test.beforeEach(async ({ page, context, baseURL }) => {
    await context.addCookies([
      { name: 'access_token', value: adminToken(), url: baseURL! },
    ])
    await page.route('**/api/metriks/**', async route => {
      const url = new URL(route.request().url())
      if (url.pathname.endsWith('/providers')) {
        return route.fulfill({ json: ['open4goods-api'] })
      }
      if (url.pathname.endsWith('/weekly/latest')) {
        return route.fulfill({ json: null })
      }
      if (url.pathname.endsWith('/latest')) {
        return route.fulfill({
          contentType: 'application/json',
          body: readFileSync(path.join(FIXTURE_DIR, 'latest.json'), 'utf-8'),
        })
      }
      if (url.pathname.endsWith('/history')) {
        return route.fulfill({
          contentType: 'text/plain',
          body: readFileSync(path.join(FIXTURE_DIR, 'events.ndjson'), 'utf-8'),
        })
      }
      return route.continue()
    })
  })

  test('renders the Search Console tops as tables with a "new" marker', async ({
    page,
  }) => {
    await page.goto('/metriks')
    const queries = page.getByTestId('metriks-top-seo.gsc.top_queries')
    await expect(queries).toBeVisible()
    await expect(queries.locator('table')).toBeVisible()
    await expect(queries.getByText('meilleur lave-linge 2026')).toBeVisible()
    await expect(
      queries
        .getByTestId('metriks-top-evolution')
        .filter({ hasText: /nouveau|new/i })
    ).toHaveCount(1)
    await expect(
      page.getByTestId('metriks-top-seo.gsc.top_pages').locator('table')
    ).toBeVisible()
  })

  test('formats affiliation amounts in euros', async ({ page }) => {
    await page.goto('/metriks')
    const row = page
      .locator('tr')
      .filter({ hasText: 'Affiliation awin : Commissions validées (7d)' })
    await expect(row).toContainText(/412[.,]50/)
    await expect(row).toContainText('€')
  })

  test('shows the error message instead of 0 for a failing network', async ({
    page,
  }) => {
    await page.goto('/metriks')
    const errors = page.getByTestId('metrik-error')
    await expect(errors.first()).toBeVisible()
    await expect(errors.first()).toContainText('Credential manquant')
    const row = page
      .locator('tr')
      .filter({ hasText: 'rakuten : Commissions validées (7d)' })
    await expect(row).toContainText('Credential manquant')
    await expect(row).not.toContainText(/^\s*0(,00)?\s*€/)
  })

  test('shows the previous 28-day value next to the GSC totals', async ({
    page,
  }) => {
    await page.goto('/metriks')
    const row = page.locator('tr').filter({ hasText: 'Search Console : clics' })
    await expect(row).toContainText(/28 j précédents|Previous 28 d/)
  })
})
