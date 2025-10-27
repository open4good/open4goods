import { mountSuspended } from '@nuxt/test-utils/runtime'
import { beforeEach, afterEach, describe, expect, it, vi } from 'vitest'
import { createI18n } from 'vue-i18n'
import { defineComponent, h, onMounted, ref, type PropType } from 'vue'
import type { ProductDto } from '~~/shared/api-client'
import ProductHeroGallery from './ProductHeroGallery.vue'

const openLightboxMock = vi.fn()

vi.mock('#imports', () => ({
  useImage: () => (src: string) => src,
}))

vi.mock('vue3-picture-swipe', () => {
  const VuePictureSwipeStub = defineComponent({
    name: 'VuePictureSwipeStub',
    props: {
      items: {
        type: Array as PropType<Array<Record<string, unknown>>>,
        default: () => [],
      },
      options: {
        type: Object as PropType<Record<string, unknown>>,
        default: () => ({}),
      },
    },
    setup(props) {
      const root = ref<HTMLElement | null>(null)

      onMounted(() => {
        window.setTimeout(() => {
          const gallery = root.value?.querySelector('.my-gallery') ?? null
          if (!gallery) {
            return
          }

          gallery.onclick = (event: Event) => {
            event.preventDefault?.()
            const figures = Array.from(gallery.querySelectorAll('figure'))
            const target = event.target as Node | null
            const index = figures.findIndex((figure) =>
              target ? figure.contains(target) : false,
            )
            openLightboxMock(index)
          }
        }, 40)
      })

      return () =>
        h('div', { ref: root, class: 'vue-picture-swipe-stub' }, [
          h(
            'div',
            { class: 'my-gallery' },
            (props.items as Array<Record<string, unknown>>).map((item, index) =>
              h('figure', { class: 'gallery-thumbnail', 'data-index': index }, [
                h(
                  'a',
                  { href: (item.src as string | undefined) ?? '', title: (item.title as string | undefined) ?? '' },
                  [
                    h('img', {
                      src: (item.thumbnail as string | undefined) ?? '',
                      alt: (item.alt as string | undefined) ?? '',
                    }),
                  ],
                ),
              ]),
            ),
          ),
        ])
    },
  })

  return { default: VuePictureSwipeStub }
})

const createProduct = (): ProductDto => ({
  gtin: '0000000000000',
  names: { h1Title: 'Demo Product' },
  identity: { brand: 'BrandCo', bestName: 'Demo Product', model: 'Model A' },
  base: {
    bestName: 'Demo Product',
    coverImagePath: '/images/product-cover.jpg',
  },
  offers: {
    offersCount: 0,
  },
  resources: {
    images: [
      {
        url: '/images/product-1.jpg',
        originalUrl: '/images/product-1-large.jpg',
        width: 1600,
        height: 1200,
        thumbnailWidth: 400,
        thumbnailHeight: 400,
        fileName: 'Front view',
        datasourceName: 'Source A',
        cacheKey: 'image-1',
      },
    ],
    videos: [],
  },
})

const i18n = createI18n({
  legacy: false,
  locale: 'en',
  messages: {
    en: {
      product: {
        hero: {
          openGalleryImage: 'Open image "{label}"',
          openGalleryVideo: 'Play video "{label}"',
          openGalleryFallback: 'Open gallery',
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

const NuxtImgStub = defineComponent({
  name: 'NuxtImgStub',
  props: {
    src: { type: String, default: '' },
    alt: { type: String, default: '' },
  },
  setup(props) {
    return () => h('img', { class: 'nuxt-img-stub', src: props.src, alt: props.alt })
  },
})

const ClientOnlyStub = defineComponent({
  name: 'ClientOnlyStub',
  setup(_, { slots }) {
    return () => slots.default?.()
  },
})

const VIconStub = defineComponent({
  name: 'VIconStub',
  props: {
    icon: { type: String, default: '' },
    size: { type: [Number, String], default: 24 },
  },
  setup(props) {
    return () => h('span', { class: 'v-icon-stub', 'data-icon': props.icon, 'data-size': props.size })
  },
})

describe('ProductHeroGallery', () => {
  beforeEach(() => {
    openLightboxMock.mockClear()
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('opens the lightbox when clicking the hero stage even if bindings are delayed', async () => {
    const product = createProduct()

    const wrapper = await mountSuspended(ProductHeroGallery, {
      props: { product, title: 'Demo Product' },
      global: {
        plugins: [i18n],
        stubs: {
          NuxtImg: NuxtImgStub,
          ClientOnly: ClientOnlyStub,
          'v-icon': VIconStub,
        },
      },
    })

    const stage = wrapper.get('[data-testid="product-gallery-stage"]')

    await stage.trigger('click')

    await vi.runAllTimersAsync()

    expect(openLightboxMock).toHaveBeenCalledWith(0)
  })
})
