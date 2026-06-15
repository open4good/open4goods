import { Configuration, type Middleware, type ResponseContext } from '~/generated/backend-client'
import { mapApiError } from '~/domains/errors/api-error.mapper'

/**
 * Middleware to map OpenAPI ResponseErrors into our internal AppApiError format.
 */
const errorMappingMiddleware: Middleware = {
  post: async (context: ResponseContext) => {
    const { response } = context
    if (response.status >= 200 && response.status < 300) {
      return response
    }

    // Attempt to parse body as JSON for the mapper
    let data: unknown
    try {
      data = await response.clone().json()
    } catch {
      data = null
    }

    // Throw an error compatible with our mapper's expectations (status + data)
    throw mapApiError({
      statusCode: response.status,
      data,
      message: response.statusText,
      context: {
        method: context.init.method,
        path: context.url,
        url: context.url,
        runtime: import.meta.server ? 'server' : 'client'
      }
    })
  }
}

export function useBackendClient() {
  const config = useRuntimeConfig()
  const ssrHeaders = import.meta.server ? useRequestHeaders(['cookie']) : {}
  const basePath = resolveBackendBasePath(config.public.backendBaseUrl)

  const configuration = new Configuration({
    basePath,
    credentials: 'include',
    headers: ssrHeaders as Record<string, string>,
    middleware: [errorMappingMiddleware]
  })

  /**
   * Helper to instantiate an API class with shared configuration.
   */
  function createApi<T extends { new (config: Configuration): InstanceType<T> }>(ApiClass: T): InstanceType<T> {
    return new ApiClass(configuration)
  }

  return {
    configuration,
    createApi
  }
}

function resolveBackendBasePath(configuredBaseUrl: string) {
  const normalized = configuredBaseUrl.replace(/\/+$/, '')
  if (import.meta.server && normalized.startsWith('/')) {
    return new URL(normalized, useRequestURL()).toString().replace(/\/+$/, '')
  }
  return normalized
}
