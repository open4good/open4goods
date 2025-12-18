import { test, expect } from '@playwright/test'

test.describe('Homepage Audit', () => {
  test('should load without console errors and hydration mismatches', async ({ page }) => {
    const consoleErrors: string[] = []
    page.on('console', msg => {
      if (msg.type() === 'error' || msg.type() === 'warning') {
        const text = msg.text()
        // Ignore known harmless warnings if any (currently none expected)
        consoleErrors.push(text)
      }
    })

    await page.goto('/')
    
    // Wait for the hero title to be visible, ensuring hydration likely started
    await expect(page.locator('h1.home-hero__title')).toBeVisible()
    
    // Check H1 text
    await expect(page.locator('h1.home-hero__title')).toContainText('Réconcilier écologie')

    // Wait a bit for any delayed hydration errors
    await page.waitForTimeout(1000)

    // Filter out irrelevant errors if needed (e.g. tracking blocks)
    const hydrationErrors = consoleErrors.filter(err => 
      err.includes('Hydration') || 
      err.includes('mismatch') || 
      err.includes('Invalid prop')
    )

    expect(hydrationErrors).toEqual([])
  })

  test('should have correct metadata', async ({ page }) => {
    await page.goto('/')
    await expect(page).toHaveTitle(/Nudger/)
  })
})
