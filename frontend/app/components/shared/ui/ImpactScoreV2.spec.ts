import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ImpactScore from './ImpactScore.vue'
import ImpactScoreNotRated from './ImpactScoreNotRated.vue'
import enUS from '~~/i18n/locales/en-US.json'
import {
  isImpactScoreNotRated,
  resolveBetterThanPercent,
  resolveImpactScoreV2,
  resolvePositionLevel,
} from '~/utils/_impact-score-v2'
import type { ProductImpactScoreStatusDto } from '~~/shared/api-client'

const i18n = createI18n({
  legacy: false,
  locale: 'en',
  messages: { en: enUS as Record<string, unknown> },
})

const global = {
  plugins: [i18n],
  stubs: {
    'v-btn': true,
    'v-icon': true,
    'v-tooltip': true,
    CtaCard: true,
    NuxtLink: { template: '<a><slot /></a>' },
  },
}

const rated: ProductImpactScoreStatusDto = {
  status: 'RATED',
  currentMethodologyVersion: 2,
  methodologyVersion: 2,
  scoredAt: Date.UTC(2026, 10, 8, 12),
  realCriteriaCount: 11,
  totalCriteriaCount: 13,
  ratedCount: 412,
  betterThanPercent: 97,
  level: 'excellent',
}

describe('impact score V2 helpers', () => {
  it('ignores impactScore when the vertical is in V1', () => {
    expect(
      resolveImpactScoreV2({
        impactScore: { ...rated, currentMethodologyVersion: 1 },
      })
    ).toBeNull()
    expect(resolveImpactScoreV2({})).toBeNull()
    expect(resolveImpactScoreV2({ impactScore: rated })).toEqual(rated)
  })

  it('detects not rated and unknown levels', () => {
    expect(isImpactScoreNotRated({ status: 'NOT_RATED' })).toBe(true)
    expect(isImpactScoreNotRated(rated)).toBe(false)
    expect(resolvePositionLevel('fair')).toBe('fair')
    expect(resolvePositionLevel('insufficient')).toBeNull()
    expect(resolvePositionLevel(null)).toBeNull()
  })

  it('gives no percentage when only one product is rated', () => {
    expect(resolveBetterThanPercent({ ...rated, ratedCount: 1 })).toBeNull()
    expect(resolveBetterThanPercent(rated)).toBe(97)
  })
})

describe('ImpactScore V2 rendering', () => {
  it('shows position label and context line, without leaf in the banner', () => {
    const wrapper = mount(ImpactScore, {
      props: { score: 14.2, banner: true, v2: rated },
      global,
    })
    const text = wrapper.text()
    expect(text).toContain('Among the best ranked')
    expect(text).not.toContain('Excellent')
    expect(text).toContain('97% of the 412 products rated by Nudger')
    expect(text).toContain('11 of 13 criteria filled in')
    expect(text).toContain('computed on Nov 8, 2026')
    expect(text).toContain('method v2')
    expect(text).toContain('How is this score calculated?')
    expect(wrapper.findAll('v-icon-stub')).toHaveLength(0)
  })

  it.each([
    ['excellent', 'Among the best ranked'],
    ['good', 'Above the median'],
    ['fair', 'Below the median'],
    ['poor', 'Among the lowest ranked'],
  ])('labels level %s from the position', (level, label) => {
    const wrapper = mount(ImpactScore, {
      props: { score: 3, v2: { ...rated, level } },
      global,
    })
    expect(wrapper.text()).toContain(label)
  })

  it('shows no level and no percentage when a single product is rated', () => {
    const wrapper = mount(ImpactScore, {
      props: {
        score: 10,
        v2: { ...rated, ratedCount: 1, level: null, betterThanPercent: 0 },
      },
      global,
    })
    expect(wrapper.text()).toContain('Only rated product in its category')
    expect(wrapper.text()).not.toContain('Weak')
    expect(wrapper.text()).not.toMatch(/\d+% of the/)
  })

  it('names the vertical in the position sentence, with a products fallback', () => {
    const named = mount(ImpactScore, {
      props: { score: 14.2, v2: rated, verticalPluralName: 'téléviseurs' },
      global,
    })
    expect(named.text()).toContain('of the 412 téléviseurs rated by Nudger')
    const fallback = mount(ImpactScore, {
      props: { score: 14.2, v2: rated },
      global,
    })
    expect(fallback.text()).toContain('of the 412 products rated by Nudger')
  })

  it('keeps the V1 rendering without v2 block', () => {
    const wrapper = mount(ImpactScore, {
      props: { score: 15, banner: true },
      global,
    })
    expect(wrapper.find('.impact-score-panel__context').exists()).toBe(false)
    expect(wrapper.text()).toContain('Good')
    expect(wrapper.findAll('v-icon-stub')).toHaveLength(1)
  })
})

describe('ImpactScoreNotRated', () => {
  it.each(['LOW_COVERAGE', 'NO_REAL_CRITERIA'] as const)(
    'explains %s with the criteria count',
    reason => {
      const wrapper = mount(ImpactScoreNotRated, {
        props: {
          status: {
            status: 'NOT_RATED',
            notRatedReason: reason,
            realCriteriaCount: 3,
            totalCriteriaCount: 13,
          },
        },
        global,
      })
      expect(wrapper.text()).toContain('Not rated')
      expect(wrapper.text()).toContain('3 of 13 criteria filled in')
      expect(wrapper.text()).not.toContain('Weak')
      expect(wrapper.find('.impact-score-panel__bar').exists()).toBe(false)
    }
  )

  it.each(['OUTDATED_METHOD', 'NOT_SCORED'] as const)(
    'explains %s as a recompute',
    reason => {
      const wrapper = mount(ImpactScoreNotRated, {
        props: { status: { status: 'NOT_RATED', notRatedReason: reason } },
        global,
      })
      expect(wrapper.text()).toContain('Score being recomputed with method v2.')
    }
  )
})
