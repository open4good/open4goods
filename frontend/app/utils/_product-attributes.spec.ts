import { describe, expect, it } from 'vitest'
import {
  appendAttributeUnit,
  formatAttributeValue,
  resolveAttributeUnit,
} from './_product-attributes'

const t = (key: string) => key
const n = (value: number) => value.toString()

describe('formatAttributeValue', () => {
  it('applies attribute mappings for string values', () => {
    const value = formatAttributeValue(
      {
        key: 'DISPLAY_TECHNOLOGY',
        label: 'Display technology',
        rawValue: 'lcd',
        mappings: { lcd: 'LCD' },
      },
      t,
      n
    )

    expect(value).toBe('LCD')
  })

  it('applies attribute mappings for array values', () => {
    const value = formatAttributeValue(
      {
        key: 'DISPLAY_TECHNOLOGY',
        label: 'Display technology',
        rawValue: ['lcd', 'oled'],
        mappings: { lcd: 'LCD', oled: 'OLED' },
      },
      t,
      n
    )

    expect(value).toBe('LCD, OLED')
  })
})

describe('appendAttributeUnit', () => {
  const frenchNumber = (value: number) =>
    new Intl.NumberFormat('fr-FR').format(value)

  it('renders "60 cm" with a no-break space before the unit', () => {
    expect(appendAttributeUnit('60', 'cm', frenchNumber)).toBe('60\u00a0cm')
  })

  it('formats an indexed decimal with the French separator', () => {
    expect(appendAttributeUnit('0.6', 'cm', frenchNumber)).toBe('0,6\u00a0cm')
    expect(appendAttributeUnit('59.5', 'cm', frenchNumber)).toBe('59,5\u00a0cm')
  })

  it('does not repeat a unit already present in the value', () => {
    expect(appendAttributeUnit('45 cm', 'cm', frenchNumber)).toBe('45 cm')
  })

  it('leaves a value without configured unit unchanged', () => {
    expect(appendAttributeUnit('2023', null, frenchNumber)).toBe('2023')
  })
})

describe('resolveAttributeUnit', () => {
  it('prefers the unit, then the suffix', () => {
    expect(resolveAttributeUnit({ unit: 'cm', suffix: 'ignored' })).toBe('cm')
    expect(resolveAttributeUnit({ suffix: 'an(s)' })).toBe('an(s)')
    expect(resolveAttributeUnit({ unit: ' ', suffix: '' })).toBeNull()
  })
})

describe('formatAttributeValue units', () => {
  it('appends the suffix after a no-break space', () => {
    const value = formatAttributeValue(
      { key: 'WEIGHT', label: 'Poids', rawValue: '68', suffix: 'kg' },
      t,
      n
    )

    expect(value).toBe('68\u00a0kg')
  })
})
