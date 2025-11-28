import { Configuration } from '..'
import { createHttpAgent, createHttpsAgent } from './http-agents'

/**
 * Custom fetch implementation with timeout support and connection pooling
 * @param url - URL to fetch
 * @param init - Fetch options
 * @param timeoutMs - Timeout in milliseconds (default: 30000)
 */
async function fetchWithTimeout(url: string, init?: RequestInit, timeoutMs = 30000): Promise<Response> {
  const controller = new AbortController()
  const timeoutId = setTimeout(() => controller.abort(), timeoutMs)
  const isHttps = url.startsWith('https')

  try {
    const agent = isHttps ? createHttpsAgent() : createHttpAgent()
    const response = await fetch(url, {
      ...init,
      agent,
      signal: controller.signal,
    })
    return response
  } finally {
    clearTimeout(timeoutId)
  }
}

/**
 * Factory that builds a backend API configuration seeded with the runtime configuration.
 *
 * Ensures the shared machine token is injected on every request so downstream
 * services can authenticate the proxy calls.
 */
export const createBackendApiConfig = (): Configuration => {
  const isVitest = typeof process !== 'undefined' && process.env?.VITEST === 'true'
  const isServerRuntime = import.meta.server || isVitest

  if (!isServerRuntime) {
    throw new Error('createBackendApiConfig() can only be used on the server runtime.')
  }

  const { apiUrl, machineToken } = useRuntimeConfig()

  if (!machineToken) {
    throw new Error('Missing runtime configuration value "machineToken"; backend calls cannot be authenticated.')
  }

  return new Configuration({
    basePath: apiUrl,
    headers: {
      'X-Shared-Token': machineToken,
    },
    fetchApi: fetchWithTimeout as any,
  })
}
