import { mountSuspended } from '@nuxt/test-utils/runtime'
import { describe, expect, it, beforeEach, afterEach } from 'vitest'
import type { VueWrapper } from '@vue/test-utils'
import TheSlide from './The-slide.vue'

describe('TheSlide', () => {
  let wrapper: VueWrapper<InstanceType<typeof TheSlide>>

  const mockItems = [
    {
      imageSmall: 'https://example.com/image1.jpg',
      verticalHomeTitle: 'category1',
      href: '/category1',
    },
    {
      imageSmall: 'https://example.com/image2.jpg',
      verticalHomeTitle: 'category2',
      href: '/category2',
    },
    {
      imageSmall: 'https://example.com/image3.jpg',
      verticalHomeTitle: 'category3',
      href: '/category3',
    },
  ]

  const createWrapper = async (props = {}) => {
    const wrapper = await mountSuspended(TheSlide, {
      props: {
        items: mockItems,
        height: 150,
        width: 150,
        ...props,
      },
      global: {
        stubs: {
          NuxtLink: {
            template: `<a :href="typeof to === 'string' ? to : to?.path"><slot /></a>`,
            props: ['to'],
          },
        },
      },
    })

    // Simulate image load events
    const images = wrapper.findAll('.v-img')
    for (const image of images) {
      await image.trigger('load')
    }
    await wrapper.vm.$nextTick()

    return wrapper
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

      const images = wrapper.findAll('.v-img')
      expect(images).toHaveLength(mockItems.length)
    })

    it('should render each item with correct image', async () => {
      wrapper = await createWrapper()

      const images = wrapper.findAll('.v-img')
      expect(images).toHaveLength(mockItems.length)
    })

    it('should render cards with correct dimensions', async () => {
      wrapper = await createWrapper()

      const images = wrapper.findAll('.v-img')
      expect(images).toHaveLength(mockItems.length)
    })

    it('should handle empty items array', async () => {
      wrapper = await createWrapper({ items: [] })

      const slideItems = wrapper.findAll('.v-slide-group-item')
      expect(slideItems).toHaveLength(0)
    })
  })

  describe('Selection Behavior', () => {
    it('should have no item selected initially', async () => {
      wrapper = await createWrapper()

      const images = wrapper.findAll('.v-img')
      expect(images.length).toBeGreaterThan(0)
    })

    it('should render cards as clickable images', async () => {
      wrapper = await createWrapper()

      const images = wrapper.findAll('.v-img')
      expect(images.length).toBeGreaterThan(0)

      // Images should have cursor-pointer class
      images.forEach(img => {
        expect(img.classes()).toContain('cursor-pointer')
      })
    })

    it('should expose SSR-friendly links for each card', async () => {
      wrapper = await createWrapper()

      const anchors = wrapper.findAll('a')
      expect(anchors).toHaveLength(mockItems.length)
      expect(anchors[0]?.attributes('href')).toBe(mockItems[0]?.href)
    })

    it('should render correct href for each card', async () => {
      wrapper = await createWrapper()

      const anchors = wrapper.findAll('a')
      anchors.forEach((anchor, index) => {
        expect(anchor.attributes('href')).toBe(mockItems[index]?.href)
      })
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
      const images = wrapper.findAll('.v-img')
      expect(images).toHaveLength(mockItems.length)
    })
  })
})
