import type { Ref } from 'vue'
import {
  DEFAULT_LOCALE,
  DEFAULT_DOMAIN_LANGUAGE,
  getRequestDomainContext,
  resolveDomainContext,
  syncDomainLanguageState,
} from '~~/shared/utils/domain-language'

type NuxtI18nInstance = {
  locale: Ref<string>
  setLocale: (locale: string) => Promise<void>
}

export default defineNuxtPlugin(async (nuxtApp) => {
  const serverContext = nuxtApp.ssrContext
  let resolution = resolveDomainContext(null)

  if (import.meta.server && serverContext?.event) {
    resolution = getRequestDomainContext(serverContext.event)
  } else {
    const rawClientHost = import.meta.client ? window.location.host : null
    resolution = resolveDomainContext(rawClientHost)

    if (!resolution.matched) {
      console.warn(
        `[i18n] Unknown hostname "${resolution.hostname ?? 'unknown'}" received on client. Falling back to ${DEFAULT_LOCALE}/${DEFAULT_DOMAIN_LANGUAGE}.`
      )
    }
  }

  const domainLanguageState = syncDomainLanguageState(resolution.domainLanguage)
  nuxtApp.vueApp.provide('domainLanguage', domainLanguageState)
  nuxtApp.provide('domainLanguage', domainLanguageState)

  const i18n = nuxtApp.$i18n as NuxtI18nInstance | undefined

  if (!i18n) {
    return
  }

  if (i18n.locale.value !== resolution.locale) {
    await i18n.setLocale(resolution.locale)
  }
})
