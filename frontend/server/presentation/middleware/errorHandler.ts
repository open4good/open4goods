import type { H3Event } from 'h3'
import type { DomainError } from '../../shared/errors'

/**
 * Convert domain errors to H3 errors
 * This middleware acts as a bridge between domain layer and HTTP layer
 */
export const handleDomainError = (error: DomainError, _event?: H3Event) => {
  // Log for debugging
  console.error(`[${error.code}] ${error.message}`, {
    statusCode: error.statusCode,
    cause: error.cause,
  })

  // Create H3 error with appropriate status code
  throw createError({
    statusCode: error.statusCode,
    statusMessage: error.message,
    data: error.toJSON(),
    cause: error.cause,
  })
}

/**
 * Handle unknown errors
 */
export const handleUnknownError = (error: unknown, _event?: H3Event) => {
  console.error('Unknown error:', error)

  const message =
    error instanceof Error ? error.message : 'Internal server error'

  throw createError({
    statusCode: 500,
    statusMessage: message,
    cause: error,
  })
}
