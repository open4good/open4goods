import { useRuntimeConfig } from '#imports'

import type { DomainLanguage } from '~~/shared/utils/domain-language'

const normalizeSitemapPaths = (paths: unknown): string[] => {
  if (!Array.isArray(paths)) {
    return []
  }

  return Array.from(
    new Set(
      paths
        .map((entry) => (typeof entry === 'string' ? entry.trim() : ''))
        .filter((entry): entry is string => Boolean(entry)),
    ),
  )
}

export const getLocalSitemapFilesForDomainLanguage = (domainLanguage: DomainLanguage): string[] => {
  const runtimeConfig = useRuntimeConfig()
  const configuredPaths = runtimeConfig.sitemapLocalFiles?.[domainLanguage]

  return normalizeSitemapPaths(configuredPaths)
}
