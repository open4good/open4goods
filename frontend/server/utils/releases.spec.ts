import os from 'node:os'
import path from 'node:path'
import { promises as fs } from 'node:fs'
import type { ChildProcess } from 'node:child_process'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

const execFileMock = vi.fn<
  Parameters<(typeof import('node:child_process'))['execFile']>,
  ChildProcess
>()

vi.mock('node:child_process', async importOriginal => {
  const actual = await importOriginal<typeof import('node:child_process')>()

  return {
    ...actual,
    execFile: (
      ...args: Parameters<(typeof import('node:child_process'))['execFile']>
    ) => execFileMock(...args),
    default: {
      ...actual,
      execFile: (
        ...args: Parameters<(typeof import('node:child_process'))['execFile']>
      ) => execFileMock(...args),
    },
  }
})

describe('release utilities', () => {
  let tempRoot: string
  let releasesDirectory: string
  const gitDates = new Map<string, string>()
  let cwdSpy: ReturnType<typeof vi.spyOn>

  const writeReleaseNote = async (
    fileName: string,
    publishedAt: string,
    content: string
  ) => {
    const filePath = path.join(releasesDirectory, fileName)
    await fs.writeFile(filePath, content)
    gitDates.set(filePath, publishedAt)
  }

  beforeEach(async () => {
    vi.resetModules()
    execFileMock.mockReset()
    gitDates.clear()

    tempRoot = await fs.mkdtemp(path.join(os.tmpdir(), 'releases-'))
    releasesDirectory = path.join(tempRoot, 'public', 'reports', 'releases')
    await fs.mkdir(releasesDirectory, { recursive: true })

    await writeReleaseNote('older.md', '2024-11-10T00:00:00.000Z', '# Older')
    await writeReleaseNote('newer.md', '2024-12-15T00:00:00.000Z', '# Newer')

    execFileMock.mockImplementation(
      (_command, args, _options, callback): ChildProcess => {
        const filePath =
          Array.isArray(args) && args.length > 0
            ? ((args as string[]).at(-1) ?? '')
            : ''
        const stdout = gitDates.get(filePath) ?? ''
        if (typeof callback === 'function') {
          callback(null, stdout ? `${stdout}\n` : '', '')
        }
        return {} as ChildProcess
      }
    )

    cwdSpy = vi.spyOn(process, 'cwd').mockReturnValue(tempRoot)
  })

  afterEach(async () => {
    cwdSpy?.mockRestore()
    await fs.rm(tempRoot, { recursive: true, force: true })
    vi.restoreAllMocks()
  })

  it('returns release notes from the public reports directory in descending order', async () => {
    const { getLatestRelease, getReleaseNotes } =
      await import('~~/server/utils/releases')

    const releases = await getReleaseNotes()

    expect(releases.map(release => release.slug)).toEqual(['newer', 'older'])
    expect(releases[0]?.contentHtml).toContain('<h1>Newer</h1>')
    expect(execFileMock).toHaveBeenCalledTimes(2)

    const latestRelease = await getLatestRelease()
    expect(latestRelease?.slug).toBe('newer')
  })

  it('refreshes the cached release notes when warming the cache', async () => {
    const { getReleaseNotes, warmReleaseCache } =
      await import('~~/server/utils/releases')

    await getReleaseNotes()

    await writeReleaseNote(
      'fresh.md',
      '2025-01-01T00:00:00.000Z',
      'Fresh release'
    )

    const refreshedReleases = await warmReleaseCache()

    expect(refreshedReleases.map(release => release.slug)).toEqual([
      'fresh',
      'newer',
      'older',
    ])
    expect(execFileMock).toHaveBeenCalledTimes(5)
  })
})
