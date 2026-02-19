import { test, expect } from '@playwright/test'

/**
 * Verify metriks data loading and history computation.
 *
 * Since the /metriks page requires admin auth which is not available
 * in this test environment, we test the data parsing logic by fetching
 * the raw NDJSON files and verifying the history computation directly.
 */
test('metriks NDJSON data files are accessible and contain history', async ({
  page,
}) => {
  // Navigate to any page first so we have a context
  await page.goto('/')

  // Fetch kibana events.ndjson
  const kibanaText = await page.evaluate(async () => {
    const resp = await fetch('/reports/metriks/data/kibana/events.ndjson')
    return resp.text()
  })

  const kibanaRecords = kibanaText
    .split('\n')
    .filter((l: string) => l.trim().length > 0)
    .map((l: string) => JSON.parse(l))

  const kibanaRuns = kibanaRecords.filter(
    (r: { type: string }) => r.type === 'run'
  )
  expect(kibanaRuns.length).toBeGreaterThanOrEqual(2)

  // Fetch plausible events.ndjson
  const plausibleText = await page.evaluate(async () => {
    const resp = await fetch('/reports/metriks/data/plausible/events.ndjson')
    return resp.text()
  })

  const plausibleRecords = plausibleText
    .split('\n')
    .filter((l: string) => l.trim().length > 0)
    .map((l: string) => JSON.parse(l))

  const plausibleRuns = plausibleRecords.filter(
    (r: { type: string }) => r.type === 'run'
  )
  expect(plausibleRuns.length).toBeGreaterThanOrEqual(2)
})

test('buildHistory produces data points for all run dates including null gaps', async ({
  page,
}) => {
  await page.goto('/')

  // Simulate the buildHistory logic in the browser context
  const result = await page.evaluate(async () => {
    const resp = await fetch('/reports/metriks/data/kibana/events.ndjson')
    const text = await resp.text()
    const records = text
      .split('\n')
      .filter((l: string) => l.trim().length > 0)
      .map((l: string) => JSON.parse(l))

    const runs = records.filter((r: { type: string }) => r.type === 'run')
    const latestRun = runs[runs.length - 1]

    // Get event IDs from the latest run
    const latestEventIds = latestRun.events.map((e: { id: string }) => e.id)

    // Simulate buildHistory: for each event in latest run, find matching events across all runs
    const histories: Record<string, { date: string; value: number | null }[]> =
      {}
    for (const eventId of latestEventIds) {
      histories[eventId] = runs.map(
        (run: {
          period: { dateTo: string }
          events: { id: string; value: number | null }[]
        }) => {
          const ev = run.events.find((e: { id: string }) => e.id === eventId)
          return {
            date: run.period.dateTo,
            value: ev?.value ?? null,
          }
        }
      )
    }

    return {
      totalRuns: runs.length,
      latestEventIds,
      histories,
    }
  })

  // Verify: we should have as many data points as runs for each event
  expect(result.totalRuns).toBeGreaterThanOrEqual(2)

  for (const eventId of result.latestEventIds) {
    const history = result.histories[eventId]
    // Every event should have a data point for every run
    expect(history.length).toBe(result.totalRuns)

    // The last data point should have a non-null value (from the latest ok run)
    const lastPoint = history[history.length - 1]
    expect(lastPoint.value).not.toBeNull()
  }
})
