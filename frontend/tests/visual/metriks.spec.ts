import { test, expect } from '@playwright/test'

test('metriks page loads without 500 error', async ({ page }) => {
  await page.goto('/metriks')

  // Check that we don't have a 500 error
  await expect(page.getByText('500', { exact: true })).not.toBeVisible()
  await expect(page.getByText('Server Error')).not.toBeVisible()
  await expect(page.getByText('ReferenceError')).not.toBeVisible()

  // If not logged in/admin, we expect the access denied state
  // "Login to access metrics" or similar (t('metriks.loginCta'))
  // or just check that the wrapper exists
  // We can check for "Métriques" or "Metriques" or whatever the translation for pageTitle is,
  // but access denied might show different text.
  // The code shows:
  // :headline="t('metriks.accessDenied')"
  // :title="t('metriks.pageSubtitle')"
  // v-btn to="/auth/login" with t('metriks.loginCta')

  // Let's check for the button to login if we assume we are not logged in
  // or check that the main container is present
})
