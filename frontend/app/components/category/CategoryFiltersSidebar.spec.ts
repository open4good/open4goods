import { describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import { createVuetify } from 'vuetify'
import { defineComponent, h } from 'vue'

import CategoryFiltersSidebar from './CategoryFiltersSidebar.vue'

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
            advanced: 'Filtres avancés',
            clearAllTooltip: 'Effacer tous les filtres',
            searchLabel: 'Rechercher des filtres',
            toggle: {
              hide: 'Masquer la colonne des filtres',
            },
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
          VTooltip: defineComponent({
            name: 'VTooltipStub',
            setup(_, { slots }) {
              return () =>
                h('div', slots.activator?.({ props: {} }) ?? slots.default?.())
            },
          }),
          VBtn: defineComponent({
            name: 'VBtnStub',
            emits: ['click'],
            setup(_, { attrs, emit }) {
              return () =>
                h('button', {
                  ...attrs,
                  type: 'button',
                  onClick: () => emit('click'),
                })
            },
          }),
          VChip: defineComponent({
            name: 'VChipStub',
            setup(_, { attrs, slots }) {
              return () => h('span', attrs, slots.default?.())
            },
          }),
          VTextField: defineComponent({
            name: 'VTextFieldStub',
            props: {
              modelValue: { type: String, default: '' },
            },
            emits: ['update:modelValue'],
            setup(props, { attrs, emit }) {
              return () =>
                h('input', {
                  ...attrs,
                  value: props.modelValue,
                  onInput: () => emit('update:modelValue', 'brand'),
                })
            },
          }),
        },
      },
    })

  it('renders the filters sidebar without the Eco-score card', () => {
    const wrapper = mountComponent()

    expect(wrapper.find('[data-test="category-ecoscore-card"]').exists()).toBe(
      false
    )
  })

  it('exposes active count, clear, collapse, and search controls', async () => {
    const wrapper = mountComponent({
      activeFiltersCount: 2,
      showHeader: true,
      showCollapseButton: true,
      showFilterSearch: true,
      filterSearchTerm: '',
    })

    expect(wrapper.get('[data-testid="category-filters-active-count"]').text())
      .toBe('2')

    await wrapper.get('[data-testid="category-filters-clear"]').trigger('click')
    expect(wrapper.emitted('clear-mobile')).toHaveLength(1)

    await wrapper
      .get('[data-testid="category-filters-collapse"]')
      .trigger('click')
    expect(wrapper.emitted('collapse')).toHaveLength(1)

    await wrapper.get('[data-testid="category-filters-search"]').trigger('input')
    expect(wrapper.emitted('update:filterSearchTerm')?.[0]).toEqual(['brand'])
  })
})
