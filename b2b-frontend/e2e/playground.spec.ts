import { test, expect } from '@playwright/test'

test.describe('Price playground — sample mode', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/docs/products/price/playground')
    await page.waitForLoadState('networkidle')
    // Ensure we are in sample mode (default)
    await page.getByRole('tab', { name: /sample/i }).click()
  })

  test('renders the playground page', async ({ page }) => {
    await expect(page.locator('h1, h2').first()).toBeVisible()
  })

  test('runs fresh GTIN sample and shows billable chip', async ({ page }) => {
    // Clear and set GTIN to fresh fixture
    const gtinInput = page.getByLabel(/gtin/i)
    await gtinInput.clear()
    await gtinInput.fill('0885909950805')

    await page.getByRole('button', { name: /run/i }).click()

    // Wait for response
    await expect(page.getByText(/billable/i)).toBeVisible({ timeout: 5000 })
    // Credits consumed should show
    await expect(page.getByText('5')).toBeVisible()
  })

  test('runs not-found GTIN sample and shows non-billable', async ({ page }) => {
    const gtinInput = page.getByLabel(/gtin/i)
    await gtinInput.clear()
    await gtinInput.fill('0000000000000')

    await page.getByRole('button', { name: /run/i }).click()

    await expect(page.getByText(/not.?billable/i)).toBeVisible({ timeout: 5000 })
  })

  test('shows validation error for invalid GTIN', async ({ page }) => {
    const gtinInput = page.getByLabel(/gtin/i)
    await gtinInput.clear()
    await gtinInput.fill('12345')

    await page.getByRole('button', { name: /run/i }).click()

    await expect(page.getByText(/invalid gtin/i)).toBeVisible({ timeout: 5000 })
  })

  test('stale GTIN returns non-billable response', async ({ page }) => {
    const gtinInput = page.getByLabel(/gtin/i)
    await gtinInput.clear()
    await gtinInput.fill('0194253408994')

    await page.getByRole('button', { name: /run/i }).click()

    await expect(page.getByText(/not.?billable/i)).toBeVisible({ timeout: 5000 })
    await expect(page.getByText('stale-data')).toBeVisible({ timeout: 5000 })
  })
})
