import { expect, test, type Page } from '@playwright/test'

// Stable reference product (rich data, verified offers) used across the
// SEO/UI audit as the "TV" reference URL.
const PRODUCT_PATH =
  '/televiseurs/8806096715659-televiseur-lg-55qned7eb3c-2026'

// The dev server's PWA plugin fails to emit its service worker
// (`.nuxt/dev-sw-dist/sw.js` ENOENT), which Vite surfaces as a full-page
// error overlay unrelated to application code. It only appears in `pnpm
// dev` (Playwright's webServer), never in a production build. Dismiss it
// so visual assertions see the real page, not the dev-only overlay.
const dismissDevErrorOverlay = (page: Page) =>
  page.evaluate(() => document.querySelector('vite-error-overlay')?.remove())

test.describe('product page fold', () => {
  test('desktop hero matches the recorded baseline', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await page.goto(PRODUCT_PATH)
    await page.waitForLoadState('networkidle')
    await dismissDevErrorOverlay(page)

    await expect(page).toHaveScreenshot('product-fold-desktop.png', {
      fullPage: false,
    })
  })

  test('mobile hero matches the recorded baseline and surfaces the price CTA above the fold', async ({
    page,
  }) => {
    await page.setViewportSize({ width: 390, height: 844 })
    await page.goto(PRODUCT_PATH)
    await page.waitForLoadState('networkidle')
    await dismissDevErrorOverlay(page)

    // The bold mobile reorder (WP5.4) puts the price/CTA block ahead of the
    // gallery specifically so it is visible without scrolling.
    const priceActions = page.locator('.product-hero__price-actions').first()
    await expect(priceActions).toBeVisible()
    const box = await priceActions.boundingBox()
    expect(box).not.toBeNull()
    expect(box!.y + box!.height).toBeLessThanOrEqual(844)

    await expect(page).toHaveScreenshot('product-fold-mobile.png', {
      fullPage: false,
    })
  })
})
