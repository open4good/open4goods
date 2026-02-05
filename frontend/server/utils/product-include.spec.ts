import { describe, expect, it } from 'vitest'
import { ProductIncludeEnum } from '~~/shared/api-client/apis/ProductApi'
import { parseProductIncludes } from './product-include'

describe('parseProductIncludes', () => {
  it('parses csv include values', () => {
    expect(parseProductIncludes('base,attributes,scores')).toEqual([
      ProductIncludeEnum.Base,
      ProductIncludeEnum.Attributes,
      ProductIncludeEnum.Scores,
    ])
  })

  it('parses array values and filters invalid entries', () => {
    expect(
      parseProductIncludes(['base,attributes', 'invalid', ProductIncludeEnum.Timeline])
    ).toEqual([
      ProductIncludeEnum.Base,
      ProductIncludeEnum.Attributes,
      ProductIncludeEnum.Timeline,
    ])
  })

  it('returns an empty list for unsupported values', () => {
    expect(parseProductIncludes(undefined)).toEqual([])
    expect(parseProductIncludes(['invalid'])).toEqual([])
  })
})
