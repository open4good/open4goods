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
  let outputReleasesDirectory: string
  let appReleasesDirectory: string
  let publicReleasesDirectory: string
  const gitDates = new Map<string, string>()
  let cwdSpy: ReturnType<typeof vi.spyOn>

  const writeReleaseNote = async (
    fileName: string,
    publishedAt: string,
    content: string,
    directory = outputReleasesDirectory
  ) => {
    const filePath = path.join(directory, fileName)
    await fs.writeFile(filePath, content)
    gitDates.set(filePath, publishedAt)
  }

  beforeEach(async () => {
    vi.resetModules()
    execFileMock.mockReset()
    gitDates.clear()

    tempRoot = await fs.mkdtemp(path.join(os.tmpdir(), 'releases-'))
    outputReleasesDirectory = path.join(
      tempRoot,
      '.output',
      'public',
      'reports',
      'releases'
    )
    appReleasesDirectory = path.join(
      tempRoot,
      'app',
      'public',
      'reports',
      'releases'
    )
    publicReleasesDirectory = path.join(
      tempRoot,
      'public',
      'reports',
      'releases'
    )
    await Promise.all([
      fs.mkdir(outputReleasesDirectory, { recursive: true }),
      fs.mkdir(appReleasesDirectory, { recursive: true }),
    ])

    await writeReleaseNote('older.md', '2024-11-10T00:00:00.000Z', '# Older')
    await writeReleaseNote('newer.md', '2024-12-15T00:00:00.000Z', '# Newer')
    await writeReleaseNote(
      'app.md',
      '2025-01-01T00:00:00.000Z',
      '# App',
      appReleasesDirectory
    )

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

  it('falls back to app public releases when the build output is missing', async () => {
    await fs.rm(path.join(tempRoot, '.output'), {
      recursive: true,
      force: true,
    })
    gitDates.clear()
    await fs.rm(path.join(appReleasesDirectory, 'app.md'), { force: true })

    await writeReleaseNote(
      'app-older.md',
      '2024-10-10T00:00:00.000Z',
      '# App older',
      appReleasesDirectory
    )
    await writeReleaseNote(
      'app-newer.md',
      '2024-12-20T00:00:00.000Z',
      '# App newer',
      appReleasesDirectory
    )

    const { getReleaseNotes } = await import('~~/server/utils/releases')

    const releases = await getReleaseNotes()

    expect(releases.map(release => release.slug)).toEqual([
      'app-newer',
      'app-older',
    ])
    expect(execFileMock).toHaveBeenCalledTimes(2)
  })

  it('falls back to public releases when app public is missing', async () => {
    await fs.rm(path.join(tempRoot, '.output'), {
      recursive: true,
      force: true,
    })
    await fs.rm(path.join(tempRoot, 'app'), { recursive: true, force: true })
    await fs.mkdir(publicReleasesDirectory, { recursive: true })
    gitDates.clear()

    await writeReleaseNote(
      'public-newer.md',
      '2024-12-31T00:00:00.000Z',
      '# Public newer',
      publicReleasesDirectory
    )

    const { getLatestRelease } = await import('~~/server/utils/releases')

    const latestRelease = await getLatestRelease()

    expect(latestRelease?.slug).toBe('public-newer')
    expect(execFileMock).toHaveBeenCalledTimes(1)
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
