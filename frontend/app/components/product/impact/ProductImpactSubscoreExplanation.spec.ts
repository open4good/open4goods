import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import { createI18n } from 'vue-i18n'
import ProductImpactSubscoreExplanation from './ProductImpactSubscoreExplanation.vue'
import type { ScoreView } from './impact-types'
import enUS from '../../../../../i18n/locales/en-US.json'

describe('ProductImpactSubscoreExplanation', () => {
  const i18n = createI18n({
    legacy: false,
    locale: 'en-US',
    messages: {
      'en-US': enUS,
    },
  })

  // Mock global Nudger objects if needed, or just rely on i18n
  // The component imports 'useI18n' from 'vue-i18n'

  const baseScore: ScoreView = {
    id: 'test-score',
    label: 'Test Score',
    description: 'A test score description',
    value: 3.5,
    relativeValue: 3.5,
    on20: 14,
    betterIs: 'GREATER',
    impactBetterIs: 'GREATER',
    scoring: {
      normalization: {
        method: 'SIGMA',
        params: { sigmaK: 2 },
      },
      scale: { min: 0, max: 5 },
    },
    absolute: {
      min: 1.0,
      max: 5.0,
      avg: 2.5,
      count: 100,
      value: 3.5,
    },
  }

  const defaultProps = {
    score: baseScore,
    absoluteValue: '3.5 kg',
    productName: 'Test Product',
    productBrand: 'Test Brand',
    productModel: 'Test Model',
    verticalTitle: 'Ovens',
  }

  it('renders the explanation title', () => {
    const wrapper = mount(ProductImpactSubscoreExplanation, {
      props: defaultProps,
      global: {
        plugins: [i18n],
      },
    })
    expect(wrapper.text()).toContain('How to read this indicator')
  })

  it('displays the average as a pivot score of 10/20 (Sigma scoring)', () => {
    const wrapper = mount(ProductImpactSubscoreExplanation, {
      props: defaultProps,
      global: {
        plugins: [i18n],
      },
    })

    // Check for the updated text structure
    const text = wrapper.text()
    expect(text).toContain('the average reaches 2.5')
  })

  it('displays the product score on 20 scale using the provided on20 value', () => {
    const wrapper = mount(ProductImpactSubscoreExplanation, {
      props: defaultProps,
      global: {
        plugins: [i18n],
      },
    })

    const text = wrapper.text()
    // Product on 20 is 14
    expect(text).toContain('Test Brand Test Model posts a test score of 3.5')
    expect(text).toContain('converts to 14/20')
  })

  it('displays min and max values without claiming they are 0/20 or 20/20', () => {
    const wrapper = mount(ProductImpactSubscoreExplanation, {
      props: defaultProps,
      global: {
        plugins: [i18n],
      },
    })

    const text = wrapper.text()

    // Worst (Lowest for HIGHER betterIs)
    expect(text).toContain('The lowest test score recorded is 1.')
    expect(text).not.toContain('corresponds to a 0/20 score')

    // Best (Highest for HIGHER betterIs)
    expect(text).toContain(
      'The highest test score observed among the 100 ovens is 5.'
    )
    expect(text).not.toContain('worth 20/20')
  })

  it('handles "betterIs LOWER" correctly (Worst is High, Best is Low)', () => {
    const scoreLower: ScoreView = {
      ...baseScore,
      impactBetterIs: 'LOWER',
      absolute: { ...baseScore.absolute!, min: 10, max: 50, avg: 30 },
    }

    const wrapper = mount(ProductImpactSubscoreExplanation, {
      props: { ...defaultProps, score: scoreLower },
      global: {
        plugins: [i18n],
      },
    })

    const text = wrapper.text()
    // Best (Lowest for LOWER betterIs)
    expect(text).toContain(
      'The lowest test score observed among the 100 ovens is 10.'
    )
  })

  it('displays unit when provided', () => {
    const scoreWithUnit: ScoreView = {
      ...baseScore,
      unit: 'kg',
    }

    const wrapper = mount(ProductImpactSubscoreExplanation, {
      props: { ...defaultProps, score: scoreWithUnit },
      global: {
        plugins: [i18n],
      },
    })

    const text = wrapper.text()
    expect(text).toContain('2.5kg') // Average with unit
    expect(text).toContain('5kg') // Max with unit
  })

  it('displays distribution explanation when stdDev is present', () => {
    const scoreWithSigma: ScoreView = {
      ...baseScore,
      unit: 'W',
      absolute: {
        ...baseScore.absolute,
        stdDev: 1.5,
      },
    }

    const wrapper = mount(ProductImpactSubscoreExplanation, {
      props: { ...defaultProps, score: scoreWithSigma },
      global: {
        plugins: [i18n],
      },
    })

    const text = wrapper.text()
    expect(text).toContain('standard deviation (1.5W)')
  })
})
