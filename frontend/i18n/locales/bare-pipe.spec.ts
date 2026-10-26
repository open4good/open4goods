import { createI18n } from 'vue-i18n'
import { describe, expect, it } from 'vitest'

import enUS from './en-US.json'
import frFR from './fr-FR.json'

// vue-i18n reads a bare "|" as its plural separator. Only the messages listed
// here are real plurals (called with a count); any other message must write
// the separator as a literal: {'|'}.
const PLURAL_KEYS = new Set([
  'common.count.months',
  'common.count.years',
  'home.hero.search.partnerLinkLabel',
  'product.banner.offersCount',
  'product.docs.pageCount',
  'product.hero.moreOffersBadge',
  'product.hero.offersCountLabel',
  'product.hero.trendPeriodDays',
  'product.hero.trendPeriodHours',
  'product.hero.trendPeriodMinutes',
  'product.hero.viewOffersNew',
  'product.hero.viewOffersOccasion',
  'product.vigilance.teaser.alertCount',
  'siteIdentity.menu.account.privacy.compare.count',
])

const collectBarePipes = (node: unknown, path = ''): string[] => {
  if (typeof node === 'string') {
    return node.replace(/\{'\|'\}/g, '').includes('|') ? [path] : []
  }
  if (node && typeof node === 'object') {
    return Object.entries(node).flatMap(([key, value]) =>
      collectBarePipes(value, path ? `${path}.${key}` : key),
    )
  }
  return []
}

describe('i18n bare pipe', () => {
  it.each([
    ['fr-FR', frFR],
    ['en-US', enUS],
  ])('only allows a bare "|" in plural messages in %s', (_locale, messages) => {
    const offenders = collectBarePipes(messages).filter(
      (key) => !PLURAL_KEYS.has(key),
    )

    expect(offenders).toEqual([])
  })

  it.each([
    ['fr-FR', 'product.meta.serp.title.withImpact.compact', { productName: 'X', score: '15' }, 'X : meilleur prix | ImpactScore 15/20'],
    ['fr-FR', 'dataviz.page.metaTitle', { category: 'TV', siteName: 'Nudger' }, 'Statistiques et données - TV | Nudger'],
    ['fr-FR', 'metriks.seo.title', {}, 'Metriks - Tableau de bord | Nudger'],
  ])('renders the whole title for %s %s', (locale, key, params, expected) => {
    const i18n = createI18n({
      legacy: false,
      locale,
      messages: { 'fr-FR': frFR, 'en-US': enUS },
    })

    expect(String(i18n.global.t(key, params))).toBe(expected)
  })
})
