import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ProductImpactWhyThisScore from './ProductImpactWhyThisScore.vue'
import enUS from '~~/i18n/locales/en-US.json'
import frFR from '~~/i18n/locales/fr-FR.json'
import type { ProductImpactScoreStatusDto } from '~~/shared/api-client'
import type { ScoreView } from './impact-types'

const mountBlock = (
  props: Record<string, unknown>,
  locale: 'en' | 'fr' = 'en'
) => {
  const i18n = createI18n({
    legacy: false,
    locale,
    messages: {
      en: enUS as Record<string, unknown>,
      fr: frFR as Record<string, unknown>,
    },
  })
  return mount(ProductImpactWhyThisScore, {
    props: {
      itemsLabel: 'televisions',
      methodologyHref: '/tv/ecoscore',
      ...props,
    },
    global: {
      plugins: [i18n],
      stubs: { NuxtLink: { template: '<a><slot /></a>' } },
    },
  })
}

const scores: ScoreView[] = [
  {
    id: 'POWER',
    label: 'Power use',
    relativeValue: 5,
    coefficient: 2,
    attributeValue: '41',
    attributeSuffix: 'W',
    absolute: { avg: 78 },
  },
  { id: 'WARRANTY', label: 'Warranty', relativeValue: 4, coefficient: 1 },
  { id: 'PARTS', label: 'Spare parts', relativeValue: 0.5, coefficient: 1 },
  {
    id: 'REPAIR',
    label: 'Repairability',
    relativeValue: null,
    notFilled: true,
  },
]

const rated = (
  level: string,
  extra: Partial<ProductImpactScoreStatusDto> = {}
): ProductImpactScoreStatusDto => ({
  status: 'RATED',
  level: level as ProductImpactScoreStatusDto['level'],
  ratedCount: 412,
  betterThanPercent: 97,
  realCriteriaCount: 3,
  totalCriteriaCount: 4,
  methodologyVersion: 2,
  scoredAt: Date.UTC(2026, 10, 8, 12),
  ...extra,
})

describe('ProductImpactWhyThisScore', () => {
  it('top variant: score, position, strengths with category average, caveat', () => {
    const text = mountBlock({
      status: rated('excellent'),
      scores,
      score: 14.2,
    }).text()
    expect(text).toContain('Why this score?')
    expect(text).toContain('14.2/20')
    expect(text).toContain('above 97% of the 412 televisions')
    expect(text).toContain('Power use (41 W, versus 78 W on average')
    expect(text).toContain('Spare parts')
    expect(text).toContain('Repairability')
    expect(text).toContain('3 of 4 criteria')
    expect(text).toContain('A high score does not mean no impact')
  })

  it('middle variant: no caveat, favourable and limiting points', () => {
    const text = mountBlock({
      status: rated('good'),
      scores,
      score: 11,
    }).text()
    expect(text).toContain('Favourable points')
    expect(text).toContain('Points that limit the score')
    expect(text).not.toContain('does not mean no impact')
  })

  it('bottom variant: leads with what weighs on the score', () => {
    const wrapper = mountBlock({
      status: rated('poor', { betterThanPercent: 8 }),
      scores,
      score: 4,
    })
    const items = wrapper.findAll('li').map(li => li.text())
    expect(wrapper.text()).toContain('ahead of 8%')
    expect(items[0]).toContain('What weighs most on the score')
    expect(wrapper.text()).toContain('does not measure its real impact')
  })

  it('Non noté: reason, missing data, no number, no level', () => {
    const wrapper = mountBlock({
      status: {
        status: 'NOT_RATED',
        notRatedReason: 'LOW_COVERAGE',
        realCriteriaCount: 1,
        totalCriteriaCount: 4,
      },
      scores,
      score: null,
    })
    const text = wrapper.text()
    expect(text).toContain('Not rated')
    expect(text).toContain('1 of 4 criteria are provided')
    expect(text).toContain('Missing data: Repairability')
    expect(text).toContain('not rated lower')
    expect(text).not.toMatch(/\/20/)
  })

  it('omits the headline when the score is missing, with no placeholder', () => {
    const text = mountBlock({
      status: rated('good'),
      scores,
      score: null,
    }).text()
    expect(text).not.toContain('/20')
    expect(text).not.toContain('undefined')
  })

  it('single rated product: no percentage', () => {
    const text = mountBlock({
      status: rated('good', { ratedCount: 1, betterThanPercent: 100 }),
      scores,
      score: 12,
    }).text()
    expect(text).toContain('only rated product')
    expect(text).not.toContain('100%')
  })

  it('renders in French', () => {
    const text = mountBlock(
      {
        status: rated('excellent'),
        scores,
        score: 14.2,
        itemsLabel: 'téléviseurs',
      },
      'fr'
    ).text()
    expect(text).toContain('Pourquoi cette note ?')
    expect(text).toContain('mieux classé que 97 %')
    expect(text).toContain('Critères non renseignés : Repairability')
  })

  it('renders nothing without a V2 status', () => {
    expect(mountBlock({ status: null, scores }).find('section').exists()).toBe(
      false
    )
  })

  it('keeps forbidden claims out of the block texts', () => {
    const forbidden =
      /écologique|ecological|\bvert\b|\bgreen\b|responsable|sobre|planète|planet/i
    for (const messages of [enUS, frFR]) {
      const why = (
        messages as { components: { impactScore: { why: unknown } } }
      ).components.impactScore.why
      expect(JSON.stringify(why)).not.toMatch(forbidden)
    }
  })
})
