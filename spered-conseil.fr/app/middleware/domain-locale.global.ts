import { useI18n } from 'vue-i18n'

export default defineNuxtRouteMiddleware(() => {
  if (import.meta.server) {
    return
  }

  const runtimeConfig = useRuntimeConfig()
  const localeDomains = runtimeConfig.public.localeDomains as {
    fr?: string
    en?: string
  }

  const { locale } = useI18n()
  const currentHost = window.location.host.toLowerCase()
  const resolvedLocale =
    localeDomains.en && currentHost.includes(localeDomains.en.toLowerCase())
      ? 'en-US'
      : 'fr-FR'

  if (locale.value !== resolvedLocale) {
    locale.value = resolvedLocale
  }
})
