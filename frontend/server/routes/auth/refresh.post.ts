import type { H3Event } from 'h3'
import type { CookieSerializeOptions } from 'cookie-es'

/**
 * Proxy endpoint to renew access and refresh tokens using the backend API.
 */
interface RefreshResponse { accessToken: string; refreshToken: string }

export default defineEventHandler(async (event: H3Event) => {
  const config = useRuntimeConfig()
  const refreshToken = getCookie(event, config.refreshCookieName)
  if (!refreshToken) {
    throw createError({ statusCode: 401, statusMessage: 'Missing refresh token' })
  }

  try {
    const tokens = await $fetch<RefreshResponse>(`${config.public.apiUrl}/auth/refresh`, {
      method: 'POST',
      headers: { cookie: `${config.refreshCookieName}=${refreshToken}` },
    })

    const secure = process.env.NODE_ENV === 'production'
    const sameSite: 'lax' | 'none' = secure ? 'none' : 'lax'
    const cookieOptions: CookieSerializeOptions = {
      httpOnly: true,
      sameSite,
      secure,
      path: '/',
    }
    setCookie(event, config.tokenCookieName, tokens.accessToken, cookieOptions)
    setCookie(event, config.refreshCookieName, tokens.refreshToken, cookieOptions)
    return { success: true }
  } catch (err) {
    console.error('Refresh error', err)
    throw createError({ statusCode: 401, statusMessage: 'Refresh failed' })
  }
})
