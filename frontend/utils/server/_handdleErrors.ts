// Helper to centralize error handling in server API routes
import { FetchError, ResponseError } from '~/src/api'

interface ResponseHolder {
  response?: { status?: number }
}

export function _handleError(error: unknown, message: string): never {
  let status = 500

  if (error instanceof ResponseError) {
    status = error.response.status
    console.error(`${message} (status ${status})`, error)
  } else if (error instanceof FetchError) {
    const cause = error.cause as ResponseHolder | undefined
    if (cause?.response?.status) {
      status = cause.response.status
      console.error(`${message} (status ${status})`, error)
    } else {
      console.error(message, error)
    }
  } else if (
    typeof error === 'object' &&
    error !== null &&
    'response' in error &&
    (error as ResponseHolder).response?.status
  ) {
    const holder = error as ResponseHolder
    status = holder.response?.status ?? 500
    console.error(`${message} (status ${status})`, error)
  } else {
    console.error(message, error)
  }

  throw createError({
    statusCode: status,
    statusMessage: message,
    cause: error instanceof Error ? error : undefined,
  })
}
