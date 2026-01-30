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
    // Formula: (Value - Min) / (Max - Min) * 100
    // (10 - 5) / (20 - 5) = 5 / 15 = 1/3 ~ 33.33
    expect(transformRadarValue(10, context, map)).toBeCloseTo(33.33)
  })

  it('preserves values when impactBetterIs is undefined (default)', () => {
    const context: RadarInversionContext = {
      axisId: 'TEST',
      productValue: 10,
      bestValue: 20,
      worstValue: 5,
    }
    const map = createConfigMap({})
    // Formula: (Value - Min) / (Max - Min) * 100
    // (10 - 5) / (20 - 5) = 5 / 15 = 1/3 ~ 33.33
    expect(transformRadarValue(10, context, map)).toBeCloseTo(33.33)
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

    // Formula: (Max - Value) / (Max - Min) * 100
    // Min observed = 50, Max observed = 200

    // Product: 100 -> (200 - 100) / (200 - 50) = 100 / 150 = 2/3 ~ 66.67
    expect(transformRadarValue(100, context, map)).toBeCloseTo(66.67)

    // Best: 50 -> (200 - 50) / (200 - 50) = 150 / 150 = 1 = 100
    expect(transformRadarValue(50, context, map)).toBeCloseTo(100)

    // Worst: 200 -> (200 - 200) / (200 - 50) = 0
    expect(transformRadarValue(200, context, map)).toBeCloseTo(0)
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

    // Min: -20, Max: -5
    // Formula: (Max - Value) / (Max - Min) * 100
    // (-5 - (-10)) / (-5 - (-20)) = 5 / 15 = 1/3 ~ 33.33
    expect(transformRadarValue(-10, context, map)).toBeCloseTo(33.33)
  })

  it('handles case where map is undefined', () => {
    const context: RadarInversionContext = {
      axisId: 'TEST',
      productValue: 10,
      bestValue: 5,
      worstValue: 20,
    }
    // (10 - 5) / (20 - 5) = 5 / 15 = 1/3 ~ 33.33
    expect(transformRadarValue(10, context, undefined)).toBeCloseTo(33.33)
  })
})
