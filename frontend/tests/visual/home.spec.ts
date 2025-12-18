import { expect, test } from '@playwright/test'

test.describe('Homepage visual regression', () => {
  test('renders landing hero', async ({ page }) => {
    await page.goto('/')
    await page.waitForLoadState('networkidle')
    await expect(page).toHaveScreenshot('home.png', { fullPage: true })
  })
})
