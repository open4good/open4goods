import type { H3Event } from 'h3'
import type { CookieSerializeOptions } from 'cookie-es'

interface LoginResponse { accessToken: string; refreshToken: string }
interface LoginBody { username: string; password: string }

export default defineEventHandler(async (event: H3Event) => {
  const body = await readBody<LoginBody>(event)
  if (!body?.username || !body?.password) {
    throw createError({ statusCode: 400, statusMessage: 'Missing credentials' })
  }

  const config = useRuntimeConfig()
  try {
    const tokens = await $fetch<LoginResponse>(`${config.public.apiUrl}/auth/login`, {
      method: 'POST',
      body,
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
    console.error('Login error', err)
    throw createError({ statusCode: 401, statusMessage: 'Invalid credentials' })
  }
})
