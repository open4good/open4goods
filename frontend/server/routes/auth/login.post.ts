import type { H3Event } from 'h3'
import type { CookieSerializeOptions } from 'cookie-es'
import { FetchError } from 'ofetch'
import { getRequestDomainContext } from '~~/shared/utils/domain-language'

interface LoginResponse { accessToken: string; refreshToken: string }
interface LoginBody { username: string; password: string }

export default defineEventHandler(async (event: H3Event) => {
  const body = await readBody<LoginBody>(event)
  if (!body?.username || !body?.password) {
    throw createError({ statusCode: 400, statusMessage: 'Missing credentials' })
  }

  const config = useRuntimeConfig()
  const { domainLanguage } = getRequestDomainContext(event)
  try {
    const tokens = await $fetch<LoginResponse>(`${config.apiUrl}/auth/login`, {
      method: 'POST',
      body,
      query: { domainLanguage },
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
    // Return tokens so the client can decode and update its state immediately
    return tokens
  } catch (err) {
    if (err instanceof FetchError) {
      const fallbackStatusCode = 401
      const fallbackStatusMessage = 'Invalid credentials'
      const statusCode = err.response?.status ?? fallbackStatusCode
      const statusMessage = err.response?.statusText ?? fallbackStatusMessage

      let payload: unknown = err.data
      if (payload === undefined && err.response) {
        try {
          payload = await err.response.text()
        } catch (payloadReadError) {
          console.error('Login payload read error', payloadReadError)
        }
      }

      const fetchErrorDetails = {
        statusCode,
        statusMessage,
        payload,
      }

      // Surface backend error metadata so clients and logs stay in sync.
      console.error('Login fetch error', fetchErrorDetails)
      throw createError({
        statusCode,
        statusMessage,
        cause: fetchErrorDetails,
      })
    }

    console.error('Login error', err)
    throw createError({ statusCode: 401, statusMessage: 'Invalid credentials', cause: err })
  }
})
