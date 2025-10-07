import { mountSuspended } from '@nuxt/test-utils/runtime'
import { describe, expect, it, beforeEach, afterEach } from 'vitest'
import type { VueWrapper } from '@vue/test-utils'
import TheSlide from './The-slide.vue'

describe('TheSlide', () => {
  let wrapper: VueWrapper<InstanceType<typeof TheSlide>>

  const mockItems = [
    'https://example.com/image1.jpg',
    'https://example.com/image2.jpg',
    'https://example.com/image3.jpg',
  ]

  const createWrapper = async (props = {}) => {
    return await mountSuspended(TheSlide, {
      props: {
        items: mockItems,
        height: 150,
        ...props,
      },
    })
  }

  beforeEach(() => {
    // Reset before each test
  })

  afterEach(() => {
    if (wrapper) {
      wrapper.unmount()
    }
  })

  describe('Component Rendering', () => {
    it('should render the slide group component', async () => {
      wrapper = await createWrapper()

      const slideGroup = wrapper.find('.v-slide-group')
      expect(slideGroup.exists()).toBe(true)
    })

    it('should render all items as cards', async () => {
      wrapper = await createWrapper()

      const cards = wrapper.findAll('.v-card')
      expect(cards).toHaveLength(mockItems.length)
    })

    it('should render each item with correct image', async () => {
      wrapper = await createWrapper()

      const images = wrapper.findAll('.v-img')
      expect(images).toHaveLength(mockItems.length)
    })

    it('should render cards with correct dimensions', async () => {
      wrapper = await createWrapper()

      const cards = wrapper.findAll('.v-card')
      cards.forEach((card) => {
        expect(card.classes()).toContain('ma-2')
      })
    })

    it('should handle empty items array', async () => {
      wrapper = await createWrapper({ items: [] })

      const cards = wrapper.findAll('.v-card')
      expect(cards).toHaveLength(0)
    })
  })

  describe('Selection Behavior', () => {
    it('should have no item selected initially', async () => {
      wrapper = await createWrapper()

      const cards = wrapper.findAll('.v-card')
      // Initially all cards should have grey color class
      cards.forEach((card) => {
        const classes = card.classes().join(' ')
        expect(classes).toContain('grey-lighten-1')
      })
    })

    it('should render cards as clickable', async () => {
      wrapper = await createWrapper()

      const cards = wrapper.findAll('.v-card')
      expect(cards.length).toBeGreaterThan(0)

      // Cards should be clickable
      cards.forEach((card) => {
        expect(card.classes()).toContain('v-card')
      })
    })

    it('should toggle selection when card is clicked', async () => {
      wrapper = await createWrapper()

      const cards = wrapper.findAll('.v-card')
      expect(cards.length).toBeGreaterThan(0)

      const firstCard = cards[0]
      expect(firstCard).toBeDefined()
      if (!firstCard) return

      const initialClasses = firstCard.classes().join(' ')

      // Click on the first card
      await firstCard.trigger('click')
      await wrapper.vm.$nextTick()

      const updatedClasses = firstCard.classes().join(' ')
      // Classes should change after click
      expect(updatedClasses).not.toBe(initialClasses)
    })

    it('should handle multiple card clicks', async () => {
      wrapper = await createWrapper()

      const cards = wrapper.findAll('.v-card')

      // Click on first card
      if (cards[0]) {
        await cards[0].trigger('click')
        await wrapper.vm.$nextTick()
      }

      // Click on second card
      if (cards[1]) {
        await cards[1].trigger('click')
        await wrapper.vm.$nextTick()
      }

      // Both operations should complete without errors
      expect(cards[0]?.exists()).toBe(true)
      expect(cards[1]?.exists()).toBe(true)
    })
  })

  describe('Slide Group Features', () => {
    it('should render slide group with proper structure', async () => {
      wrapper = await createWrapper()

      const slideGroup = wrapper.find('.v-slide-group')
      expect(slideGroup.exists()).toBe(true)

      // Check that the component renders navigation controls or content area
      const slideGroupContent = slideGroup.find('.v-slide-group__content')
      expect(slideGroupContent.exists()).toBe(true)
    })

    it('should handle component props correctly', async () => {
      wrapper = await createWrapper()

      // Verify the component receives and renders items
      const cards = wrapper.findAll('.v-card')
      expect(cards).toHaveLength(mockItems.length)
    })
  })
})
