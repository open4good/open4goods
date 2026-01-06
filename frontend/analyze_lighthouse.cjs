const fs = require('fs')
const files = ['home.json', 'product.json', 'category.json']

files.forEach(file => {
  try {
    const path = `./lighthouse_reports/${file}`
    if (!fs.existsSync(path)) {
      console.log(`${file}: Not found`)
      return
    }
    const data = JSON.parse(fs.readFileSync(path, 'utf8'))
    console.log(`\n=== ${file} ===`)

    // Scores
    const categories = data.categories
    console.log('Scores:')
    if (categories.performance)
      console.log(`  Performance: ${categories.performance.score * 100}`)
    if (categories.accessibility)
      console.log(`  Accessibility: ${categories.accessibility.score * 100}`)
    if (categories['best-practices'])
      console.log(
        `  Best Practices: ${categories['best-practices'].score * 100}`
      )
    if (categories.seo) console.log(`  SEO: ${categories.seo.score * 100}`)

    // Failed Audits (Opportunities)
    const audits = data.audits
    console.log('Top Opportunities:')
    const opportunities = Object.values(audits)
      .filter(
        audit =>
          audit.details &&
          audit.details.type === 'opportunity' &&
          audit.score < 0.9
      )
      .sort(
        (a, b) =>
          (b.details.overallSavingsMs || 0) - (a.details.overallSavingsMs || 0)
      )
      .slice(0, 5)

    opportunities.forEach(op => {
      console.log(
        `  - ${op.title}: ${Math.round(op.details.overallSavingsMs)}ms`
      )
      if (op.details.items) {
        op.details.items.slice(0, 3).forEach(item => {
          console.log(
            `    * ${item.url} (${Math.round(item.wastedBytes / 1024)} KB)`
          )
        })
      }
    })

    // Key Metrics
    console.log('Metrics:')
    const metrics = [
      'first-contentful-paint',
      'largest-contentful-paint',
      'total-blocking-time',
      'cumulative-layout-shift',
      'speed-index',
      'interactive',
    ]
    metrics.forEach(id => {
      if (audits[id]) {
        console.log(`  - ${audits[id].title}: ${audits[id].displayValue}`)
      }
    })
  } catch (e) {
    console.log(`Error parsing ${file}: ${e.message}`)
  }
})
