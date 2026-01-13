import { defineEventHandler, getRequestURL, sendRedirect, setCookie } from 'h3'

import {
  HOTJAR_RECORDING_COOKIE_MAX_AGE,
  HOTJAR_RECORDING_COOKIE_NAME,
  HOTJAR_RECORDING_COOKIE_VALUE,
} from '~~/shared/utils/hotjar-recording'

const isHomePath = (pathname: string): boolean => pathname === '/'

export default defineEventHandler(event => {
  const requestUrl = getRequestURL(event)

  if (!isHomePath(requestUrl.pathname)) {
    return
  }

  if (!requestUrl.searchParams.has('record')) {
    return
  }

  setCookie(
    event,
    HOTJAR_RECORDING_COOKIE_NAME,
    HOTJAR_RECORDING_COOKIE_VALUE,
    {
      httpOnly: false,
      sameSite: 'lax',
      secure: process.env.NODE_ENV === 'production',
      path: '/',
      maxAge: HOTJAR_RECORDING_COOKIE_MAX_AGE,
    }
  )

  return sendRedirect(event, requestUrl.pathname, 302)
})
