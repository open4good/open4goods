// Centralized helper to log and rethrow server errors with a consistent response
export function _handleError(error: unknown, message: string) {
  console.error(message, error)
  throw createError({
    statusCode: 500,
    statusMessage: message,
  })
}
