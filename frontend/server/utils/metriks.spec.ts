import os from 'node:os'
import path from 'node:path'
import { promises as fs } from 'node:fs'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

// createError is auto-imported in the Nitro runtime; stub it for unit tests.
;(globalThis as { createError?: (input: unknown) => Error }).createError = (
  input: unknown
) => new Error(JSON.stringify(input))

describe('metriks server utilities', () => {
  let tempRoot: string
  let dataDir: string
  let weeklyDir: string
  let cwdSpy: ReturnType<typeof vi.spyOn>

  const run = (provider: string, dateTo: string, value: number) => ({
    type: 'run',
    schemaVersion: '2.0',
    provider,
    period: { dateFrom: dateTo, dateTo },
    events: [
      { id: `${provider}.metric`, name: 'M', value, unit: 'count', status: 'ok' },
    ],
  })

  const writeProvider = async (
    provider: string,
    runs: Array<ReturnType<typeof run>>
  ) => {
    const dir = path.join(dataDir, provider)
    await fs.mkdir(dir, { recursive: true })
    const meta = {
      type: 'meta',
      schemaVersion: '2.0',
      provider,
      providerDisplayName: provider,
    }
    const ndjson = [meta, ...runs].map(r => JSON.stringify(r)).join('\n') + '\n'
    await fs.writeFile(path.join(dir, 'events.ndjson'), ndjson)
    await fs.writeFile(
      path.join(dir, 'latest.json'),
      JSON.stringify({ schemaVersion: '2.0', type: 'latest', meta, run: runs.at(-1) })
    )
  }

  beforeEach(async () => {
    vi.resetModules()
    delete process.env.METRIKS_DATA_DIR
    tempRoot = await fs.mkdtemp(path.join(os.tmpdir(), 'metriks-'))
    dataDir = path.join(tempRoot, '.output', 'public', 'reports', 'metriks', 'data')
    weeklyDir = path.join(tempRoot, '.output', 'public', 'reports', 'metriks', 'weekly')
    await fs.mkdir(dataDir, { recursive: true })
    await fs.mkdir(weeklyDir, { recursive: true })
    cwdSpy = vi.spyOn(process, 'cwd').mockReturnValue(tempRoot)
  })

  afterEach(async () => {
    cwdSpy?.mockRestore()
    await fs.rm(tempRoot, { recursive: true, force: true })
  })

  it('lists only providers that have a latest.json snapshot', async () => {
    await writeProvider('plausible', [run('plausible', '2026-06-05', 277)])
    // A directory without latest.json must be ignored.
    await fs.mkdir(path.join(dataDir, 'empty'), { recursive: true })

    const { listMetrikProviders } = await import('~~/server/utils/metriks')
    expect(await listMetrikProviders()).toEqual(['plausible'])
  })

  it('reads latest.json and history for a provider', async () => {
    await writeProvider('open4goods-api', [
      run('open4goods-api', '2026-05-29', 100),
      run('open4goods-api', '2026-06-05', 120),
    ])

    const { readMetrikLatest, readMetrikHistory } = await import(
      '~~/server/utils/metriks'
    )
    const latest = (await readMetrikLatest('open4goods-api')) as {
      run: { events: { value: number }[] }
    }
    expect(latest.run.events[0]?.value).toBe(120)

    const history = await readMetrikHistory('open4goods-api')
    const runs = history
      .split('\n')
      .filter(Boolean)
      .map(l => JSON.parse(l))
      .filter(r => r.type === 'run')
    expect(runs).toHaveLength(2)
  })

  it('rejects path-traversal provider names', async () => {
    const { readMetrikLatest } = await import('~~/server/utils/metriks')
    await expect(readMetrikLatest('../../etc')).rejects.toThrow()
  })

  it('renders the weekly digest markdown to sanitized html', async () => {
    await writeProvider('plausible', [run('plausible', '2026-06-05', 1)])
    await fs.writeFile(
      path.join(weeklyDir, 'latest.md'),
      '# Rapport\n\n| Métrique | Valeur |\n| --- | --- |\n| Visiteurs | 277 |\n'
    )

    const { getLatestWeeklyDigest } = await import('~~/server/utils/metriks')
    const digest = await getLatestWeeklyDigest()
    expect(digest?.contentHtml).toContain('<h1>Rapport</h1>')
    expect(digest?.contentHtml).toContain('<table>')
  })

  it('returns null when no weekly digest exists', async () => {
    await writeProvider('plausible', [run('plausible', '2026-06-05', 1)])
    const { getLatestWeeklyDigest } = await import('~~/server/utils/metriks')
    expect(await getLatestWeeklyDigest()).toBeNull()
  })
})
