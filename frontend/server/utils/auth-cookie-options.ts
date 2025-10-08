import type { CookieSerializeOptions } from 'cookie-es'
import type { H3Event } from 'h3'
import { getRequestProtocol } from 'h3'

/**
 * Builds consistent cookie options for authentication cookies.
 * Ensures Secure/SameSite flags align with the current request protocol so
 * local HTTP previews keep working while HTTPS deployments remain protected.
 */
export const buildAuthCookieOptions = (event: H3Event): CookieSerializeOptions => {
  const secure = getRequestProtocol(event) === 'https'

  return {
    httpOnly: true,
    sameSite: secure ? 'none' : 'lax',
    secure,
    path: '/',
  }
}
