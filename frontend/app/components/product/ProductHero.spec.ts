import { mountSuspended } from '@nuxt/test-utils/runtime'
import { describe, expect, it, vi } from 'vitest'
import { defineComponent, h } from 'vue'
import { createI18n } from 'vue-i18n'
import type { ProductDto } from '~~/shared/api-client'
import ProductHero from './ProductHero.vue'

type CompareListBlockReason =
  | 'limit-reached'
  | 'vertical-mismatch'
  | 'missing-identifier'

vi.mock('~/stores/useProductCompareStore', () => {
  let isSelected = false

  return {
    MAX_COMPARE_ITEMS: 4,
    useProductCompareStore: () => ({
      canAddProduct: () => ({
        success: true,
        reason: undefined as CompareListBlockReason | undefined,
      }),
      hasProduct: () => isSelected,
      toggleProduct: () => {
        isSelected = !isSelected
      },
    }),
  }
})

const createI18nInstance = () =>
  createI18n({
    legacy: false,
    locale: 'en-US',
    messages: {
      'en-US': {
        product: {
          hero: {
            breadcrumbAriaLabel: 'Breadcrumb',
            missingBreadcrumbTitle: 'Missing',
            gtinTooltip: 'GTIN country',
            compare: {
              add: 'Add to compare',
              label: 'Add to compare',
              selected: 'Selected',
              remove: 'Remove',
              limitReached: 'Limit',
              differentCategory: 'Different',
              missingIdentifier: 'Missing identifier',
            },
          },
        },
      },
    },
  })

const stubs = {
  CategoryNavigationBreadcrumbs: defineComponent({
    name: 'CategoryNavigationBreadcrumbsStub',
    props: {
      items: { type: Array, default: () => [] },
      ariaLabel: { type: String, default: '' },
    },
    setup(props) {
      return () =>
        h(
          'nav',
          { class: 'breadcrumbs-stub', 'aria-label': props.ariaLabel },
          (props.items as { title: string }[]).map(item =>
            h('span', { class: 'breadcrumb-item' }, item.title)
          )
        )
    },
  }),
  ImpactScore: defineComponent({
    name: 'ImpactScoreStub',
    setup() {
      return () => h('div', { class: 'impact-score-stub' }, 'impact')
    },
  }),
  ProductHeroGallery: defineComponent({
    name: 'ProductHeroGalleryStub',
    setup() {
      return () => h('div', { class: 'gallery-stub' }, 'gallery')
    },
  }),
  ProductHeroPricing: defineComponent({
    name: 'ProductHeroPricingStub',
    setup() {
      return () => h('div', { class: 'pricing-stub' }, 'pricing')
    },
  }),
  ProductAttributeSourcingLabel: defineComponent({
    name: 'ProductAttributeSourcingLabelStub',
    props: {
      value: { type: String, default: '' },
      sourcing: { type: Object, default: null },
      enableTooltip: { type: Boolean, default: true },
    },
    setup(_, { slots }) {
      return () =>
        h(
          'span',
          { class: 'attribute-sourcing-stub' },
          slots.default?.({ displayValue: 'value', displayHtml: '' })
        )
    },
  }),
  NuxtImg: defineComponent({
    name: 'NuxtImgStub',
    props: {
      src: { type: String, default: '' },
      alt: { type: String, default: '' },
    },
    setup(props) {
      return () =>
        h('img', { class: 'nuxt-img-stub', src: props.src, alt: props.alt })
    },
  }),

  'v-tooltip': defineComponent({
    name: 'VTooltipStub',
    props: { text: { type: String, default: '' } },
    setup(_, { slots }) {
      return () =>
        h('div', { class: 'v-tooltip-stub' }, [
          slots.activator?.({ props: {} }),
          slots.default?.(),
        ])
    },
  }),
  'v-btn': defineComponent({
    name: 'VBtnStub',
    props: { disabled: { type: Boolean, default: false } },
    setup(props, { slots, attrs }) {
      return () =>
        h(
          'button',
          {
            class: ['v-btn-stub', attrs.class],
            disabled: props.disabled,
            'aria-pressed': attrs['aria-pressed'] as boolean | undefined,
            'aria-label': attrs['aria-label'] as string | undefined,
            title: attrs.title as string | undefined,
            onClick: attrs.onClick as ((event: MouseEvent) => void) | undefined,
          },
          slots.default?.()
        )
    },
  }),
  'v-icon': defineComponent({
    name: 'VIconStub',
    props: { icon: { type: String, default: '' } },
    setup(props) {
      return () => h('span', { class: 'v-icon-stub' }, props.icon)
    },
  }),
}

const baseProduct: ProductDto = {
  names: { h1Title: 'Orbital Product' },
  identity: { brand: 'Orbit', model: 'X1', bestName: 'Orbit X1' },
  base: {
    bestName: 'Orbit X1',
    gtinInfo: {
      countryName: 'France',
      countryFlagUrl: 'https://flag.example/fr.png',
    },
  },
  aiReview: { review: { mediumTitle: 'Next-gen Orbit X1' } },
}

const mountComponent = async () =>
  mountSuspended(ProductHero, {
    props: {
      product: baseProduct,
      breadcrumbs: [{ title: 'Category' }],
      popularAttributes: [],
    },
    global: {
      plugins: [createI18nInstance()],
      stubs,
    },
  })

describe('ProductHero', () => {
  it('renders correctly', async () => {
    const wrapper = await mountComponent()

    expect(wrapper.get('.product-hero__title').text()).toContain(
      'Next-gen Orbit X1'
    )
    expect(wrapper.find('.product-hero__panel--pricing').exists()).toBe(true)
    expect(wrapper.find('.product-hero__details-section').exists()).toBe(true)

    expect(
      wrapper.findComponent({ name: 'ProductHeroBackgroundStub' }).exists()
    ).toBe(false)

    await wrapper.unmount()
  })
})
