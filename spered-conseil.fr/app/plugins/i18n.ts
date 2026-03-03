import { createI18n } from 'vue-i18n'
import enUS from '~/i18n/locales/en-US.json'
import frFR from '~/i18n/locales/fr-FR.json'

const detectLocaleFromHost = (
  host: string,
  localeDomains: { fr?: string; en?: string }
): 'fr-FR' | 'en-US' => {
  const normalizedHost = host.toLowerCase()

  if (localeDomains.en && normalizedHost.includes(localeDomains.en.toLowerCase())) {
    return 'en-US'
  }

  return 'fr-FR'
}

export default defineNuxtPlugin(nuxtApp => {
  const runtimeConfig = useRuntimeConfig()
  const localeDomains = runtimeConfig.public.localeDomains as {
    fr?: string
    en?: string
  }

  const requestUrl = import.meta.server ? useRequestURL() : null
  const host = import.meta.server
    ? requestUrl?.host ?? localeDomains.fr ?? 'spered-conseil.fr'
    : window.location.host

  const locale = detectLocaleFromHost(host, localeDomains)

  const i18n = createI18n({
    legacy: false,
    locale,
    fallbackLocale: 'fr-FR',
    messages: {
      'fr-FR': frFR,
      'en-US': enUS,
    },
  })

  nuxtApp.vueApp.use(i18n)
})
