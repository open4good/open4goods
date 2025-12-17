import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import { h } from 'vue'
import type { Component } from 'vue'

import SearchResultGroup from './SearchResultGroup.vue'

const createStub = (tag: string): Component => ({
  inheritAttrs: false,
  setup(_props, { attrs, slots }) {
    return () => h(tag, attrs, slots.default?.())
  },
})

const NuxtLinkStub: Component = {
  inheritAttrs: false,
  props: {
    to: {
      type: [String, Object],
      default: '#',
    },
    ariaLabel: {
      type: String,
      default: undefined,
    },
  },
  setup(props, { slots, attrs }) {
    const href = typeof props.to === 'string' ? props.to : '#'

    return () =>
      h(
        'a',
        {
          ...attrs,
          href,
          'aria-label': props.ariaLabel,
        },
        slots.default?.()
      )
  },
}

type SearchResultGroupProps = InstanceType<typeof SearchResultGroup>['$props']

const emptyProducts: SearchResultGroupProps['products'] = []

const mountGroup = (props: SearchResultGroupProps) =>
  mount(SearchResultGroup, {
    props,
    global: {
      stubs: {
        CategoryProductCardGrid: createStub('div'),
        NuxtLink: NuxtLinkStub,
        VIcon: createStub('span'),
      },
    },
  })

describe('SearchResultGroup', () => {
  it('renders the title, count and navigation link', () => {
    const wrapper = mountGroup({
      title: 'Coffee makers',
      products: emptyProducts,
      countLabel: '4 produits',
      verticalHomeUrl: '/cafetieres',
      categoryLinkLabel: 'Accéder à la catégorie',
      categoryLinkAria: 'Accéder à la catégorie Cafetières',
    })

    expect(wrapper.find('h2').text()).toBe('Coffee makers')
    expect(wrapper.find('.search-result-group__count').text()).toBe(
      '4 produits'
    )

    const link = wrapper.get('[data-test="search-result-group-link"]')
    expect(link.attributes('href')).toBe('/cafetieres')
    expect(link.attributes('aria-label')).toBe(
      'Accéder à la catégorie Cafetières'
    )
    expect(link.text()).toContain('Accéder à la catégorie')
  })

  it('does not render the link when no category URL is provided', () => {
    const wrapper = mountGroup({
      title: 'Mixers',
      products: emptyProducts,
      countLabel: '2 produits',
      verticalHomeUrl: null,
      categoryLinkLabel: 'Voir la catégorie',
    })

    expect(
      wrapper.find('[data-test="search-result-group-link"]').exists()
    ).toBe(false)
  })

  it('omits the count block when no label is provided', () => {
    const wrapper = mountGroup({
      title: 'Grinders',
      products: emptyProducts,
      verticalHomeUrl: '/grinders',
      categoryLinkLabel: 'Browse this category',
    })

    expect(wrapper.find('.search-result-group__count').exists()).toBe(false)
  })
})
