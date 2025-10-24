import { mountSuspended } from '@nuxt/test-utils/runtime'
import { describe, expect, it, vi } from 'vitest'
import { defineComponent, h, onMounted, ref, type PropType } from 'vue'
import { createI18n } from 'vue-i18n'
import type { ProductDto } from '~~/shared/api-client'
import ProductHero from './ProductHero.vue'

vi.mock('#imports', () => ({
  useImage: () => (src: string) => src,
}))

type StubItem = {
  src?: string
  title?: string
  thumbnail?: string
  alt?: string
  type?: string
}

vi.mock('vue3-picture-swipe', () => {
  const VuePictureSwipeStub = defineComponent({
    name: 'VuePictureSwipeStub',
    props: {
      items: {
        type: Array as PropType<StubItem[]>,
        default: () => [] as StubItem[],
      },
      options: {
        type: Object as PropType<Record<string, unknown>>,
        default: () => ({}),
      },
    },
    setup(props, { expose }) {
      const root = ref<HTMLElement | null>(null)
      const pswp = {
        listen: () => {},
        getCurrentIndex: () => 0,
        container: typeof document !== 'undefined' ? document.createElement('div') : undefined,
      }

      onMounted(() => {
        expose({ $el: root.value, pswp })
      })

      return () =>
        h(
          'div',
          { ref: root, class: 'vue-picture-swipe-stub' },
          (props.items as StubItem[]).map((item, index: number) =>
            h('figure', { class: 'gallery-thumbnail', id: `item-${index}` }, [
              h(
                'a',
                { href: item.src ?? '', title: item.title ?? '' },
                [h('img', { src: item.thumbnail ?? '', alt: item.alt ?? '' })],
              ),
              h(
                'span',
                {
                  class: 'product-gallery__lightbox-caption',
                  'data-type': item.type ?? 'image',
                },
                item.title ?? '',
              ),
            ]),
          ),
        )
    },
  })

  return { default: VuePictureSwipeStub }
})

