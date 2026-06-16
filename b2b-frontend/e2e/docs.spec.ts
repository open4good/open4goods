import { test, expect } from '@playwright/test'

test.describe('Docs', () => {
  test('docs index renders content explorer', async ({ page }) => {
    await page.goto('/docs')
    await page.waitForLoadState('networkidle')
    // Page renders without a 500 or blank
    await expect(page.locator('h1')).toBeVisible()
  })

  test('getting-started doc renders via ContentRenderer', async ({ page }) => {
    await page.goto('/docs/getting-started')
    await page.waitForLoadState('networkidle')
    await expect(page.locator('h1')).toBeVisible()
    // Verify content is rendered (not just a shell)
    await expect(page.locator('main, article, .nuxt-content, [class*="content"]').first()).toBeVisible()
  })

  test('products/price doc page renders', async ({ page }) => {
    await page.goto('/docs/products/price')
    await page.waitForLoadState('networkidle')
    await expect(page.locator('h1')).toBeVisible()
  })

  test('French locale mirrors English docs at /fr/docs', async ({ page }) => {
    await page.goto('/fr/docs/getting-started')
    await page.waitForLoadState('networkidle')
    // Should render — not 404 or blank
    await expect(page.locator('h1')).toBeVisible()
  })
})
