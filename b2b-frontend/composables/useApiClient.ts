import { mapApiError, type AppApiError } from '~/domains/errors/api-error.mapper'

type HttpMethod = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'

export function useApiClient() {
  const config = useRuntimeConfig()
  const ssrHeaders = import.meta.server ? useRequestHeaders(['cookie']) : {}
  const runtime = import.meta.server ? 'server' : 'client'
  const baseURL = config.public.backendBaseUrl
  const absoluteBaseURL = baseURL.startsWith('/')
    ? new URL(baseURL, useRequestURL().origin).toString().replace(/\/+$/, '')
    : baseURL

  function buildUrl(path: string, query?: Record<string, unknown>) {
    const url = new URL(path, absoluteBaseURL)
    Object.entries(query ?? {}).forEach(([key, value]) => {
      if (value !== undefined && value !== null) {
        url.searchParams.set(key, String(value))
      }
    })
    return url.toString()
  }

  function logApiFailure(method: HttpMethod, path: string, error: unknown, startedAt: number, query?: Record<string, unknown>) {
    const elapsedMs = Math.round(performance.now() - startedAt)
    const context = {
      method,
      path,
      url: buildUrl(path, query),
      runtime,
      elapsedMs
    }
    const err = error as {
      status?: number
      statusCode?: number
      statusMessage?: string
      message?: string
      data?: unknown
      cause?: unknown
    }

    console.error('[API Client] Backend request failed', {
      ...context,
      status: err.status ?? err.statusCode ?? null,
      statusMessage: err.statusMessage,
      message: err.message,
      data: err.data,
      cause: err.cause
    })

    return mapApiError({ ...err, context })
  }

  async function get<T>(path: string, query?: Record<string, unknown>) {
    const startedAt = performance.now()
    try {
      return await $fetch<T>(path, {
        baseURL,
        query,
        credentials: 'include',
        headers: ssrHeaders
      })
    } catch (error) {
      throw logApiFailure('GET', path, error, startedAt, query)
    }
  }

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  async function post<T>(path: string, body?: BodyInit | Record<string, any> | null, query?: Record<string, unknown>) {
    const startedAt = performance.now()
    try {
      return await $fetch<T>(path, {
        method: 'POST',
        baseURL,
        body,
        query,
        credentials: 'include',
        headers: ssrHeaders
      })
    } catch (error) {
      throw logApiFailure('POST', path, error, startedAt, query)
    }
  }

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  async function patch<T>(path: string, body?: BodyInit | Record<string, any> | null, query?: Record<string, unknown>) {
    const startedAt = performance.now()
    try {
      return await $fetch<T>(path, {
        method: 'PATCH',
        baseURL,
        body,
        query,
        credentials: 'include',
        headers: ssrHeaders
      })
    } catch (error) {
      throw logApiFailure('PATCH', path, error, startedAt, query)
    }
  }

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  async function put<T>(path: string, body?: BodyInit | Record<string, any> | null, query?: Record<string, unknown>) {
    const startedAt = performance.now()
    try {
      return await $fetch<T>(path, {
        method: 'PUT',
        baseURL,
        body,
        query,
        credentials: 'include',
        headers: ssrHeaders
      })
    } catch (error) {
      throw logApiFailure('PUT', path, error, startedAt, query)
    }
  }

  async function del<T>(path: string, query?: Record<string, unknown>) {
    const startedAt = performance.now()
    try {
      return await $fetch<T>(path, {
        method: 'DELETE',
        baseURL,
        query,
        credentials: 'include',
        headers: ssrHeaders
      })
    } catch (error) {
      throw logApiFailure('DELETE', path, error, startedAt, query)
    }
  }

  return {
    get,
    post,
    put,
    patch,
    del
  }
}

export type { AppApiError }
