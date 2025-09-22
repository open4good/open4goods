import type { Ref } from 'vue'
import { resolveDomainResolutionFromHost, warnOnUnknownHostname } from '~~/shared/utils/domain-language'

type NuxtI18nInstance = {
  locale: Ref<string>
  setLocale: (locale: string) => Promise<void>
}

export default defineNuxtPlugin(async (nuxtApp) => {
  const serverContext = nuxtApp.ssrContext
  const rawServerHost =
    serverContext?.event.node.req.headers['x-forwarded-host'] ?? serverContext?.event.node.req.headers.host
  const rawClientHost = import.meta.client ? window.location.host : null

  const rawHost = import.meta.server ? rawServerHost : rawClientHost

  const resolution = resolveDomainResolutionFromHost(rawHost)

  if (import.meta.server) {
    warnOnUnknownHostname(resolution)
  }

  const i18n = nuxtApp.$i18n as NuxtI18nInstance | undefined

  if (!i18n) {
    return
  }

  if (i18n.locale.value !== resolution.locale) {
    await i18n.setLocale(resolution.locale)
  }
})
