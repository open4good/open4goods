import { describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import { createVuetify } from 'vuetify'
import { defineComponent, h } from 'vue'

import ProductSizeGuide from './ProductSizeGuide.vue'
import CategorySizeGuideLink from '../category/CategorySizeGuideLink.vue'
import { inchesToCm } from '~/constants/vertical-size-guide'

vi.mock('#i18n', () => ({ useLocalePath: () => (path: string) => path }))

const i18n = createI18n({
  legacy: false,
  locale: 'fr-FR',
  messages: {
    'fr-FR': {
      verticalSizeGuide: {
        tv: {
          categoryLink: 'Quelle taille de TV choisir pour son salon ?',
          productTitle: 'Quelle taille choisir ?',
          productText: 'Texte sans diagonale.',
          productTextWithDiagonal: 'Diagonale {cm} cm.',
          productLink: 'Lire le billet',
        },
      },
    },
  },
})
const NuxtLink = defineComponent({
  props: { to: { type: String, default: '' } },
  setup:
    (props, { slots }) =>
    () =>
      h('a', { href: props.to }, slots.default?.()),
})
const global = {
  plugins: [createVuetify(), i18n],
  stubs: { NuxtLink },
}

describe('inchesToCm', () => {
  it('converts and rejects invalid values', () => {
    expect(inchesToCm(55)).toBe(140)
    expect(inchesToCm(null)).toBeNull()
    expect(inchesToCm(0)).toBeNull()
  })
})

describe('ProductSizeGuide', () => {
  it('renders a crawlable link and the diagonal for tv', () => {
    const wrapper = mount(ProductSizeGuide, {
      props: {
        verticalId: 'tv',
        indexedAttributes: {
          DIAGONALE_POUCES: { numericValue: 55 },
        } as never,
      },
      global,
    })
    expect(wrapper.find('a').attributes('href')).toBe(
      '/blog/quelle-taille-de-tv-choisir-pour-son-salon'
    )
    expect(wrapper.text()).toContain('Diagonale 140 cm.')
  })

  it('omits the diagonal when the attribute is missing', () => {
    const wrapper = mount(ProductSizeGuide, {
      props: { verticalId: 'tv', indexedAttributes: {} },
      global,
    })
    expect(wrapper.text()).toContain('Texte sans diagonale.')
  })

  it('renders nothing for other verticals', () => {
    const wrapper = mount(ProductSizeGuide, {
      props: { verticalId: 'dishwasher' },
      global,
    })
    expect(wrapper.find('a').exists()).toBe(false)
  })
})

describe('CategorySizeGuideLink', () => {
  it('links to the post for tv only', () => {
    const tv = mount(CategorySizeGuideLink, {
      props: { verticalId: 'tv' },
      global,
    })
    expect(tv.find('a').attributes('href')).toBe(
      '/blog/quelle-taille-de-tv-choisir-pour-son-salon'
    )
    expect(tv.text()).toContain('Quelle taille de TV choisir pour son salon ?')
    const other = mount(CategorySizeGuideLink, {
      props: { verticalId: 'fridge' },
      global,
    })
    expect(other.find('a').exists()).toBe(false)
  })
})
