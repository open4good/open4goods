import { Configuration } from '..'

/**
 * Factory that builds a backend API configuration seeded with the runtime configuration.
 *
 * Ensures the shared machine token is injected on every request so downstream
 * services can authenticate the proxy calls.
 */
export const createBackendApiConfig = (): Configuration => {
  const isVitest =
    typeof process !== 'undefined' && process.env?.VITEST === 'true'
  const isServerRuntime = import.meta.server || isVitest

  if (!isServerRuntime) {
    throw new Error(
      'createBackendApiConfig() can only be used on the server runtime.'
    )
  }

  const { apiUrl, machineToken } = useRuntimeConfig()

  if (!machineToken) {
    throw new Error(
      'Missing runtime configuration value "machineToken"; backend calls cannot be authenticated.'
    )
  }

  return new Configuration({
    basePath: apiUrl,
    headers: {
      'X-Shared-Token': machineToken,
    },
  })
}
