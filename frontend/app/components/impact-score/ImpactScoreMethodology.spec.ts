import { describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { mockNuxtImport } from '@nuxt/test-utils/runtime'
import { defineComponent } from 'vue'
import ImpactScoreMethodology from './ImpactScoreMethodology.vue'

mockNuxtImport('useLocalePath', () => () => (input: string) => input)
mockNuxtImport('useI18n', () => () => ({ t: (key: string) => key }))
mockNuxtImport('useRouter', () => () => ({ push: () => {} }))
vi.stubGlobal('$fetch', () => Promise.resolve())

const VChipStub = defineComponent({
  name: 'VChip',
  inheritAttrs: false,
  template: '<a v-bind="$attrs"><slot /></a>',
})

const VCardStub = defineComponent({
  name: 'VCard',
  inheritAttrs: false,
  template: '<div v-bind="$attrs"><slot /></div>',
})

const VImgStub = defineComponent({
  name: 'VImg',
  inheritAttrs: false,
  template: '<img v-bind="$attrs" />',
})

const VIconStub = defineComponent({
  name: 'VIcon',
  template: '<span />',
})

const VBtnStub = defineComponent({
  name: 'VBtn',
  inheritAttrs: false,
  template: '<button v-bind="$attrs"><slot /></button>',
})

const ResponsiveCarouselStub = defineComponent({
  name: 'ResponsiveCarousel',
  props: {
    items: {
      type: Array,
      default: () => [],
    },
  },
  template:
    '<div><slot name="item" v-for="(item, index) in items" :item="item" :index="index" /></div>',
})

const NuxtLinkStub = defineComponent({
  name: 'NuxtLink',
  inheritAttrs: false,
  template: '<a v-bind="$attrs"><slot /></a>',
})

const globalConfig = {
  stubs: {
    VChip: VChipStub,
    VCard: VCardStub,
    VBtn: VBtnStub,
    VImg: VImgStub,
    VIcon: VIconStub,
    ResponsiveCarousel: ResponsiveCarouselStub,
    NuxtLink: NuxtLinkStub,
  },
}

describe('ImpactScoreMethodology', () => {
  it('renders clickable capsules for each vertical', () => {
    const wrapper = mount(ImpactScoreMethodology, {
      props: {
        verticals: [
          {
            id: 'washing',
            verticalHomeTitle: 'Lave-linge',
            verticalHomeUrl: 'lave-linge',
            order: 1,
          },
          {
            id: 'tv',
            verticalHomeTitle: 'Téléviseurs',
            verticalHomeUrl: '/televiseurs',
            order: 2,
          },
        ],
      },
      global: globalConfig,
    })

    const cards = wrapper.findAll('.impact-score-methodology__card')
    expect(cards).toHaveLength(2)
    expect(cards[0].text()).toContain('Lave-linge')
    expect(cards[1].text()).toContain('Téléviseurs')
    expect(
      cards[0].find('.impact-score-methodology__card-cta').attributes('to')
    ).toBe('/lave-linge')
  })

  it('shows an empty state when no verticals are available', () => {
    const wrapper = mount(ImpactScoreMethodology, {
      props: {
        verticals: [],
      },
      global: globalConfig,
    })

    expect(wrapper.text()).toContain(
      'impactScorePage.sections.methodology.empty'
    )
  })
})
