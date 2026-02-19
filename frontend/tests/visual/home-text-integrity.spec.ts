import { expect, test, type TestInfo } from '@playwright/test'

type BreakpointConfig = {
  name: string
  viewport: { width: number; height: number }
  zoomLevels: number[]
}

const breakpoints: BreakpointConfig[] = [
  {
    name: 'mobile',
    viewport: { width: 390, height: 844 },
    zoomLevels: [1, 1.25],
  },
  {
    name: 'tablet',
    viewport: { width: 768, height: 1024 },
    zoomLevels: [1, 1.25],
  },
  {
    name: 'desktop',
    viewport: { width: 1440, height: 1080 },
    zoomLevels: [1, 1.1, 1.25],
  },
]

const criticalSectionSelectors = [
  '.home-hero',
  '#home-problems',
  '#home-solution',
]

test.describe('home critical copy remains readable', () => {
  for (const config of breakpoints) {
    for (const zoomLevel of config.zoomLevels) {
      test(`no clipped text on ${config.name} at ${Math.round(zoomLevel * 100)}% zoom`, async ({
        page,
      }, testInfo: TestInfo) => {
        await page.setViewportSize(config.viewport)
        await page.goto('/fr', { waitUntil: 'networkidle' })
        await page.evaluate(zoom => {
          document.body.style.zoom = String(zoom)
        }, zoomLevel)

        await page.waitForTimeout(400)

        const clippingReport = await page.evaluate(sectionSelectors => {
          const textSelectors = [
            'h1',
            'h2',
            'h3',
            'p',
            'span',
            'a',
            'button',
            'li',
          ].join(',')

          const issues: Array<{
            section: string
            text: string
            className: string
            overflowX: number
            overflowY: number
          }> = []

          for (const selector of sectionSelectors) {
            const section = document.querySelector(selector)

            if (!section) {
              continue
            }

            const elements = section.querySelectorAll<HTMLElement>(textSelectors)

            for (const element of elements) {
              const computedStyle = window.getComputedStyle(element)
              const overflowX = element.scrollWidth - element.clientWidth
              const overflowY = element.scrollHeight - element.clientHeight
              const truncatesWithClamp =
                computedStyle.webkitLineClamp &&
                computedStyle.webkitLineClamp !== 'none' &&
                computedStyle.webkitLineClamp !== 'unset' &&
                computedStyle.webkitLineClamp !== 'initial'

              const canClipText =
                computedStyle.overflow === 'hidden' ||
                computedStyle.overflowX === 'hidden' ||
                computedStyle.overflowY === 'hidden' ||
                computedStyle.textOverflow === 'ellipsis' ||
                truncatesWithClamp

              if (!canClipText) {
                continue
              }

              if (overflowX > 1 || overflowY > 1) {
                issues.push({
                  section: selector,
                  text: (element.textContent ?? '').trim().slice(0, 120),
                  className: element.className,
                  overflowX,
                  overflowY,
                })
              }
            }
          }

          return issues
        }, criticalSectionSelectors)

        expect(clippingReport).toEqual([])

        await page.screenshot({
          path: testInfo.outputPath(
            `home-critical-${config.name}-${Math.round(zoomLevel * 100)}.png`
          ),
          fullPage: true,
        })
      })
    }
  }
})
