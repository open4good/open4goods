import { describe, expect, it } from 'vitest'
import { priceToSliderValue, sliderValueToPrice } from './price-scale'

describe('price-scale conversions', () => {
  it('maps price bounds to slider positions and back', () => {
    const cases: Array<{ price: number; slider: number }> = [
      { price: 0, slider: 0 },
      { price: 500, slider: 100 },
      { price: 1000, slider: 200 },
      { price: 7500, slider: 290 },
    ]

    cases.forEach(({ price, slider }) => {
      expect(priceToSliderValue(price)).toBe(slider)
      expect(sliderValueToPrice(slider)).toBe(price)
    })
  })
})
