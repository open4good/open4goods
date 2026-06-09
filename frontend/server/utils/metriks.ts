import { promises as fs } from 'node:fs'
import path from 'node:path'
import DOMPurify from 'isomorphic-dompurify'
import MarkdownIt from 'markdown-it'

/**
 * Server-side access to the weekly Metriks reports.
 *
 * Data is produced/versioned in open4goods-config and deployed at runtime to the directory
 * pointed to by `runtimeConfig.metriksDataDir` (env `METRIKS_DATA_DIR`, typically
 * `/opt/open4goods/metriks-data`). During local dev it falls back to the bundled public dir.
 *
 * Layout of the resolved "metriks root":
 *   <root>/data/<provider>/latest.json
 *   <root>/data/<provider>/events.ndjson
 *   <root>/weekly/latest.md
 */
const markdown = new MarkdownIt({ html: true, linkify: true, breaks: true })

const PROJECT_ROOT = path.resolve(process.cwd())

/** Resolve the configured metriks dir, tolerating contexts where useRuntimeConfig is absent (tests). */
const configuredDir = (): string => {
  try {
    const runtimeConfig = (
      globalThis as { useRuntimeConfig?: () => { metriksDataDir?: string } }
    ).useRuntimeConfig
    const value = runtimeConfig?.().metriksDataDir
    if (value) return value
  } catch {
    // useRuntimeConfig unavailable (e.g. unit tests) — fall back to the env var.
  }
  return process.env.METRIKS_DATA_DIR || ''
}

const candidateRoots = (): string[] => {
  const configured = configuredDir()
  const roots: string[] = []
  if (configured) roots.push(configured)
  roots.push(
    path.join(PROJECT_ROOT, '.output', 'public', 'reports', 'metriks'),
    path.join(PROJECT_ROOT, 'app', 'public', 'reports', 'metriks'),
    path.join(PROJECT_ROOT, 'public', 'reports', 'metriks')
  )
  return roots
}

let cachedRoot: string | null = null

const resolveRoot = async (): Promise<string> => {
  if (cachedRoot) return cachedRoot
  for (const candidate of candidateRoots()) {
    try {
      // A valid root contains a `data` directory.
      const stats = await fs.stat(path.join(candidate, 'data'))
      if (stats.isDirectory()) {
        cachedRoot = candidate
        return cachedRoot
      }
    } catch (error: unknown) {
      if ((error as NodeJS.ErrnoException).code !== 'ENOENT') throw error
    }
  }
  cachedRoot = candidateRoots().at(-1) ?? PROJECT_ROOT
  return cachedRoot
}

/** Names of the provider directories that contain a latest.json snapshot. */
export const listMetrikProviders = async (): Promise<string[]> => {
  try {
    const dataDir = path.join(await resolveRoot(), 'data')
    const entries = await fs.readdir(dataDir, { withFileTypes: true })
    const providers: string[] = []
    for (const entry of entries) {
      if (!entry.isDirectory()) continue
      try {
        await fs.access(path.join(dataDir, entry.name, 'latest.json'))
        providers.push(entry.name)
      } catch {
        // skip provider dirs without a snapshot
      }
    }
    return providers.sort()
  } catch (error: unknown) {
    console.error('[metriks] failed to list providers', error)
    return []
  }
}

const safeProvider = (provider: string): string => {
  // Provider names are simple directory slugs; reject anything with path separators.
  if (!/^[a-z0-9][a-z0-9._-]*$/i.test(provider)) {
    throw createError({
      statusCode: 400,
      statusMessage: 'Invalid provider name',
    })
  }
  return provider
}

/** Raw `latest.json` content for a provider (parsed). */
export const readMetrikLatest = async (
  provider: string
): Promise<unknown | null> => {
  try {
    const file = path.join(
      await resolveRoot(),
      'data',
      safeProvider(provider),
      'latest.json'
    )
    return JSON.parse(await fs.readFile(file, 'utf-8'))
  } catch (error: unknown) {
    if ((error as NodeJS.ErrnoException).code === 'ENOENT') return null
    throw error
  }
}

/** Raw `events.ndjson` text for a provider. */
export const readMetrikHistory = async (provider: string): Promise<string> => {
  try {
    const file = path.join(
      await resolveRoot(),
      'data',
      safeProvider(provider),
      'events.ndjson'
    )
    return await fs.readFile(file, 'utf-8')
  } catch (error: unknown) {
    if ((error as NodeJS.ErrnoException).code === 'ENOENT') return ''
    throw error
  }
}

export interface WeeklyDigest {
  name: string
  contentHtml: string
}

/** Render the latest weekly markdown digest to sanitized HTML. */
export const getLatestWeeklyDigest = async (): Promise<WeeklyDigest | null> => {
  try {
    const file = path.join(await resolveRoot(), 'weekly', 'latest.md')
    const raw = await fs.readFile(file, 'utf-8')
    return {
      name: 'latest',
      contentHtml: DOMPurify.sanitize(markdown.render(raw)),
    }
  } catch (error: unknown) {
    if ((error as NodeJS.ErrnoException).code === 'ENOENT') return null
    console.error('[metriks] failed to read weekly digest', error)
    return null
  }
}
