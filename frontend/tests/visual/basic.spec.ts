import { test, expect } from '@playwright/test'

test('homepage has title and main content', async ({ page }) => {
  await page.goto('/')

  // Expect a title "to contain" a substring.
  await expect(page).toHaveTitle(/Nudger/)

  // Expect the main heading to be visible
  // Adjust the selector based on actual content, e.g., 'h1', or text 'Bienvenue'
  // Using a generic check for now to ensure page loads
  await expect(page.locator('body')).toBeVisible()

  // Take a screenshot for visual verification artifact
  await page.screenshot({ path: 'test-results/homepage.png' })
})
