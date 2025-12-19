import { expect, test } from '@playwright/test'

test.describe('Product Page visual regression', () => {
  test('renders product page correctly', async ({ page }) => {
    // Go to the target product page
    // Using relative path, requires PLAYWRIGHT_BASE_URL to be set correctly
    await page.goto(
      '/televiseurs/8718863041000-television-philips-40pfs600912-2024'
    )

    // Check title (case insensitive)
    await expect(page).toHaveTitle(/philips/i)

    // Wait for main content
    await page.waitForSelector('.product-hero')

    // Small delay to ensure rendering settles
    await page.waitForTimeout(2000)

    // Take screenshots
    await expect(page).toHaveScreenshot('product-page-mobile.png', {
      fullPage: true,
      maxDiffPixelRatio: 0.05,
    })
  })
})
