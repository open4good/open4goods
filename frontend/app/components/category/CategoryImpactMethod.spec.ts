import { mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import { defineComponent, h } from 'vue'

import CategoryImpactMethod from './CategoryImpactMethod.vue'
import type {
  AttributeConfigDto,
  ImpactScoreConfigDto,
  ImpactScoreStatsDto,
} from '~~/shared/api-client'

vi.mock('vue-i18n', () => ({
  useI18n: () => ({
    t: (key: string, params?: Record<string, unknown>) =>
      params ? `${key} ${JSON.stringify(params)}` : key,
    locale: { value: 'en-US' },
  }),
}))

const config: ImpactScoreConfigDto = {
  methodologyVersion: 1,
  minRealWeightCoverage: 0.6,
  criteriasPonderation: { REPAIRABILITY: 0.3, POWER: 0.7 },
  audit: {
    whatItMeansForUsers: 'Helps you pick a lasting television.',
    notAnAbsoluteFootprint: true,
    whatIsMissingVsFullLca: 'Manufacturing emissions are not measured.',
    whyRelativeIsStillUseful: 'Ranking is still useful.',
    mitigations: ['Compare within the category only.'],
    criteria: [
      {
        key: 'POWER',
        weight: 0.7,
        directionality: 'lower_is_better',
        rationale: 'Use phase dominates the footprint.',
        evidenceLevel: 'high',
      },
    ],
    sources: [
      {
        id: 'S1',
        title: 'EU energy label',
        authorsOrOrg: 'EC',
        year: 2021,
        url: 'https://example.com/eprel',
      },
    ],
  },
}

const stats: ImpactScoreStatsDto = {
  verticalId: 'tv',
  methodologyVersion: 2,
  ratedCount: 100,
  notRatedLowCoverageCount: 20,
  notRatedNoCriteriaCount: 5,
  outdatedCount: 3,
  excludedCount: 7,
  brandCount: 12,
  coverageByCriterion: { POWER: 90, REPAIRABILITY: 40 },
  realCriteriaCountDistribution: { 1: 60, 2: 40 },
  scoreHistogram: [0, 5, 20, 50, 25],
  scoreBandWidth: 2,
  scoreMin: 2.1,
  scoreMedian: 6.4,
  scoreMax: 9.8,
  lastScoredAt: Date.UTC(2026, 10, 8),
}

// V2 matrix: a derived criterion absent from the stored V1 audit, which keeps
// its V1 texts (mitigations, rationales) and must not describe the V2 method.
const v2Config: ImpactScoreConfigDto = {
  ...config,
  methodologyVersion: 2,
  criteriasPonderation: { POWER_DENSITY: 0.7, REPAIRABILITY: 0.3 },
  audit: {
    ...config.audit,
    mitigations: [
      'Raise DATA_QUALITY because missing criteria are imputed to the mean.',
    ],
    criteria: [
      {
        key: 'REPAIRABILITY',
        weight: 0.14,
        directionality: 'categorical',
        rationale: 'V1 rationale of the repairability index.',
      },
    ],
  },
}

const criteriaAttributes: Record<string, AttributeConfigDto> = {
  POWER_DENSITY: {
    key: 'POWER_DENSITY',
    impactBetterIs: 'LOWER',
    scoreDescription: 'Power divided by screen area.',
    scoreUtility: 'Compares a 32-inch and a 75-inch television.',
  },
  REPAIRABILITY: {
    key: 'REPAIRABILITY',
    impactBetterIs: 'GREATER',
    scoreDescription: 'Official index out of 10.',
  },
}

const mountMethod = (
  statsValue: ImpactScoreStatsDto | null,
  configValue: ImpactScoreConfigDto = config
) =>
  mount(CategoryImpactMethod, {
    props: {
      config: configValue,
      stats: statsValue,
      categoryLabel: 'Televisions',
      criteriaLabels: {
        POWER: 'Energy',
        POWER_DENSITY: 'Energy per screen area',
        REPAIRABILITY: 'Repairability',
      },
      criteriaAttributes,
      idPrefix: 'method',
    },
    global: {
      stubs: {
        NuxtLink: defineComponent({
          props: { to: { type: String, default: '' } },
          setup(props, { slots }) {
            return () => h('a', { href: props.to }, slots.default?.())
          },
        }),
        'v-alert': { template: '<div class="alert"><slot /></div>' },
      },
    },
  })

describe('CategoryImpactMethod', () => {
  it('shows the stored audit texts instead of a fallback', () => {
    const wrapper = mountMethod(stats)
    expect(wrapper.get('[data-test="what-it-means"]').text()).toBe(
      'Helps you pick a lasting television.'
    )
    expect(wrapper.find('[data-test="not-absolute"]').exists()).toBe(true)
    expect(wrapper.text()).toContain(
      'Manufacturing emissions are not measured.'
    )
    expect(wrapper.text()).toContain('Use phase dominates the footprint.')
    expect(wrapper.text()).toContain('EU energy label')
  })

  it('lists criteria by decreasing weight', () => {
    const rows = mountMethod(stats).findAll(
      '[data-test="criteria-table"] tbody tr'
    )
    expect(rows).toHaveLength(2)
    expect(rows[0]!.text()).toContain('Energy')
    expect(rows[0]!.text()).toContain('70%')
    expect(rows[1]!.text()).toContain('30%')
  })

  it('renders real coverage and distribution with accessible labels', () => {
    const wrapper = mountMethod(stats)
    const coverage = wrapper.findAll('[data-test="coverage"] li')
    expect(coverage).toHaveLength(2)
    expect(coverage[0]!.text()).toContain('90%')
    expect(coverage[1]!.text()).toContain('40%')
    expect(
      wrapper
        .findAll('[data-test="coverage"] [role="img"]')[0]!
        .attributes('aria-label')
    ).toContain('90')
    expect(wrapper.findAll('[data-test="distribution"] li')).toHaveLength(5)
    expect(wrapper.get('[data-test="counts"]').text()).toContain('"count":"25"')
  })

  it('shows covered brands, excluded products and the update fate', () => {
    const wrapper = mountMethod(stats)
    expect(wrapper.get('[data-test="brands"]').text()).toContain('"count":"12"')
    expect(wrapper.get('[data-test="excluded"]').text()).toContain(
      '"count":"7"'
    )
    expect(wrapper.find('[data-test="outdated-fate"]').exists()).toBe(true)
  })

  it('links to localized partners and contact pages', () => {
    const hrefs = mountMethod(stats)
      .findAll('a')
      .map(a => a.attributes('href'))
    expect(hrefs).toContain('/partners')
    expect(hrefs).toContain('/contact')
  })

  it('does not invent figures when stats are missing', () => {
    const wrapper = mountMethod(null)
    expect(wrapper.find('[data-test="stats-unavailable"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="coverage"]').exists()).toBe(false)
    expect(wrapper.find('[data-test="distribution"]').exists()).toBe(false)
  })

  it('keeps the V1 audit texts and adds the disclaimer in V1', () => {
    const wrapper = mountMethod(stats)
    expect(wrapper.get('[data-test="mitigations"]').text()).toContain(
      'Compare within the category only.'
    )
    expect(wrapper.find('[data-test="disclaimer"]').text()).toBe(
      'components.impactScore.v2.disclaimer'
    )
    expect(wrapper.find('[data-test="rated-count"]').exists()).toBe(false)
    expect(wrapper.find('[data-test="sources-v1"]').exists()).toBe(false)
  })
})

describe('CategoryImpactMethod with a V2 matrix', () => {
  it('describes criteria from their attributes, not from the V1 audit', () => {
    const rows = mountMethod(stats, v2Config).findAll(
      '[data-test="criteria-table"] tbody tr'
    )
    expect(rows).toHaveLength(2)
    expect(rows[0]!.text()).toContain('Energy per screen area')
    expect(rows[0]!.text()).toContain(
      'category.ecoscorePage.method.method.lower'
    )
    expect(rows[0]!.text()).toContain(
      'Compares a 32-inch and a 75-inch television.'
    )
    expect(rows[1]!.text()).toContain('Repairability')
    expect(rows[1]!.text()).toContain(
      'category.ecoscorePage.method.method.higher'
    )
    expect(rows[1]!.text()).toContain('Official index out of 10.')
    expect(rows[1]!.text()).not.toContain('V1 rationale')
    rows.forEach(row => expect(row.text()).not.toContain('—'))
  })

  it('hides the V1 audit texts and shows the disclaimer', () => {
    const wrapper = mountMethod(stats, v2Config)
    expect(wrapper.find('[data-test="mitigations"]').exists()).toBe(false)
    expect(wrapper.find('[data-test="not-absolute"]').exists()).toBe(false)
    expect(wrapper.get('[data-test="what-it-means"]').text()).toContain(
      'category.ecoscorePage.method.meaning.fallback'
    )
    expect(wrapper.find('[data-test="disclaimer"]').exists()).toBe(true)
    const text = wrapper.text()
    for (const v1Text of [
      'imputed',
      'Raise',
      'Manufacturing emissions are not measured.',
      'Ranking is still useful.',
    ]) {
      expect(text).not.toContain(v1Text)
    }
  })

  it('states the number of rated and compared products', () => {
    const wrapper = mountMethod(stats, v2Config)
    expect(wrapper.get('[data-test="rated-count"]').text()).toBe(
      'category.ecoscorePage.method.products.ratedCompared {"count":"100","items":"televisions"}'
    )
    expect(wrapper.get('[data-test="counts"]').text()).not.toContain(
      'products.rated '
    )
  })

  it('labels the stored audit sources as the historical V1 ones', () => {
    const wrapper = mountMethod(stats, v2Config)
    expect(wrapper.find('[data-test="sources-v1"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('EU energy label')
  })

  it('uses the labels of the criteria for the coverage', () => {
    const coverage = mountMethod(
      {
        ...stats,
        coverageByCriterion: { POWER_DENSITY: 80, REPAIRABILITY: 40 },
      },
      v2Config
    ).findAll('[data-test="coverage"] li')
    expect(coverage[0]!.text()).toContain('Energy per screen area')
    expect(coverage[0]!.text()).toContain('80%')
  })
})
