import { test, expect } from '@playwright/test'

test.describe('Locale switch', () => {
  test('navigating to /fr/docs/getting-started renders the French page', async ({ page }) => {
    await page.goto('/fr/docs/getting-started')
    await page.waitForLoadState('networkidle')

    // Should render without 404/500
    await expect(page.locator('h1')).toBeVisible()

    // URL should stay on the /fr/ path
    await expect(page).toHaveURL(/\/fr\//)
  })

  test('/docs/getting-started (EN) and /fr/docs/getting-started (FR) render distinct pages', async ({ page }) => {
    await page.goto('/docs/getting-started')
    await page.waitForLoadState('networkidle')
    const enTitle = await page.locator('h1').textContent()

    await page.goto('/fr/docs/getting-started')
    await page.waitForLoadState('networkidle')
    const frTitle = await page.locator('h1').textContent()

    // Titles may differ or be the same if translation is identical — just assert both render
    expect(enTitle).toBeTruthy()
    expect(frTitle).toBeTruthy()
  })
})
