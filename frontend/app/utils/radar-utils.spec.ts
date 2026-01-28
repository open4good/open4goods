import { describe, it, expect } from 'vitest'
import { transformRadarValue, type RadarInversionContext } from './radar-utils'
import type { AttributeConfigDto } from '~~/shared/api-client'

describe('radar-utils', () => {
  const createConfigMap = (
    configs: Record<string, 'LOWER' | 'GREATER' | undefined>
  ): Map<string, AttributeConfigDto> => {
    const map = new Map<string, AttributeConfigDto>()
    Object.entries(configs).forEach(([key, impactBetterIs]) => {
      // @ts-expect-error - partial mock
      map.set(key, { impactBetterIs } as AttributeConfigDto)
    })
    return map
  }

  it('preserves null values', () => {
    const context: RadarInversionContext = {
      axisId: 'TEST',
      productValue: 10,
      bestValue: 5,
      worstValue: 20,
    }
    const map = createConfigMap({ TEST: 'LOWER' })
    expect(transformRadarValue(null, context, map)).toBeNull()
  })

  it('preserves values when impactBetterIs is explicitly GREATER', () => {
    const context: RadarInversionContext = {
      axisId: 'TEST',
      productValue: 10,
      bestValue: 20,
      worstValue: 5,
    }
    const map = createConfigMap({ TEST: 'GREATER' })
    // No inversion, should remain 10
    expect(transformRadarValue(10, context, map)).toBe(10)
  })

  it('preserves values when impactBetterIs is undefined (default)', () => {
    const context: RadarInversionContext = {
      axisId: 'TEST',
      productValue: 10,
      bestValue: 20,
      worstValue: 5,
    }
    const map = createConfigMap({})
    // No inversion, should remain 10
    expect(transformRadarValue(10, context, map)).toBe(10)
  })

  it('inverts values correctly when impactBetterIs is LOWER', () => {
    // Scenario: Power consumption (Lower is better)
    // Product: 100
    // Best: 50
    // Worst: 200

    const context: RadarInversionContext = {
      axisId: 'POWER',
      productValue: 100,
      bestValue: 50,
      worstValue: 200,
    }
    const map = createConfigMap({ POWER: 'LOWER' })

    // Validating max logic
    // Max observed = 200
    // Padded max = 200 * 1.1 = 220

    // Product: 220 - 100 = 120
    // Best: 220 - 50 = 170 (Should be big, near edge)
    // Worst: 220 - 200 = 20 (Should be small, near center)

    expect(transformRadarValue(100, context, map)).toBeCloseTo(120)
    expect(transformRadarValue(50, context, map)).toBeCloseTo(170)
    expect(transformRadarValue(200, context, map)).toBeCloseTo(20)
  })

  it('handles negative values with inversion', () => {
    // Not typical for physical attributes but good to test robustness
    const context: RadarInversionContext = {
      axisId: 'NEG',
      productValue: -10,
      bestValue: -20,
      worstValue: -5,
    }
    const map = createConfigMap({ NEG: 'LOWER' })

    // Max observed = -5
    // Padded max = 5 (fallback since maxObserved <= 0? no)
    // Code says: const paddedMax = maxObserved > 0 ? maxObserved * 1.1 : 5
    // Since -5 is not > 0, paddedMax is 5.

    // Product: 5 - (-10) = 15
    expect(transformRadarValue(-10, context, map)).toBe(15)
  })

  it('handles case where map is undefined', () => {
    const context: RadarInversionContext = {
      axisId: 'TEST',
      productValue: 10,
      bestValue: 5,
      worstValue: 20,
    }
    expect(transformRadarValue(10, context, undefined)).toBe(10)
  })
})
