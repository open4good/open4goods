import { createError, setResponseHeader } from 'h3'
import type { H3Event } from 'h3'

/** Seconds suggested to crawlers before retrying a page the backend could not serve. */
export const BACKEND_UNAVAILABLE_RETRY_AFTER_SECONDS = 120

/**
 * Builds the 503 thrown when the backend fails (5xx, timeout) while a page is
 * rendered on the server. A 200 carrying an error banner reads as a soft 404 to
 * crawlers; a 503 with `Retry-After` tells them to come back, and the page is
 * deliberately not marked noindex.
 */
export const createBackendUnavailableError = (
  cause: unknown,
  event?: H3Event | null
) => {
  if (event) {
    setResponseHeader(
      event,
      'Retry-After',
      String(BACKEND_UNAVAILABLE_RETRY_AFTER_SECONDS)
    )
  }

  return createError({
    statusCode: 503,
    statusMessage: 'Service Unavailable',
    cause,
    fatal: true,
  })
}
