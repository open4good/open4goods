// Here, create a function that will handle errors in a more global way
export function _handleError(error: unknown, message: string) {
  console.error(message, error)
  throw createError({
    statusCode: 500,
    statusMessage: message,
  })
}
