import { describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import { createVuetify } from 'vuetify'
import { defineComponent, h } from 'vue'

import CategoryFiltersSidebar from './CategoryFiltersSidebar.vue'
import CategoryEcoscoreCard from './CategoryEcoscoreCard.vue'

vi.mock('vue-router', () => ({
  useRouter: () => ({
    resolve: (location: unknown) => {
      if (typeof location === 'string') {
        return { href: location }
      }

      const candidate = location as { href?: string; path?: string }
      return { href: candidate.href ?? candidate.path ?? '' }
    },
  }),
}))

describe('CategoryFiltersSidebar', () => {
  const vuetify = createVuetify()

  const i18n = createI18n({
    legacy: false,
    locale: 'fr-FR',
    messages: {
      'fr-FR': {
        category: {
          filters: {
            mobileApply: 'Appliquer',
            mobileClear: 'Effacer',
            ecoscore: {
              title: 'Découvrir',
              description: 'Description',
              cta: 'Voir',
              ariaLabel: 'Voir Eco-score',
            },
          },
        },
      },
    },
  })

  const baseProps = {
    filterOptions: null,
    aggregations: [],
    baselineAggregations: [],
    filters: null,
    impactExpanded: false,
    technicalExpanded: false,
    showMobileActions: false,
    hasDocumentation: true,
    wikiPages: [],
    relatedPosts: [],
    verticalHomeUrl: 'https://open4goods.example/maison',
  }

  const mountComponent = (overrides: Partial<typeof baseProps> = {}) =>
    mount(CategoryFiltersSidebar, {
      props: { ...baseProps, ...overrides },
      global: {
        plugins: [vuetify, i18n],
        stubs: {
          CategoryFiltersPanel: defineComponent({
            name: 'CategoryFiltersPanelStub',
            setup() {
              return () => h('div', { 'data-test': 'filters-panel-stub' })
            },
          }),
          CategoryDocumentationRail: defineComponent({
            name: 'CategoryDocumentationRailStub',
            setup() {
              return () => h('div', { 'data-test': 'documentation-rail-stub' })
            },
          }),
        },
      },
    })

  it('renders the Eco-score card when a vertical home URL is provided', () => {
    const wrapper = mountComponent()

    const cardComponent = wrapper.findComponent(CategoryEcoscoreCard)
    expect(cardComponent.exists()).toBe(true)
    expect(cardComponent.props('verticalHomeUrl')).toBe(
      'https://open4goods.example/maison'
    )

    const card = wrapper.get('[data-test="category-ecoscore-card"]')
    expect(card.text()).toContain('Découvrir')
  })

  it('hides the Eco-score card when no vertical home URL is available', () => {
    const wrapper = mountComponent({ verticalHomeUrl: null })

    expect(wrapper.find('[data-test="category-ecoscore-card"]').exists()).toBe(
      false
    )
  })
})
