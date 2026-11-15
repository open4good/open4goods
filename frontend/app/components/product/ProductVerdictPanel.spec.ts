import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ProductVerdictPanel from './ProductVerdictPanel.vue'

const i18n = createI18n({
  legacy: false,
  locale: 'en',
  messages: {
    en: {
      product: {
        verdict: {
          title: 'Verdict',
          dimensions: {
            impact: 'Impact Score',
            price: 'Price',
            reliability: 'Reliability',
          },
          levels: { insufficient: 'Weak', unrated: 'Not rated' },
          priceLevels: { insufficient: 'n/a' },
          vsMedian: 'vs median',
          reliabilityLevels: { good: 'Reliable', insufficient: 'n/a' },
        },
      },
      components: {
        impactScore: {
          v2: {
            reliabilityCriteria: '{score}/{max} - {label} ({criteria})',
            criteria:
              '{count} of {total} criterion | {count} of {total} criteria',
          },
        },
      },
    },
  },
})

describe('ProductVerdictPanel', () => {
  it('shows "Not rated" instead of "Weak" for a product without a score', () => {
    const wrapper = mount(ProductVerdictPanel, {
      props: { product: {} as never },
      global: {
        plugins: [i18n],
        stubs: {
          'v-icon': true,
          ProductVerdictDimensionChip: {
            props: ['title', 'value', 'tooltip'],
            template: '<div class="chip">{{ title }}:{{ value }}</div>',
          },
        },
      },
    })

    expect(wrapper.text()).toContain('Impact Score:Not rated')
    expect(wrapper.text()).not.toContain('Weak')
  })

  const reliabilityTooltip = (impactScore?: Record<string, unknown>) => {
    const wrapper = mount(ProductVerdictPanel, {
      props: {
        product: {
          scores: {
            impactScore,
            scores: {
              DATA_QUALITY: { relativ: { value: 0.8, avg: 0.5 } },
            },
          },
        } as never,
      },
      global: {
        plugins: [i18n],
        stubs: {
          'v-icon': true,
          ProductVerdictDimensionChip: {
            props: ['title', 'tooltip'],
            template: '<div class="chip" :data-tip="tooltip">{{ title }}</div>',
          },
        },
      },
    })
    return wrapper
      .findAll('.chip')
      .find(chip => chip.text() === 'Reliability')
      ?.attributes('data-tip')
  }

  it('adds the criteria counter to the reliability tooltip in V2', () => {
    expect(
      reliabilityTooltip({
        status: 'RATED',
        currentMethodologyVersion: 2,
        realCriteriaCount: 3,
        totalCriteriaCount: 5,
      })
    ).toContain('3 of 5 criteria')
  })

  it('keeps the reliability tooltip unchanged in V1', () => {
    const tip = reliabilityTooltip({
      status: 'RATED',
      currentMethodologyVersion: 1,
      realCriteriaCount: 3,
      totalCriteriaCount: 5,
    })
    expect(tip).toBeDefined()
    expect(tip).not.toContain('criteria')
  })
})
