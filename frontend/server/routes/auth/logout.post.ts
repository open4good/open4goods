import type { H3Event } from 'h3'
import { FetchError } from 'ofetch'

const clearAuthCookies = (
  event: H3Event,
  config: { tokenCookieName: string; refreshCookieName: string }
) => {
  deleteCookie(event, config.tokenCookieName, { path: '/' })
  deleteCookie(event, config.refreshCookieName, { path: '/' })
}

export default defineEventHandler(async (event: H3Event) => {
  const config = useRuntimeConfig()
  const token = getCookie(event, config.tokenCookieName)
  const refreshToken = getCookie(event, config.refreshCookieName)

  try {
    const cookieHeader: string[] = []
    if (token) {
      cookieHeader.push(`${config.tokenCookieName}=${token}`)
    }
    if (refreshToken) {
      cookieHeader.push(`${config.refreshCookieName}=${refreshToken}`)
    }

    await $fetch(`${config.apiUrl}/auth/logout`, {
      method: 'POST',
      headers: cookieHeader.length
        ? { cookie: cookieHeader.join('; ') }
        : undefined,
    })
  } catch (err) {
    clearAuthCookies(event, config)

    if (err instanceof FetchError) {
      const fallbackStatusCode = 500
      const fallbackStatusMessage = 'Logout failed'
      const statusCode = err.response?.status ?? fallbackStatusCode
      const statusMessage = err.response?.statusText ?? fallbackStatusMessage

      let payload: unknown = err.data
      if (payload === undefined && err.response) {
        try {
          payload = await err.response.text()
        } catch (payloadReadError) {
          console.error('Logout payload read error', payloadReadError)
        }
      }

      const fetchErrorDetails = {
        statusCode,
        statusMessage,
        payload,
      }

      console.error('Logout fetch error', fetchErrorDetails)
      throw createError({
        statusCode,
        statusMessage,
        cause: fetchErrorDetails,
      })
    }

    console.error('Logout error', err)
    throw createError({
      statusCode: 500,
      statusMessage: 'Logout failed',
      cause: err,
    })
  }

  clearAuthCookies(event, config)

  return { success: true }
})
