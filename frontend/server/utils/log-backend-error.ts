import { ResponseError } from '~~/shared/api-client'

export interface BackendErrorDetails {
  statusCode: number
  statusText?: string
  statusMessage: string
  bodyText?: string
  isResponseError: boolean
  logMessage: string
}

function buildLogMessage({
  statusCode,
  statusText,
  statusMessage,
}: Pick<BackendErrorDetails, 'statusCode' | 'statusText' | 'statusMessage'>) {
  const parts = [`HTTP ${statusCode}`]

  if (statusText) {
    parts.push(statusText)
  }

  if (statusMessage && statusMessage !== statusText) {
    parts.push(statusMessage)
  }

  return parts.join(' - ')
}

export async function extractBackendErrorDetails(
  error: unknown
): Promise<BackendErrorDetails> {
  if (error instanceof ResponseError) {
    let bodyText: string | undefined
    try {
      bodyText = await error.response.text()
    } catch {
      bodyText = undefined
    }

    const trimmedBody = bodyText?.trim()
    const statusText = error.response.statusText || undefined
    const statusMessage =
      trimmedBody && trimmedBody.length > 0
        ? trimmedBody
        : statusText || `HTTP ${error.response.status}`

    return {
      statusCode: error.response.status,
      statusText,
      statusMessage,
      bodyText: trimmedBody,
      isResponseError: true,
      logMessage: buildLogMessage({
        statusCode: error.response.status,
        statusText,
        statusMessage,
      }),
    }
  }

  const fallbackMessage =
    error instanceof Error ? error.message : 'Unexpected error while calling backend'

  const statusCode = 500
  const statusText = 'Internal Server Error'

  return {
    statusCode,
    statusText,
    statusMessage: fallbackMessage,
    bodyText: undefined,
    isResponseError: false,
    logMessage: buildLogMessage({
      statusCode,
      statusText,
      statusMessage: fallbackMessage,
    }),
  }
}
