import { mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import { defineComponent, h } from 'vue'

import CategoryImpactMethod from './CategoryImpactMethod.vue'
import type {
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

vi.stubGlobal('useLocalePath', () => (path: string) => path)

const config: ImpactScoreConfigDto = {
  methodologyVersion: 2,
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
  outdatedCount: 0,
  excludedCount: 0,
  coverageByCriterion: { POWER: 90, REPAIRABILITY: 40 },
  realCriteriaCountDistribution: { 1: 60, 2: 40 },
  scoreHistogram: [0, 5, 20, 50, 25],
  scoreBandWidth: 2,
  scoreMin: 2.1,
  scoreMedian: 6.4,
  scoreMax: 9.8,
  lastScoredAt: Date.UTC(2026, 10, 8),
}

const mountMethod = (statsValue: ImpactScoreStatsDto | null) =>
  mount(CategoryImpactMethod, {
    props: {
      config,
      stats: statsValue,
      categoryLabel: 'Televisions',
      criteriaLabels: { POWER: 'Energy', REPAIRABILITY: 'Repairability' },
      idPrefix: 'method',
    },
    global: {
      stubs: {
        NuxtLink: defineComponent({
          setup(_p, { slots }) {
            return () => h('a', slots.default?.())
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

  it('does not invent figures when stats are missing', () => {
    const wrapper = mountMethod(null)
    expect(wrapper.find('[data-test="stats-unavailable"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="coverage"]').exists()).toBe(false)
    expect(wrapper.find('[data-test="distribution"]').exists()).toBe(false)
  })
})
