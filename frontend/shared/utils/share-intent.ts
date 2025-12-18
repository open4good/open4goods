import { extractGtinParam, findGtinInText } from './_gtin'

export interface ShareIntentPayload {
  title?: string | null
  text?: string | null
  url?: string | null
  fileText?: string | null
}

export interface ShareIntentResolution {
  gtin: string | null
  query: string | null
}

export const deriveQueryFromUrl = (rawUrl?: string | null): string | null => {
  if (!rawUrl) {
    return null
  }

  try {
    const parsed = new URL(rawUrl)
    const pathname = parsed.pathname || ''
    const segments = pathname
      .split('/')
      .map(segment => segment.trim())
      .filter(Boolean)

    if (segments.length) {
      const lastSegment = segments[segments.length - 1]!
      const cleaned = lastSegment.replace(/[-_]+/g, ' ').trim()
      if (cleaned) {
        return cleaned
      }
    }

    const host = parsed.hostname.replace(/^www\./i, '').trim()
    return host || null
  } catch {
    return null
  }
}

const MAX_QUERY_LENGTH = 160

const normalizeText = (value?: string | null): string | null => {
  const trimmed = value?.trim()
  return trimmed && trimmed.length > 0 ? trimmed : null
}

const extractGtinFromUrl = (rawUrl?: string | null): string | null => {
  if (!rawUrl) {
    return null
  }

  try {
    const parsedUrl = new URL(rawUrl)
    const searchParamCandidates = [
      parsedUrl.searchParams.get('gtin'),
      parsedUrl.searchParams.get('ean'),
      parsedUrl.searchParams.get('barcode'),
    ]

    for (const candidate of searchParamCandidates) {
      const normalized = extractGtinParam(candidate)
      if (normalized) {
        return normalized
      }
    }

    const pathSegments = parsedUrl.pathname.split('/')
    for (const segment of pathSegments.reverse()) {
      const normalized = extractGtinParam(segment) ?? findGtinInText(segment)
      if (normalized) {
        return normalized
      }
    }
  } catch {
    const normalized = extractGtinParam(rawUrl) ?? findGtinInText(rawUrl)
    if (normalized) {
      return normalized
    }
  }

  return null
}

export const resolveShareIntent = (
  payload: ShareIntentPayload
): ShareIntentResolution => {
  const candidates = [payload.text, payload.title, payload.fileText]

  for (const candidate of candidates) {
    const gtin = findGtinInText(candidate)
    if (gtin) {
      return { gtin, query: normalizeText(candidate) }
    }
  }

  const urlGtin = extractGtinFromUrl(payload.url)
  if (urlGtin) {
    return {
      gtin: urlGtin,
      query: normalizeText(payload.text || payload.title || payload.url),
    }
  }

  const fallbackQuery =
    normalizeText(payload.text) ||
    normalizeText(payload.title) ||
    normalizeText(payload.fileText) ||
    normalizeText(payload.url)

  const safeQuery = fallbackQuery?.slice(0, MAX_QUERY_LENGTH) ?? null

  return {
    gtin: null,
    query: safeQuery,
  }
}
