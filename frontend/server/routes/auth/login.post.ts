import { readBody, setCookie } from 'h3'

interface LoginResponse {
  token: string
  refreshToken: string
}

export default defineEventHandler(async event => {
  const body = await readBody(event)
  const { username, password } = body as { username: string; password: string }
  const config = useRuntimeConfig()
  try {
    const tokens = await $fetch<LoginResponse>(
      `${config.public.apiUrl}/auth/login`,
      {
        method: 'POST',
        body: { username, password },
      }
    )
    const secure = process.env.NODE_ENV === 'production'
    const cookieOptions = {
      httpOnly: true,
      path: '/',
      secure,
      sameSite: secure ? 'none' : 'lax',
      domain: config.authCookieDomain as string | undefined,
    } as const
    setCookie(event, 'jwt', tokens.token, cookieOptions)
    setCookie(event, 'refresh_token', tokens.refreshToken, cookieOptions)
    return { success: true }
  } catch (err) {
    console.error('Login error', err)
    throw createError({
      statusCode: 401,
      statusMessage: 'Invalid credentials',
      cause: err,
    })
  }
})
