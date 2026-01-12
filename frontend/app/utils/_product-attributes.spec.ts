import { describe, expect, it } from 'vitest'
import { formatAttributeValue } from './_product-attributes'

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