describe('ProductHero', () => {
  const i18n = createI18n({
    legacy: false,
    locale: 'en-US',
    messages: {
      'en-US': {
        product: {
          hero: {
            gtin: 'GTIN code',
            offersCount: 'Number of offers',
            bestPriceTitle: 'Best price',
            priceMerchantPrefix: 'At',
            viewSingleOffer: 'View the offer',
            viewOffersCount: 'View the {count} offers',
            breadcrumbAriaLabel: 'Product breadcrumb',
            missingBreadcrumbTitle: 'Category',
            gtinTooltip: 'Tooltip text',
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
          price: {
            trend: {
              decrease: 'Price drop of {amount}',
              increase: 'Price increase of {amount}',
              stable: 'Price unchanged',
            },
          },
        },
      },
    },
  })

  const product = {
    gtin: '0123456789012',
    names: { h1Title: 'Demo Product' },
    identity: { brand: 'BrandCo', model: 'Model X', bestName: 'Demo Product' },
    base: {
      ecoscoreValue: 3.5,
      bestName: 'Demo Product',
      gtinInfo: { countryName: 'France', countryFlagUrl: '/flag.png' },
      coverImagePath: '/cover.jpg',
    },
    offers: {
      offersCount: 4,
      bestPrice: {
        price: 199,
        currency: 'EUR',
        datasourceName: 'Shop',
        url: 'https://example.com',
        favicon: 'https://example.com/favicon.ico',
      },
      newTrend: {
        trend: 'PRICE_DECREASE',
        variation: -10,
      },
    },
    resources: {
      images: [
        {
          url: '/images/product-1.jpg',
          originalUrl: '/images/product-1-large.jpg',
          width: 1600,
          height: 1200,
          fileName: 'Front view',
          datasourceName: 'Source A',
          cacheKey: 'img-1',
        },
      ],
      videos: [
        {
          url: 'https://cdn.example.com/video.mp4',
          fileName: 'Demo video',
          datasourceName: 'Source B',
          cacheKey: 'vid-1',
        },
      ],
      pdfs: [],
    },
    scores: {
      ecoscore: { relativeValue: 3 },
    },
  } as unknown as ProductDto

  const breadcrumbs = [
    { title: 'Home', link: '/' },
    { title: 'Appliances', link: '/appliances' },
    { title: 'Demo Product', link: '/appliances/demo-product' },
  ]

  const mountComponent = async () =>
    await mountSuspended(ProductHero, {
      props: { product, breadcrumbs },
      global: {
        plugins: [i18n],
        stubs: {
          ImpactScore: defineComponent({
            name: 'ImpactScoreStub',
            props: ['score'],
            setup(props) {
              return () => h('div', { class: 'impact-score-stub' }, String(props.score ?? ''))
            },
          }),
          NuxtImg: defineComponent({
            name: 'NuxtImgStub',
            props: ['src', 'alt'],
            setup(props) {
              return () => h('img', { class: 'nuxt-img-stub', src: props.src, alt: props.alt })
            },
          }),
          ClientOnly: defineComponent({
            name: 'ClientOnlyStub',
            setup(_, { slots }) {
              return () => slots.default?.()
            },
          }),
          'v-icon': defineComponent({
            name: 'VIconStub',
            props: ['icon'],
            setup(props) {
              return () => h('span', { class: 'v-icon-stub' }, props.icon as string)
            },
          }),
          'v-btn': defineComponent({
            name: 'VBtnStub',
            setup(_, { slots, attrs }) {
              return () =>
                h(
                  'button',
                  {
                    class: 'v-btn-stub',
                    type: 'button',
                    onClick: attrs.onClick as ((event: MouseEvent) => void) | undefined,
                  },
                  slots.default?.(),
                )
            },
          }),
          'v-tooltip': defineComponent({
            name: 'VTooltipStub',
            props: ['text'],
            setup(props, { slots }) {
              return () =>
                h('div', { class: 'v-tooltip-stub', 'data-tooltip': props.text }, [
                  slots.activator?.({ props: {} }),
                  slots.default?.(),
                ])
            },
          }),
          NuxtLink: defineComponent({
            name: 'NuxtLinkStub',
            props: ['to'],
            setup(props, { slots, attrs }) {
              return () =>
                h(
                  'a',
                  {
                    class: 'nuxt-link-stub',
                    href: props.to as string,
                    target: attrs.target as string | undefined,
                    rel: attrs.rel as string | undefined,
                  },
                  slots.default?.(),
                )
            },
          }),
          CategoryNavigationBreadcrumbs: defineComponent({
            name: 'CategoryNavigationBreadcrumbsStub',
            props: {
              items: {
                type: Array as PropType<Array<{ title: string; link?: string }>>,
                default: () => [],
              },
              ariaLabel: {
                type: String,
                default: '',
              },
            },
            setup(props) {
              return () =>
                h(
                  'nav',
                  { 'aria-label': props.ariaLabel, class: 'breadcrumbs-stub' },
                  props.items.map((item) =>
                    h(
                      'a',
                      { class: 'breadcrumbs-stub__item', href: item.link ?? '#' },
                      item.title,
                    ),
                  ),
                )
            },
          }),
        },
      },
    })

  it('renders gallery stage and thumbnails and switches media on selection', async () => {
    const wrapper = await mountComponent()

    const stage = wrapper.get('[data-testid="product-gallery-stage"]')
    expect(stage.classes()).not.toContain('product-gallery__stage--video')
    expect(stage.text()).toContain('Source A')

    const thumbnails = wrapper.findAll('[data-testid="product-gallery-thumbnail"]')
    expect(thumbnails).toHaveLength(2)
    expect(thumbnails[0]?.classes()).toContain('product-gallery__thumbnail-button--active')
    expect(thumbnails[1]?.find('.product-gallery__thumbnail-badge').exists()).toBe(true)

    await thumbnails[1]?.trigger('click')
    await wrapper.vm.$nextTick()

    expect(stage.classes()).toContain('product-gallery__stage--video')
    expect(stage.find('.product-gallery__stage-overlay').exists()).toBe(true)

    await wrapper.unmount()
  })

  it('renders pricing metadata, breadcrumbs and smooth scroll actions', async () => {
    const priceHeading = document.createElement('h2')
    priceHeading.id = 'price-history'
    priceHeading.getBoundingClientRect = () =>
      ({ top: 200, left: 0, right: 0, bottom: 0, width: 0, height: 0, x: 0, y: 0 } as DOMRect)
    document.body.appendChild(priceHeading)

    const offersHeading = document.createElement('h3')
    offersHeading.id = 'offers-list'
    offersHeading.getBoundingClientRect = () =>
      ({ top: 400, left: 0, right: 0, bottom: 0, width: 0, height: 0, x: 0, y: 0 } as DOMRect)
    document.body.appendChild(offersHeading)

    const scrollToSpy = vi.spyOn(window, 'scrollTo').mockImplementation(() => {})

    const wrapper = await mountComponent()

    const breadcrumbLinks = wrapper.findAll('.breadcrumbs-stub__item')
    expect(breadcrumbLinks).toHaveLength(3)
    expect(breadcrumbLinks[2]?.attributes('href')).toBe('/appliances/demo-product')

    const merchantPrefix = wrapper.get('.product-hero__price-merchant-prefix')
    expect(merchantPrefix.text()).toBe('At')

    const merchantLink = wrapper.get('.nuxt-link-stub')
    expect(merchantLink.text()).toContain('Shop')
    expect(merchantLink.attributes('href')).toBe('https://example.com')
    expect(wrapper.find('.product-hero__price-merchant-favicon').attributes('src')).toBe(
      'https://example.com/favicon.ico',
    )

    const trendButton = wrapper.get('.product-hero__price-trend')
    expect(trendButton.text()).toContain('Price drop of')
    await trendButton.trigger('click')
    expect(scrollToSpy).toHaveBeenCalledWith(expect.objectContaining({ top: 80, behavior: 'smooth' }))

    scrollToSpy.mockClear()

    const offersButton = wrapper.get('.v-btn-stub')
    expect(offersButton.text()).toBe('View the 4 offers')
    await offersButton.trigger('click')
    expect(scrollToSpy).toHaveBeenCalledWith(expect.objectContaining({ top: 264, behavior: 'smooth' }))

    scrollToSpy.mockRestore()
    priceHeading.remove()
    offersHeading.remove()
    await wrapper.unmount()
  })
})
