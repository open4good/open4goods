import type { Ref } from 'vue'

const DEFAULT_LOCALE = 'en-US' as const
const HOST_LOCALE_MAP: Record<string, typeof DEFAULT_LOCALE | 'fr-FR'> = {
  'nudger.com': DEFAULT_LOCALE,
  'nudger.fr': 'fr-FR',
  localhost: 'fr-FR',
  '127.0.0.1': DEFAULT_LOCALE,
}

const _normalizeHostname = (rawHost: string | undefined | null): string | null => {
  if (!rawHost) {
    return null
  }

  const hostValue = rawHost.split(',')[0]?.trim()
  if (!hostValue) {
    return null
  }

  const hostname = hostValue.split(':')[0]?.toLowerCase()
  return hostname || null
}

const _resolveLocale = (hostname: string | null) => {
  if (!hostname) {
    return {
      locale: DEFAULT_LOCALE,
      matched: false,
    }
  }

  const locale = HOST_LOCALE_MAP[hostname]

  if (!locale) {
    return {
      locale: DEFAULT_LOCALE,
      matched: false,
    }
  }

  return {
    locale,
    matched: true,
  }
}

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

  const hostname = _normalizeHostname(Array.isArray(rawHost) ? rawHost[0] : rawHost)
  const { locale: targetLocale, matched } = _resolveLocale(hostname)

  if (import.meta.server && !matched) {
    console.warn(
      `[i18n] Unknown hostname "${hostname ?? 'unknown'}" received. Falling back to ${DEFAULT_LOCALE}.`
    )
  }

  const i18n = nuxtApp.$i18n as NuxtI18nInstance | undefined

  if (!i18n) {
    return
  }

  if (i18n.locale.value !== targetLocale) {
    await i18n.setLocale(targetLocale)
  }
})
