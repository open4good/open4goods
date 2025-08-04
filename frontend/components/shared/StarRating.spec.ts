import { mountSuspended } from '@nuxt/test-utils/runtime'
import { describe, expect, it } from 'vitest'
import StarRating from './StarRating.vue'

describe('StarRating', () => {
  it('renders fractional ratings with a half star', async () => {
    const wrapper = await mountSuspended(StarRating, {
      props: { rating: 2.5 },
    })

    expect(wrapper.findAll('.mdi-star')).toHaveLength(2)
    expect(wrapper.find('.mdi-star-half-full').exists()).toBe(true)
    expect(wrapper.findAll('.mdi-star-outline')).toHaveLength(2)
  })
})
