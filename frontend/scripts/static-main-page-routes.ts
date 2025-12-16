import { readdirSync } from 'node:fs'
import { join, relative, sep } from 'node:path'

const PAGE_EXTENSION_PATTERN = /\.(vue|md)$/iu
const DYNAMIC_SEGMENT_PATTERN = /(^|\/)\[[^/]+?\](?=\/|$)/u
const EXCLUDED_ROUTE_SEGMENTS = new Set(['auth', 'contrib'])
const EXCLUDED_ROUTE_PATHS = new Set([
  'index-v1',
  'xwiki-fullpage',
  'blog/test-images',
])

const toPosixPath = (value: string): string => value.split(sep).join('/')

export const normalizeRouteNameFromRelativePath = (
  relativePath: string
): string | null => {
  if (!PAGE_EXTENSION_PATTERN.test(relativePath)) {
    return null
  }

  const withoutExtension = relativePath.replace(PAGE_EXTENSION_PATTERN, '')

  if (!withoutExtension || DYNAMIC_SEGMENT_PATTERN.test(withoutExtension)) {
    return null
  }

  if (EXCLUDED_ROUTE_PATHS.has(withoutExtension)) {
    return null
  }

  const segments = withoutExtension.split('/')

  if (segments.some(segment => EXCLUDED_ROUTE_SEGMENTS.has(segment))) {
    return null
  }

  if (withoutExtension === 'index') {
    return 'index'
  }

  if (withoutExtension.endsWith('/index')) {
    const trimmed = withoutExtension.slice(0, -'/index'.length)

    return trimmed || 'index'
  }

  return withoutExtension
}

export interface CollectStaticPageRouteNamesOptions {
  rootDir?: string
}

export const collectStaticPageRouteNames = (
  directory: string,
  options: CollectStaticPageRouteNamesOptions = {}
): string[] => {
  const rootDir = options.rootDir ?? directory
  const entries = readdirSync(directory, { withFileTypes: true })
  const routeNames: string[] = []

  for (const entry of entries) {
    const entryPath = join(directory, entry.name)

    if (entry.isDirectory()) {
      const relativeDirectory = toPosixPath(relative(rootDir, entryPath))

      if (
        relativeDirectory &&
        DYNAMIC_SEGMENT_PATTERN.test(relativeDirectory)
      ) {
        continue
      }

      if (relativeDirectory) {
        const segments = relativeDirectory.split('/')

        if (segments.some(segment => EXCLUDED_ROUTE_SEGMENTS.has(segment))) {
          continue
        }
      }

      routeNames.push(...collectStaticPageRouteNames(entryPath, { rootDir }))
      continue
    }

    if (!entry.isFile()) {
      continue
    }

    const relativePath = toPosixPath(relative(rootDir, entryPath))
    const routeName = normalizeRouteNameFromRelativePath(relativePath)

    if (!routeName) {
      continue
    }

    routeNames.push(routeName)
  }

  return routeNames
}
