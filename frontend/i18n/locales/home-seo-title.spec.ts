import { createI18n } from 'vue-i18n'
import { describe, expect, it } from 'vitest'

import enUS from './en-US.json'
import frFR from './fr-FR.json'

// vue-i18n reads a bare "|" as its plural separator and would keep only the
// first form ("Nudger"): the separator must be written as a literal.
describe('home SEO title', () => {
  it.each([
    ['fr-FR', 'Nudger | Comparateur écologique'],
    ['en-US', enUS.home.seo.title],
  ])('keeps the whole title in %s', (locale, expected) => {
    const i18n = createI18n({
      legacy: false,
      locale,
      messages: { 'fr-FR': frFR, 'en-US': enUS },
    })

    expect(String(i18n.global.t('home.seo.title'))).toBe(expected)
  })
})
