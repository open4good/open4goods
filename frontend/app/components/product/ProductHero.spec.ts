import { mountSuspended } from '@nuxt/test-utils/runtime'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent, h, onMounted, ref, type PropType } from 'vue'
import { createPinia, setActivePinia } from 'pinia'
import { createI18n } from 'vue-i18n'
import type { AttributeConfigDto, ProductDto, ProductScoreDto } from '~~/shared/api-client'
import ProductHero from './ProductHero.vue'
import { useProductCompareStore } from '~/stores/useProductCompareStore'

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
            gtinCountryLabel: 'GTIN registration country',
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
            compare: {
              label: 'Compare',
              selected: 'In comparison',
              add: 'Add to compare',
              remove: 'Remove from compare',
              ariaAdd: 'Add {name} to compare',
              ariaSelected: '{name} is in your comparison list',
            },
            offerConditions: {
              occasion: 'Second-hand',
              new: 'New',
            },
            offerConditionsToggleAria: 'Choose which offer condition to highlight',
          },
          price: {
            trend: {
              decrease: 'Price drop of {amount}',
              increase: 'Price increase of {amount}',
              stable: 'Price unchanged',
            },
          },
        },
        category: {
          products: {
            compare: {
              limitReached: 'Limit reached',
              differentCategory: 'Different category',
              missingIdentifier: 'Missing identifier',
              addToList: 'Add to compare',
              removeFromList: 'Remove from compare',
            },
            untitledProduct: 'Untitled product',
          },
        },
      },
    },
  })

  beforeEach(() => {
    setActivePinia(createPinia())
    window.localStorage.clear()
    product = JSON.parse(JSON.stringify(baseProduct)) as ProductDto
  })

  const baseProduct = {
    gtin: '0123456789012',
    names: { h1Title: 'Demo Product' },
    identity: { brand: 'BrandCo', model: 'Model X', bestName: 'Demo Product' },
    base: {
      ecoscoreValue: 3.5,
      bestName: 'Demo Product',
      gtinInfo: { countryName: 'France', countryFlagUrl: '/flag.png' },
      coverImagePath: '/cover.jpg',
    },
    slug: 'demo-product',
    fullSlug: 'appliances/demo-product',
    offers: {
      offersCount: 4,
      bestPrice: {
        price: 799,
        currency: 'EUR',
        datasourceName: 'Shop',
        url: 'https://example.com',
        favicon: 'https://example.com/favicon.ico',
        condition: 'NEW',
      },
      bestNewOffer: {
        price: 799,
        currency: 'EUR',
        datasourceName: 'Shop',
        url: 'https://example.com',
        favicon: 'https://example.com/favicon.ico',
        condition: 'NEW',
      },
      bestOccasionOffer: {
        price: 649,
        currency: 'EUR',
        datasourceName: 'Merchant U',
        url: 'https://merchant-u.example',
        favicon: 'https://merchant-u.example/favicon.ico',
        condition: 'OCCASION',
      },
      occasionTrend: {
        trend: 'PRICE_INCREASE',
        variation: 5,
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
      ecoscore: {
        id: 'ECOSCORE',
        relativeValue: '3',
        relativ: { value: 3 },
      },
      scores: {
        ECOSCORE: {
          id: 'ECOSCORE',
          on20: 14,
        },
      },
    },
  } as unknown as ProductDto
  let product: ProductDto

  const breadcrumbs = [
    { title: 'Home', link: '/' },
    { title: 'Appliances', link: '/appliances' },
    { title: 'BrandCo', link: '/appliances#state-brandco' },
  ]

  const popularAttributes = [
    { key: 'identity.model', name: 'Model' },
    { key: 'base.gtinInfo.countryName', name: 'Origin' },
  ] as AttributeConfigDto[]

  const rewriteImpactScoreEntry = (entry: Partial<ProductScoreDto>) => {
    const score: ProductScoreDto = {
      id: 'ECOSCORE',
      ...entry,
    }

    product.scores = {
      ecoscore: { ...score },
      scores: {
        ECOSCORE: { ...score },
      },
    }
  }

  const mountComponent = async () =>
    await mountSuspended(ProductHero, {
      props: { product, breadcrumbs, popularAttributes },
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
                    class: ['v-btn-stub', attrs.class],
                    type: 'button',
                    disabled: attrs.disabled as boolean | undefined,
                    'aria-pressed': attrs['aria-pressed'] as string | boolean | undefined,
                    'aria-label': attrs['aria-label'] as string | undefined,
                    title: attrs.title as string | undefined,
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
                    class: ['nuxt-link-stub', attrs.class],
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
    expect(stage.classes()).toContain('product-gallery__stage--video')
    expect(stage.find('.product-gallery__stage-overlay').exists()).toBe(true)

    const thumbnails = wrapper.findAll('[data-testid="product-gallery-thumbnail"]')
    expect(thumbnails).toHaveLength(2)
    expect(thumbnails[0]?.find('.product-gallery__thumbnail-badge').exists()).toBe(true)
    expect(thumbnails[0]?.classes()).toContain('product-gallery__thumbnail-button--active')

    await thumbnails[1]?.trigger('click')
    await wrapper.vm.$nextTick()

    expect(stage.classes()).not.toContain('product-gallery__stage--video')
    const stageImage = stage.find('img')
    expect(stageImage.exists()).toBe(true)
    expect(stageImage.attributes('alt')).toBe('Front view')

    await wrapper.unmount()
  })

  it('displays brand, model and popular attributes beneath the title', async () => {
    const wrapper = await mountComponent()

    expect(wrapper.get('.product-hero__title').text()).toBe('Demo Product')
    expect(wrapper.get('.product-hero__brand-name').text()).toBe('BrandCo')
    expect(wrapper.get('.product-hero__model-name').text()).toBe('Model X')

    const attributes = wrapper.findAll('.product-hero__attribute')
    expect(attributes).toHaveLength(2)
    expect(attributes[0]?.text()).toContain('Model')
    expect(attributes[0]?.text()).toContain('Model X')
    expect(attributes[1]?.text()).toContain('Origin')
    expect(attributes[1]?.text()).toContain('France')

    await wrapper.unmount()
  })

  it('places the impact score summary before the attribute list', async () => {
    const wrapper = await mountComponent()

    const impactOverview = wrapper.get('.product-hero__impact-overview')
    expect(impactOverview.text()).toContain('3.5')
    expect(impactOverview.text()).not.toContain('Impact score')
    expect(impactOverview.get('.impact-score-stub').text()).toBe('3.5')

    const brandLine = wrapper.get('.product-hero__brand-line')
    const attributes = wrapper.get('.product-hero__attributes')
    expect(impactOverview.element.nextElementSibling).toBe(brandLine.element)
    expect(brandLine.element.nextElementSibling).toBe(attributes.element)

    await wrapper.unmount()
  })

  describe('impact score resolution', () => {
    it.each([
      ['on20 values', { on20: 18 }, '4.5'],
      ['percentages', { percent: 70 }, '3.5'],
      ['absolute max ranges', { absolute: { value: 80, max: 160 } }, '2.5'],
      ['relative fallbacks', { relativ: { value: 4.2 } }, '4.2'],
    ])('normalises %s to a five point scale', async (
      _,
      scoreEntry: Partial<ProductScoreDto>,
      expected: string,
    ) => {
      rewriteImpactScoreEntry(scoreEntry)
      const wrapper = await mountComponent()

      expect(wrapper.get('.impact-score-stub').text()).toBe(expected)

      await wrapper.unmount()
    })

    it('renders the impact score stub when only relative values are available', async () => {
      rewriteImpactScoreEntry({ relativ: { value: 6.2 } })

      const wrapper = await mountComponent()

      expect(wrapper.get('.impact-score-stub').text()).toBe('5')
      expect(wrapper.get('.product-hero__impact-overview').exists()).toBe(true)

      await wrapper.unmount()
    })
  })

  it('toggles compare state with visual feedback', async () => {
    const wrapper = await mountComponent()
    const store = useProductCompareStore()

    const compareButton = wrapper.get('.product-hero__compare-button')
    expect(compareButton.get('.product-hero__compare-label').text()).toBe('Compare')
    expect(compareButton.classes()).not.toContain('product-hero__compare-button--active')
    expect(store.hasProduct(product)).toBe(false)

    await compareButton.trigger('click')
    await wrapper.vm.$nextTick()

    expect(store.hasProduct(product)).toBe(true)
    expect(compareButton.get('.product-hero__compare-label').text()).toBe('In comparison')
    expect(compareButton.classes()).toContain('product-hero__compare-button--active')

    await compareButton.trigger('click')
    await wrapper.vm.$nextTick()

    expect(store.hasProduct(product)).toBe(false)

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
    expect(breadcrumbLinks).toHaveLength(4)
    expect(breadcrumbLinks[1]?.attributes('href')).toBe('/appliances')
    expect(breadcrumbLinks[2]?.text()).toBe('BrandCo')
    expect(breadcrumbLinks[2]?.attributes('href')).toBe('/appliances#state-brandco')
    expect(breadcrumbLinks[3]?.text()).toBe('Model X')
    expect(breadcrumbLinks[3]?.attributes('href')).toBe('#')

    const chips = wrapper.findAll('.product-hero__price-chip')
    expect(chips).toHaveLength(2)
    expect(chips[0]?.text()).toBe('Second-hand')
    expect(chips[0]?.classes()).toContain('product-hero__price-chip--active')

    const priceValue = wrapper.get('.product-hero__price-value')
    expect(priceValue.text()).toBe('649')
    expect(wrapper.get('.product-hero__price-currency').text()).toBe('€')

    const merchantLink = wrapper.get('.product-hero__price-merchant-link')
    expect(merchantLink.text()).toContain('Merchant U')
    expect(merchantLink.attributes('href')).toBe('https://merchant-u.example')
    expect(merchantLink.find('.product-hero__price-merchant-favicon').attributes('width')).toBe('48')

    let trendButton = wrapper.get('.product-hero__price-trend')
    expect(trendButton.text()).toContain('Price increase of')
    await trendButton.trigger('click')
    expect(scrollToSpy).toHaveBeenCalledWith(expect.objectContaining({ top: 80, behavior: 'smooth' }))

    scrollToSpy.mockClear()

    await chips[1]?.trigger('click')
    await wrapper.vm.$nextTick()

    expect(chips[1]?.classes()).toContain('product-hero__price-chip--active')
    expect(priceValue.text()).toBe('799')
    expect(wrapper.get('.product-hero__price-currency').text()).toBe('€')

    const refreshedMerchantLink = wrapper.get('.product-hero__price-merchant-link')
    expect(refreshedMerchantLink.text()).toContain('Shop')
    expect(refreshedMerchantLink.attributes('href')).toBe('https://example.com')

    trendButton = wrapper.get('.product-hero__price-trend')
    expect(trendButton.text()).toContain('Price drop of')

    const offersButton = wrapper
      .findAll('.v-btn-stub')
      .find((btn) => btn.text().includes('View the 4 offers'))
    expect(offersButton?.exists()).toBe(true)
    await offersButton?.trigger('click')
    expect(scrollToSpy).toHaveBeenCalledWith(expect.objectContaining({ top: 264, behavior: 'smooth' }))

    scrollToSpy.mockRestore()
    priceHeading.remove()
    offersHeading.remove()
    await wrapper.unmount()
  })
})
