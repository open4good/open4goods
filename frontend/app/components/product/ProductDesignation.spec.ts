import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import { describe, expect, it } from 'vitest'
import type { ProductDto } from '~~/shared/api-client'
import ProductDesignation from './ProductDesignation.vue'

const createI18nInstance = () =>
  createI18n({
    legacy: false,
    locale: 'en',
    messages: { en: {} },
  })

const buildProduct = (overrides: Partial<ProductDto> = {}): ProductDto =>
  ({
    gtin: 1,
    names: overrides.names,
    aiReview: overrides.aiReview,
    identity: overrides.identity,
    base: overrides.base,
    attributes: overrides.attributes,
    resources: overrides.resources,
    offers: overrides.offers,
    scores: overrides.scores,
    datasources: overrides.datasources,
    slug: overrides.slug,
    fullSlug: overrides.fullSlug,
  }) as ProductDto

describe('ProductDesignation', () => {
  it('renders the card title for card variant', () => {
    const wrapper = mount(ProductDesignation, {
      props: {
        product: buildProduct({
          names: {
            cardTitle: 'Card title',
            shortName: 'Short name',
            longName: 'Long name',
          },
        }),
        variant: 'card',
        titleTag: 'h3',
        titleClass: 'title',
      },
      global: {
        plugins: [createI18nInstance()],
      },
    })

    expect(wrapper.find('.title').text()).toBe('Card title')
    expect(wrapper.find('p').exists()).toBe(false)
  })

  it('renders the H1 title on page variant', () => {
    const wrapper = mount(ProductDesignation, {
      props: {
        product: buildProduct({
          names: { longName: 'Long name', h1Title: 'H1 title' },
          aiReview: { review: { shortDescription: 'AI short description' } },
        }),
        variant: 'page',
        titleTag: 'h1',
        titleClass: 'title',
        descriptionClass: 'description',
      },
      global: {
        plugins: [createI18nInstance()],
      },
    })

    expect(wrapper.find('.title').text()).toBe('H1 title')
    expect(wrapper.find('.description').exists()).toBe(false)
  })

  it('hides the short description when not provided', () => {
    const wrapper = mount(ProductDesignation, {
      props: {
        product: buildProduct({
          names: { longName: 'Long name' },
        }),
        variant: 'page',
      },
      global: {
        plugins: [createI18nInstance()],
      },
    })

    expect(wrapper.find('p').exists()).toBe(false)
  })
})
