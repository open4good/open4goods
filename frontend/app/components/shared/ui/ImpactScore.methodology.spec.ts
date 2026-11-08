import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import { createI18n } from 'vue-i18n'
import ImpactScore from './ImpactScore.vue'

const i18n = createI18n({
  legacy: false,
  locale: 'en-US',
  missingWarn: false,
  messages: {
    'en-US': { product: { impact: { methodologyShort: 'Methodology' } } },
  },
})

const mountScore = (props: Record<string, unknown> = {}) =>
  mount(ImpactScore, {
    props: { score: 12, max: 20, ...props },
    global: {
      plugins: [i18n],
      stubs: {
        'v-icon': true,
        CtaCard: true,
        NuxtLink: true,
        'v-btn': { props: ['to'], template: '<a :href="to"><slot /></a>' },
      },
    },
  })

describe('ImpactScore methodology link', () => {
  it('points to the vertical ecoscore page', () => {
    const w = mountScore({ verticalHomeUrl: 'climatiseurs' })
    expect(w.find('a').attributes('href')).toBe('/climatiseurs/ecoscore')
  })

  it('falls back to impact-score when the vertical is unknown', () => {
    const w = mountScore({ verticalHomeUrl: '' })
    expect(w.find('a').attributes('href')).toBe('/impact-score')
  })
})
