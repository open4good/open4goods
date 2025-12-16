import { ResponseError } from '~~/shared/api-client'

export interface BackendErrorDetails {
  statusCode: number
  statusText?: string
  statusMessage: string
  bodyText?: string
  isResponseError: boolean
  logMessage: string
}

const GLOBAL_STATE_KEY = Symbol.for('open4goods.backendErrorLogger')

interface BackendErrorLoggerState {
  lastLoggedAt: Map<string, number>
}

const getLoggerState = (): BackendErrorLoggerState => {
  const globalScope = globalThis as typeof globalThis & {
    [GLOBAL_STATE_KEY]?: BackendErrorLoggerState
  }

  if (!globalScope[GLOBAL_STATE_KEY]) {
    globalScope[GLOBAL_STATE_KEY] = {
      lastLoggedAt: new Map<string, number>(),
    }
  }

  return globalScope[GLOBAL_STATE_KEY] as BackendErrorLoggerState
}

interface LogBackendErrorOptions {
  /**
   * Unique identifier for the call site.
   * When combined with the backend error log message it helps throttling duplicates.
   */
  namespace: string
  details: BackendErrorDetails
  /**
   * Minimum delay (in milliseconds) between two identical logs.
   * Defaults to 60 seconds which keeps local dev consoles readable
   * while still surfacing persistent outages.
   */
  throttleMs?: number
}

export const logBackendError = ({
  namespace,
  details,
  throttleMs = 60_000,
}: LogBackendErrorOptions) => {
  const { lastLoggedAt } = getLoggerState()
  const cacheKey = `${namespace}:${details.logMessage}`
  const now = Date.now()
  const lastLogTime = lastLoggedAt.get(cacheKey) ?? 0

  if (now - lastLogTime < throttleMs) {
    return
  }

  lastLoggedAt.set(cacheKey, now)
  console.error(namespace, details.logMessage, details)
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
    error instanceof Error
      ? error.message
      : 'Unexpected error while calling backend'

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
