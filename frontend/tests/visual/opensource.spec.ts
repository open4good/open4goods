import { test, expect } from '@playwright/test';

test.describe('Opensource Page', () => {
  test('should match snapshot', async ({ page }) => {
    await expect(page).toHaveScreenshot('opensource-page.png', { fullPage: true });
  });

  test.beforeEach(async ({ page }) => {
    await page.goto('/opensource');
  });

  test('should load without console errors', async ({ page }) => {
    const consoleLogs: string[] = [];
    page.on('console', msg => {
      if (msg.type() === 'error' || msg.type() === 'warning') {
        consoleLogs.push(msg.text());
      }
    });

    await expect(page).toHaveTitle(/Open Source/);
    
    // Wait a bit to catch hydration errors
    await page.waitForTimeout(1000);

    // Filter out known harmless warnings if any (currently strict)
    const validErrors = consoleLogs.filter(log => 
      !log.includes('[vite]') && // ignore vite connection warnings
      !log.includes('Third-party cookie will be blocked') // ignore some chrome warnings
    );
    
    expect(validErrors).toEqual([]);
  });

  test('should display primary sections', async ({ page }) => {
    await expect(page.locator('h1')).toBeVisible(); // Hero title
    await expect(page.getByRole('heading', { name: 'Un écosystème ouvert et fiable' })).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Rejoignez le collectif en quelques étapes' })).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Tout pour bien démarrer' })).toBeVisible();
  });

  test('should have accessible links in hero', async ({ page }) => {
    const heroSection = page.locator('.opensource-hero');
    const cta = heroSection.getByRole('link', { name: 'Accéder au code' });
    await expect(cta).toBeVisible();
    await expect(cta).toHaveAttribute('href', 'https://github.com/open4good/open4goods');
  });

  test('should have correct metadata', async ({ page }) => {
    const description = page.locator('meta[name="description"]');
    await expect(description).toHaveAttribute('content', /.+/);
    
    const canonical = page.locator('link[rel="canonical"]');
    await expect(canonical).toHaveAttribute('href', /opensource/);
  });
});
