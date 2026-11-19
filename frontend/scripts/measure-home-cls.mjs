// Usage: node scripts/measure-home-cls.mjs [baseUrl]  (build de prod servi sur baseUrl)
import { chromium } from '@playwright/test'
const base = process.argv[2] || 'http://127.0.0.1:4100'
const UA_D = 'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0 Safari/537.36'
const UA_M = 'Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1'
const cases = [[840,900,UA_D],[960,800,UA_D],[1024,768,UA_D],[1280,800,UA_D],[390,844,UA_M]]
const browser = await chromium.launch({ executablePath: process.env.CHROMIUM_PATH })
for (const [w,h,ua] of cases) {
  const ctx = await browser.newContext({ viewport:{width:w,height:h}, userAgent:ua })
  const page = await ctx.newPage()
  await page.addInitScript(() => {
    window.__cls = 0; window.__src = []
    new PerformanceObserver(l => { for (const e of l.getEntries()) if (!e.hadRecentInput) { window.__cls += e.value; window.__src.push((e.sources||[]).map(s=>s.node&&s.node.className).join('|')) } }).observe({type:'layout-shift',buffered:true})
  })
  await page.goto(base, { waitUntil:'networkidle' })
  await page.waitForTimeout(2500)
  const H = await page.evaluate(() => document.documentElement.scrollHeight)
  for (let y=0; y<H; y+=h*0.8) { await page.evaluate(v=>window.scrollTo(0,v), y); await page.waitForTimeout(150) }
  await page.waitForTimeout(1000)
  const r = await page.evaluate(() => ({cls: window.__cls, wiz: document.querySelector('.nudge-wizard')?.getBoundingClientRect().height}))
  console.log(`${w}x${h} cls=${r.cls.toFixed(4)} wizardH=${r.wiz}`)
  await ctx.close()
}
await browser.close()
