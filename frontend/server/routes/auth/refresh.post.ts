import type { H3Event } from 'h3'
import type { CookieSerializeOptions } from 'cookie-es'
import { FetchError } from 'ofetch'

/**
 * Proxy endpoint to renew access and refresh tokens using the backend API.
 */
interface RefreshResponse {
  accessToken: string
  refreshToken: string
}

export default defineEventHandler(async (event: H3Event) => {
  const config = useRuntimeConfig()
  const refreshToken = getCookie(event, config.refreshCookieName)
  if (!refreshToken) {
    throw createError({
      statusCode: 401,
      statusMessage: 'Missing refresh token',
    })
  }

  try {
    const tokens = await $fetch<RefreshResponse>(
      `${config.apiUrl}/auth/refresh`,
      {
        method: 'POST',
        headers: { cookie: `${config.refreshCookieName}=${refreshToken}` },
      }
    )

    const secure = process.env.NODE_ENV === 'production'
    const sameSite: 'lax' | 'none' = secure ? 'none' : 'lax'
    const cookieOptions: CookieSerializeOptions = {
      httpOnly: true,
      sameSite,
      secure,
      path: '/',
    }
    setCookie(event, config.tokenCookieName, tokens.accessToken, cookieOptions)
    setCookie(
      event,
      config.refreshCookieName,
      tokens.refreshToken,
      cookieOptions
    )
    // Return tokens to allow the client to update the auth store reactively
    return tokens
  } catch (err) {
    if (err instanceof FetchError) {
      const fallbackStatusCode = 401
      const fallbackStatusMessage = 'Refresh failed'
      const statusCode = err.response?.status ?? fallbackStatusCode
      const statusMessage = err.response?.statusText ?? fallbackStatusMessage

      let payload: unknown = err.data
      if (payload === undefined && err.response) {
        try {
          payload = await err.response.text()
        } catch (payloadReadError) {
          console.error('Refresh payload read error', payloadReadError)
        }
      }

      const fetchErrorDetails = {
        statusCode,
        statusMessage,
        payload,
      }

      // Surface backend error metadata so clients and logs stay in sync.
      console.error('Refresh fetch error', fetchErrorDetails)
      throw createError({
        statusCode,
        statusMessage,
        cause: fetchErrorDetails,
      })
    }

    console.error('Refresh error', err)
    throw createError({
      statusCode: 401,
      statusMessage: 'Refresh failed',
      cause: err,
    })
  }
})
