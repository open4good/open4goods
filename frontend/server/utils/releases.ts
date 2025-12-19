import { execFile } from 'node:child_process'
import { promises as fs } from 'node:fs'
import path from 'node:path'
import { promisify } from 'node:util'
import createDOMPurify from 'isomorphic-dompurify'
import { JSDOM, type DOMWindow } from 'jsdom'
import MarkdownIt from 'markdown-it'
import type { ReleaseNote } from '~~/types/releases'

const execFileAsync = promisify(execFile)
const markdown = new MarkdownIt({
  html: true,
  linkify: true,
  breaks: true,
})
const DOMPurify = createDOMPurify(new JSDOM('').window as unknown as DOMWindow)

const PROJECT_ROOT = path.resolve(process.cwd())
const RELEASES_DIRECTORY = path.join(PROJECT_ROOT, 'Release')

let cachedReleaseNotes: ReleaseNote[] | null = null

const getCreationDateFromGit = async (
  filePath: string
): Promise<string | null> => {
  try {
    const { stdout } = await execFileAsync(
      'git',
      ['log', '--diff-filter=A', '--follow', '--format=%cI', '-1', '--', filePath],
      { cwd: PROJECT_ROOT }
    )

    const isoDate = stdout.trim().split('\n').filter(Boolean).at(-1)

    return isoDate ?? null
  }
  catch (error: unknown) {
    console.warn('Unable to compute release creation date from git', error)
    return null
  }
}

const getReleasePublishedAt = async (filePath: string): Promise<string> => {
  const gitDate = await getCreationDateFromGit(filePath)

  if (gitDate) {
    return gitDate
  }

  const stats = await fs.stat(filePath)
  const fallbackDate = stats.birthtimeMs > 0 ? stats.birthtime : stats.mtime

  return fallbackDate.toISOString()
}

const buildReleaseNote = async (fileName: string): Promise<ReleaseNote | null> => {
  if (!fileName.toLowerCase().endsWith('.md')) {
    return null
  }

  const fullPath = path.join(RELEASES_DIRECTORY, fileName)
  const [rawContent, publishedAt] = await Promise.all([
    fs.readFile(fullPath, 'utf-8'),
    getReleasePublishedAt(fullPath),
  ])

  const name = path.basename(fileName, path.extname(fileName))

  return {
    name,
    slug: name,
    contentHtml: DOMPurify.sanitize(markdown.render(rawContent)),
    publishedAt,
  }
}

export const getReleaseNotes = async (): Promise<ReleaseNote[]> => {
  if (cachedReleaseNotes) {
    return cachedReleaseNotes
  }

  try {
    const files = await fs.readdir(RELEASES_DIRECTORY)
    const releases = (await Promise.all(files.map(buildReleaseNote)))
      .filter(Boolean) as ReleaseNote[]

    cachedReleaseNotes = releases.sort((left, right) =>
      new Date(right.publishedAt).getTime() - new Date(left.publishedAt).getTime()
    )

    return cachedReleaseNotes
  }
  catch (error: unknown) {
    console.error('Failed to read release notes', error)
    cachedReleaseNotes = []
    return cachedReleaseNotes
  }
}

export const warmReleaseCache = async (): Promise<ReleaseNote[]> => {
  cachedReleaseNotes = null
  return getReleaseNotes()
}

export const getLatestRelease = async (): Promise<ReleaseNote | null> => {
  const releases = await getReleaseNotes()

  return releases[0] ?? null
}
