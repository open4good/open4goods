/**
 * Retry logic with exponential backoff for API calls
 * Handles network-related errors that occur during API requests
 */
export async function withRetry<T>(
  fn: () => Promise<T>,
  maxRetries = 2,
  initialDelayMs = 500
): Promise<T> {
  let lastError: Error | undefined

  for (let attempt = 0; attempt <= maxRetries; attempt++) {
    try {
      return await fn()
    } catch (error) {
      lastError = error instanceof Error ? error : new Error(String(error))

      // Only retry on network-related errors (timeouts, connection resets, etc.)
      const isNetworkError =
        lastError.name === 'AbortError' || // Timeout from AbortController
        lastError.message?.includes('socket hang up') ||
        lastError.message?.includes('Premature') ||
        lastError.message?.includes('ECONNRESET') ||
        lastError.message?.includes('ETIMEDOUT') ||
        lastError.message?.includes('timed out')

      if (attempt < maxRetries && isNetworkError) {
        const delayMs = initialDelayMs * Math.pow(2, attempt)
        await new Promise(resolve => setTimeout(resolve, delayMs))
        continue
      }

      throw error
    }
  }

  throw lastError
}
