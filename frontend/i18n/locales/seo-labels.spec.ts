import { createI18n } from 'vue-i18n'
import { describe, expect, it } from 'vitest'

import enUS from './en-US.json'
import frFR from './fr-FR.json'

const i18nFor = (locale: string) =>
  createI18n({
    legacy: false,
    locale,
    messages: { 'fr-FR': frFR, 'en-US': enUS },
  }).global

describe('fiche labels', () => {
  it('has no "avis" in the product titles, no review being displayed', () => {
    for (const key of [
      'product.meta.serp.title.withImpact.full',
      'product.meta.serp.title.withoutImpact.full',
    ]) {
      const title = String(
        i18nFor('fr-FR').t(key, {
          productName: 'X',
          score: '12',
          verticalTitle: 'Y',
        })
      )
      expect(title).not.toMatch(/avis/i)
      expect(title.length).toBeLessThan(60)
    }
  })

  it.each(['machines à laver', 'téléviseurs'])(
    'does not agree "notés" with the gender of "%s"',
    items => {
      const t = i18nFor('fr-FR').t
      const sentences = [
        t('components.impactScore.why.headline', {
          score: '12',
          percent: 62,
          count: 1349,
          items,
        }),
        t('components.impactScore.why.headlineBottom', {
          score: '12',
          percent: 62,
          count: 1349,
          items,
        }),
        t(
          'category.ecoscorePage.method.products.ratedCompared',
          { count: 1349, items },
          1349
        ),
      ].map(String)
      for (const s of sentences) {
        expect(s).not.toMatch(new RegExp(`${items} notés`))
        expect(s).toContain(items)
      }
      expect(sentences[0]).toContain(
        'produits notés par Nudger dans la catégorie'
      )
    }
  )
})
