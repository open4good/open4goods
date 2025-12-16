import { createReadStream } from 'node:fs'
import { access } from 'node:fs/promises'
import { basename } from 'node:path'

import { createError, defineEventHandler, sendStream, setHeader } from 'h3'

import type { DomainLanguage } from '~~/shared/utils/domain-language'
import { getLocalSitemapFilePath } from '~~/server/utils/sitemap-local-files'

const isDomainLanguage = (value: string | undefined): value is DomainLanguage =>
  value === 'en' || value === 'fr'

export default defineEventHandler(async event => {
  const domainLanguage = event.context.params?.domainLanguage
  const fileNameParam = event.context.params?.fileName

  if (!isDomainLanguage(domainLanguage) || typeof fileNameParam !== 'string') {
    throw createError({ statusCode: 404, statusMessage: 'Sitemap not found' })
  }

  const normalizedFileName = basename(fileNameParam)

  if (!normalizedFileName) {
    throw createError({ statusCode: 404, statusMessage: 'Sitemap not found' })
  }

  const configuredFilePath = getLocalSitemapFilePath(
    domainLanguage,
    normalizedFileName
  )

  if (!configuredFilePath) {
    throw createError({
      statusCode: 404,
      statusMessage: 'Sitemap not configured',
    })
  }

  try {
    await access(configuredFilePath)
  } catch (error) {
    throw createError({
      statusCode: 404,
      statusMessage: 'Sitemap file missing',
      cause: error,
    })
  }

  setHeader(event, 'Content-Type', 'application/xml')

  return sendStream(event, createReadStream(configuredFilePath))
})
