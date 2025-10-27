import { flushPromises } from '@vue/test-utils'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createI18n } from 'vue-i18n'
import { defineComponent, h, onMounted, type PropType } from 'vue'
import type { ProductDto } from '~~/shared/api-client'
import ProductHeroGallery from './ProductHeroGallery.vue'

vi.mock('#imports', () => ({
  useImage: () => (src: string) => src,
}))

const NuxtImgStub = defineComponent({
  name: 'NuxtImgStub',
  props: {
    src: { type: String, default: '' },
    alt: { type: String, default: '' },
  },
  setup(props, { attrs }) {
    return () => h('img', { ...attrs, src: props.src, alt: props.alt })
  },
})

const mocks = vi.hoisted(() => ({
  lightGalleryOpenSpy: vi.fn(),
}))

vi.mock('lightgallery/vue', () => {
  const LightGalleryStub = defineComponent({
    name: 'LightGalleryStub',
    props: {
      dynamicEl: {
        type: Array as PropType<unknown[]>,
        default: () => [],
      },
    },
    emits: ['onInit', 'onAfterSlide', 'onBeforeSlide', 'onBeforeClose'],
    setup(props, { emit }) {
      onMounted(() => {
        emit('onInit', { instance: { openGallery: mocks.lightGalleryOpenSpy } })
      })

      return () =>
        h(
          'div',
          { class: 'lightgallery-stub' },
          h(
            'ul',
            { class: 'lightgallery-items' },
            (props.dynamicEl as unknown[]).map((item, index) =>
              h('li', {
                class: 'lightgallery-item',
                'data-index': index,
                'data-type': typeof (item as Record<string, unknown>).video === 'object' ? 'video' : 'image',
              }),
            ),
          ),
        )
    },
  })

  return { LightGallery: LightGalleryStub }
})

vi.mock('lightgallery/plugins/zoom', () => ({ default: () => ({}) }))
vi.mock('lightgallery/plugins/fullscreen', () => ({ default: () => ({}) }))
vi.mock('lightgallery/plugins/thumbnail', () => ({ default: () => ({}) }))
vi.mock('lightgallery/plugins/video', () => ({ default: () => ({}) }))

vi.mock('lightgallery/css/lightgallery.css', () => ({}), { virtual: true })
vi.mock('lightgallery/css/lg-zoom.css', () => ({}), { virtual: true })
vi.mock('lightgallery/css/lg-fullscreen.css', () => ({}), { virtual: true })
vi.mock('lightgallery/css/lg-thumbnail.css', () => ({}), { virtual: true })
vi.mock('lightgallery/css/lg-video.css', () => ({}), { virtual: true })

const i18n = createI18n({
  legacy: false,
  locale: 'en-US',
  messages: {
    'en-US': {
      product: {
        hero: {
          openGalleryImage: 'View image "{label}" in fullscreen',
          openGalleryVideo: 'Play video "{label}"',
          openGalleryFallback: 'Open product media gallery',
          thumbnailLabel: 'Select {type} {index} of {total}: {label}',
          mediaType: {
            image: 'image',
            video: 'video',
          },
          videoBadge: 'Video',
        },
      },
    },
  },
})

describe('ProductHeroGallery', () => {
  beforeEach(() => {
    mocks.lightGalleryOpenSpy.mockReset()
  })

  const baseProduct: ProductDto = {
    gtin: '0000000000000',
    names: { h1Title: 'Demo Product' },
    identity: { brand: 'BrandCo', model: 'Model X', bestName: 'Demo Product' },
    base: {
      bestName: 'Demo Product',
      coverImagePath: '/images/cover.jpg',
    },
    resources: {
      images: [
        {
          url: '/images/hero-1.jpg',
          originalUrl: '/images/hero-1-large.jpg',
          width: 1200,
          height: 900,
          fileName: 'Front view',
          datasourceName: 'Source A',
        },
        {
          url: '/images/hero-2.jpg',
          originalUrl: '/images/hero-2-large.jpg',
          width: 1200,
          height: 900,
          fileName: 'Side view',
          datasourceName: 'Source B',
        },
      ],
      videos: [],
    },
  } as unknown as ProductDto

  const productWithVideo: ProductDto = {
    ...baseProduct,
    resources: {
      ...baseProduct.resources,
      videos: [
        {
          url: '/videos/hero.mp4',
          datasourceName: 'Source Video',
        },
      ],
    },
  }

  const mountComponent = async (product: ProductDto = baseProduct) => {
    const wrapper = await mountSuspended(ProductHeroGallery, {
      props: {
        product,
        title: 'Demo Product',
      },
      global: {
        plugins: [i18n],
        stubs: {
          NuxtImg: NuxtImgStub,
        },
      },
    })

    await flushPromises()
    return wrapper
  }

  it('renders hero media and opens the lightbox from the stage', async () => {
    const wrapper = await mountComponent()
    const stage = wrapper.get('[data-testid="product-gallery-stage"]')

    expect(stage.attributes('aria-label')).toContain('Source A')

    await stage.trigger('click')
    expect(mocks.lightGalleryOpenSpy).toHaveBeenCalledWith(0)
  })

  it('updates the hero image when a thumbnail is clicked', async () => {
    const wrapper = await mountComponent()
    const thumbnails = wrapper.findAll('[data-testid="product-gallery-thumbnail"]')

    await thumbnails[1].trigger('click')
    expect(wrapper.get('[data-testid="product-gallery-stage"]').attributes('aria-label')).toContain('Source B')

    await thumbnails[1].trigger('dblclick')
    expect(mocks.lightGalleryOpenSpy).toHaveBeenCalledWith(1)
  })

  it('syncs hero media with lightbox navigation', async () => {
    const wrapper = await mountComponent()
    const gallery = wrapper.getComponent({ name: 'LightGalleryStub' })

    gallery.vm.$emit('onAfterSlide', { index: 1 })
    await wrapper.vm.$nextTick()

    expect(wrapper.get('[data-testid="product-gallery-stage"]').attributes('aria-label')).toContain('Source B')
  })

  it('provides video metadata to lightgallery items', async () => {
    const wrapper = await mountComponent(productWithVideo)
    const dynamicItems = wrapper.getComponent({ name: 'LightGalleryStub' }).props('dynamicEl') as Array<Record<string, unknown>>

    expect(dynamicItems[0]?.video).toMatchObject({
      source: [
        {
          src: '/videos/hero.mp4',
        },
      ],
    })
  })
})
