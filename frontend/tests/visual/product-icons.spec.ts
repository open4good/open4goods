import { expect, test } from '@playwright/test'

const productUrl =
  '/televiseurs/8718863036846-television-philips-24phs680812-2023'

test('product attributes view toggle uses inline SVG icons', async ({
  page,
}) => {
  await page.setViewportSize({ width: 1920, height: 959 })
  await page.goto(productUrl, { waitUntil: 'domcontentloaded' })

  const toggle = page.locator(
    'section#caracteristiques .product-attributes__view-toggle'
  )
  await expect(toggle).toBeVisible()
  await expect(toggle.locator('.v-icon svg path')).toHaveCount(2)
  await expect(page.locator('.v-icon svg path[d^="mdi-"]')).toHaveCount(0)

  const mdiStylesheetLinks = await page.locator('link').evaluateAll(links =>
    links
      .map(link => link.getAttribute('href') ?? '')
      .filter(href => /@mdi\/font|materialdesignicons/i.test(href))
  )

  expect(mdiStylesheetLinks).toEqual([])
})
