import { basename } from 'node:path'

import { useRuntimeConfig } from '#imports'

import type { DomainLanguage } from '~~/shared/utils/domain-language'

export interface LocalSitemapFileDescriptor {
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

export const getLocalSitemapFileDescriptorsForDomainLanguage = (
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

export const getPublicSitemapUrlsForDomainLanguage = (
  domainLanguage: DomainLanguage,
  origin: string,
  runtimeConfig?: SitemapLocalFilesRuntimeConfig
): string[] =>
  getLocalSitemapFileDescriptorsForDomainLanguage(
    domainLanguage,
    runtimeConfig
  ).map(descriptor => new URL(descriptor.publicPath, origin).toString())

export const getLocalSitemapFilePath = (
  domainLanguage: DomainLanguage,
  fileName: string,
  runtimeConfig?: SitemapLocalFilesRuntimeConfig
): string | null => {
  const descriptor = getLocalSitemapFileDescriptorsForDomainLanguage(
    domainLanguage,
    runtimeConfig
  ).find(entry => entry.fileName === fileName)

  return descriptor?.filePath ?? null
}
