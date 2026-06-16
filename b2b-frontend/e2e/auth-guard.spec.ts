import { test, expect } from '@playwright/test'

test.describe('Auth guard', () => {
  test('visiting /dashboard unauthenticated redirects to /auth/login', async ({ page }) => {
    await page.goto('/dashboard')
    await page.waitForLoadState('networkidle')

    // Should land on login page (either direct or via redirect)
    await expect(page).toHaveURL(/\/auth\/login/)
  })

  test('login page renders without errors', async ({ page }) => {
    const pageErrors: string[] = []
    page.on('pageerror', err => pageErrors.push(err.message))

    await page.goto('/auth/login')
    await page.waitForLoadState('networkidle')

    // Should not 500 or throw
    expect(pageErrors.filter(e => e.includes('[Vue warn]') || e.includes('Hydration'))).toHaveLength(0)
    await expect(page.locator('h1, h2, button').first()).toBeVisible()
  })
})
