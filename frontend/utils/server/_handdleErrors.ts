// Helper to throw consistent server errors
export function _handleError(error: unknown, message: string) {
  console.error(message, error)
  throw createError({
    statusCode: 500,
    statusMessage: message,
  })
}
