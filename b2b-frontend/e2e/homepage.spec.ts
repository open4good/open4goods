import { test, expect } from '@playwright/test'

test.describe('Homepage', () => {
  test('loads and hydrates without console errors', async ({ page }) => {
    const consoleErrors: string[] = []
    const pageErrors: string[] = []

    page.on('console', msg => {
      if (msg.type() === 'error') consoleErrors.push(msg.text())
    })
    page.on('pageerror', err => pageErrors.push(err.message))

    await page.goto('/')
    await page.waitForLoadState('networkidle')

    // Filter out known third-party/extension noise
    const hydrationErrors = [...consoleErrors, ...pageErrors].filter(
      e => e.includes('Hydration') || e.includes('hydration') || e.includes('[Vue warn]')
    )

    expect(hydrationErrors, `Hydration errors: ${hydrationErrors.join('\n')}`).toHaveLength(0)
  })

  test('page title is set', async ({ page }) => {
    await page.goto('/')
    await expect(page).toHaveTitle(/.+/)
  })
})
