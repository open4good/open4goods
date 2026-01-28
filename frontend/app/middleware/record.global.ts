import {
  HOTJAR_RECORDING_COOKIE_MAX_AGE,
  HOTJAR_RECORDING_COOKIE_NAME,
  HOTJAR_RECORDING_COOKIE_VALUE,
} from '../../shared/utils/hotjar-recording'

export default defineNuxtRouteMiddleware(to => {
  if (to.query.record === undefined) {
    return
  }

  const cookie = useCookie(HOTJAR_RECORDING_COOKIE_NAME, {
    maxAge: HOTJAR_RECORDING_COOKIE_MAX_AGE,
    sameSite: 'lax',
    secure: process.env.NODE_ENV === 'production',
  })

  cookie.value = HOTJAR_RECORDING_COOKIE_VALUE

  // Remove the query param from the URL
  return navigateTo(
    {
      path: to.path,
      query: {
        ...to.query,
        record: undefined,
      },
    },
    { redirectCode: 302 }
  )
})
