import { mountSuspended } from '@nuxt/test-utils/runtime'
import { describe, expect, test } from 'vitest'
import StarRating from './StarRating.vue'

describe('StarRating', () => {
  test('renders fractional ratings correctly', async () => {
    const wrapper = await mountSuspended(StarRating, {
      props: { rating: 3.7 },
    })

    expect(wrapper.findAll('.star-full')).toHaveLength(3)
    const partial = wrapper.find('.star-partial .star-front')
    expect(partial.attributes('style')).toContain('width: 70%')
    expect(wrapper.findAll('.star-empty')).toHaveLength(1)
  })
})
