import { mountSuspended } from '@nuxt/test-utils/runtime'
import { describe, expect, test } from 'vitest'
import CategoryCarousel from './CategoryCarousel.vue'

describe('CategoryCarousel', () => {
  test('renders all categories', async () => {
    const wrapper = await mountSuspended(CategoryCarousel)
    const items = wrapper.findAll('.category-item')
    expect(items).toHaveLength(3)
    expect(items[0].text()).toContain('Refrigerators')
  })
})
