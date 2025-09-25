import type { Composer } from 'vue-i18n'

import { LOCALE_DEFINITIONS } from '../../shared/config/locales'

type VuetifyMessageMap = Record<string, (typeof LOCALE_DEFINITIONS)[number]['vuetify']>

const vuetifyMessages = LOCALE_DEFINITIONS.reduce((accumulator, definition) => {
  accumulator[definition.nuxtLocale] = definition.vuetify
  accumulator[definition.domainLanguage] = definition.vuetify

  return accumulator
}, {} as Record<string, (typeof LOCALE_DEFINITIONS)[number]['vuetify']>) satisfies VuetifyMessageMap

export default defineNuxtPlugin((nuxtApp) => {
  const composer = nuxtApp.$i18n as Composer | undefined

  if (!composer || typeof composer.mergeLocaleMessage !== 'function') {
    return
  }

  for (const [locale, messages] of Object.entries(vuetifyMessages)) {
    if (!messages) {
      continue
    }

    composer.mergeLocaleMessage(locale, {
      $vuetify: messages,
    })
  }
})
