import { createError } from 'h3'
import { useNuxtApp } from '#imports'
import {
  matchLocalizedRouteByPath,
  normalizeLocale,
} from '~~/shared/utils/localized-routes'

export default defineNuxtRouteMiddleware(to => {
  const nuxtApp = useNuxtApp()
  const i18n = nuxtApp.$i18n as
    | { locale?: { value?: string | null } }
    | undefined
  const activeLocale = normalizeLocale(i18n?.locale?.value ?? undefined)
  const matchedRoute = matchLocalizedRouteByPath(to.path)

  if (!matchedRoute) {
    return
  }

  if (matchedRoute.locale !== activeLocale) {
    return abortNavigation(
      createError({
        statusCode: 404,
        statusMessage: 'Not Found',
      })
    )
  }
})
