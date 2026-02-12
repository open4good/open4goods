import { basename } from 'node:path'

// ---- Mocks ----
let mockRuntimeConfig: any = {}
const useRuntimeConfig = () => mockRuntimeConfig

// ---- Code from sitemap-local-files.ts ----
type DomainLanguage = 'en' | 'fr'

interface LocalSitemapFileDescriptor {
  filePath: string
  fileName: string
  publicPath: string
}

type SitemapLocalFilesRuntimeConfig = {
  sitemapLocalFiles?: Partial<Record<DomainLanguage, unknown>>
}

const normalizeSitemapPaths = (paths: unknown): string[] => {
  if (!Array.isArray(paths)) {
    return []
  }

  return Array.from(
    new Set(
      paths
        .map(entry => (typeof entry === 'string' ? entry.trim() : ''))
        .filter((entry): entry is string => Boolean(entry))
    )
  )
}

const getRuntimeConfig = (
  runtimeConfig?: SitemapLocalFilesRuntimeConfig
): SitemapLocalFilesRuntimeConfig => runtimeConfig ?? useRuntimeConfig()

const buildPublicPath = (
  domainLanguage: DomainLanguage,
  filePath: string
): LocalSitemapFileDescriptor | null => {
  const fileName = basename(filePath)

  if (!fileName) {
    return null
  }

  return {
    filePath,
    fileName,
    publicPath: `/sitemap/${domainLanguage}/${fileName}`,
  }
}

const getLocalSitemapFileDescriptorsForDomainLanguage = (
  domainLanguage: DomainLanguage,
  runtimeConfig?: SitemapLocalFilesRuntimeConfig
): LocalSitemapFileDescriptor[] => {
  const resolvedRuntimeConfig = getRuntimeConfig(runtimeConfig)
  const configuredPaths =
    resolvedRuntimeConfig.sitemapLocalFiles?.[domainLanguage]
  const normalizedPaths = normalizeSitemapPaths(configuredPaths)

  const seenFileNames = new Set<string>()

  return normalizedPaths
    .map(filePath => buildPublicPath(domainLanguage, filePath))
    .filter((descriptor): descriptor is LocalSitemapFileDescriptor => {
      if (!descriptor) {
        return false
      }

      if (seenFileNames.has(descriptor.fileName)) {
        return false
      }

      seenFileNames.add(descriptor.fileName)
      return true
    })
}

const getPublicSitemapUrlsForDomainLanguage = (
  domainLanguage: DomainLanguage,
  origin: string,
  runtimeConfig?: SitemapLocalFilesRuntimeConfig
): string[] =>
  getLocalSitemapFileDescriptorsForDomainLanguage(
    domainLanguage,
    runtimeConfig
  ).map(descriptor => new URL(descriptor.publicPath, origin).toString())

// ---- Code from sitemap-index.ts (logic part) ----

function testSitemapIndexLogic(domainLanguage: DomainLanguage, origin: string) {
  try {
    const additionalSitemaps = getPublicSitemapUrlsForDomainLanguage(
      domainLanguage,
      origin
    )
    console.log(
      `Success for ${domainLanguage} @ ${origin}:`,
      additionalSitemaps
    )
  } catch (e) {
    console.error(`CRASH for ${domainLanguage} @ ${origin}:`, e)
  }
}

// ---- Reproduction Scenarios ----

console.log('--- Scenario 1: Normal Config ---')
mockRuntimeConfig = {
  sitemapLocalFiles: {
    fr: ['/path/to/sitemap1.xml', '/path/to/sitemap2.xml'],
    en: ['/path/to/sitemap3.xml'],
  },
}
testSitemapIndexLogic('fr', 'https://nudger.fr')

console.log('\n--- Scenario 2: Missing Domain Config ---')
mockRuntimeConfig = {
  sitemapLocalFiles: {
    en: ['/path/to/sitemap3.xml'],
  },
}
testSitemapIndexLogic('fr', 'https://nudger.fr')

console.log('\n--- Scenario 3: Null Config ---')
mockRuntimeConfig = {
  sitemapLocalFiles: null,
}
testSitemapIndexLogic('fr', 'https://nudger.fr')

console.log('\n--- Scenario 4: Bad Input Types ---')
mockRuntimeConfig = {
  sitemapLocalFiles: {
    fr: 'string-instead-of-array',
    en: [null, 123, {}],
  },
}
testSitemapIndexLogic('fr', 'https://nudger.fr')
testSitemapIndexLogic('en', 'https://nudger.fr')

console.log('\n--- Scenario 5: Bad Origin ---')
mockRuntimeConfig = {
  sitemapLocalFiles: {
    fr: ['/path/to/file.xml'],
  },
}
// Origin usually implies protocol
testSitemapIndexLogic('fr', 'null')
testSitemapIndexLogic('fr', 'undefined')
testSitemapIndexLogic('fr', '') // Empty origin -> new URL('/path', '') throws?

console.log('\n--- Scenario 6: Malformed URL logic ---')
// If origin doesn't have protocol?
testSitemapIndexLogic('fr', 'nudger.fr')
