import {
  HOTJAR_RECORDING_COOKIE_MAX_AGE,
  HOTJAR_RECORDING_COOKIE_NAME,
  HOTJAR_RECORDING_COOKIE_VALUE,
} from '~~/shared/utils/hotjar-recording'

export default defineNuxtRouteMiddleware(to => {
  const runtimeConfig = useRuntimeConfig()
  const hotjarConfig = runtimeConfig.public.hotjar as
    | { mode?: 'always' | 'never' | 'query' }
    | undefined

  if (hotjarConfig?.mode === 'always' || hotjarConfig?.mode === 'never') {
    return
  }

  if (to.query.record === undefined) {
    return
  }

  const { record, ...restQuery } = to.query
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
        ...restQuery,
      },
    },
    { redirectCode: 302 }
  )
})
