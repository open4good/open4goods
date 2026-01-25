import { mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import CategoryFiltersPanel from './CategoryFiltersPanel.vue'

// Mock sub-components
const CategoryFilterListStub = {
  name: 'CategoryFilterList',
  props: ['fields'],
  template:
    '<div class="category-filter-list-stub"><div v-for="field in fields" :key="field.mapping" class="filter-item">{{ field.mapping }}</div></div>',
}

vi.mock('vue-i18n', () => ({
  useI18n: () => ({
    t: (key: string) => key,
  }),
}))

describe('CategoryFiltersPanel Expansion', () => {
  const mockImpactPrimary = [{ mapping: 'ecoscore' }]
  const mockImpactRemaining = [
    { mapping: 'carbon_footprint' },
    { mapping: 'energy' },
  ]
  const mockFilterOptions = {
    impact: [...mockImpactPrimary, ...mockImpactRemaining],
    technical: [],
    global: [],
  }

  it('initially shows only primary impact filters when collapsed', () => {
    const wrapper = mount(CategoryFiltersPanel, {
      props: {
        filterOptions: mockFilterOptions,
        aggregations: [],
        filters: { filters: [] },
        impactExpanded: false,
        technicalExpanded: false,
      },
      global: {
        stubs: {
          CategoryFilterList: CategoryFilterListStub,
          VIcon: true,
          VBtn: true,
        },
      },
    })

    // We expect 2 lists: one for global (empty), one for impact primary.
    // Wait, global is also a list.
    // Structure:
    // Global Section -> List
    // Impact Section -> List (Primary)
    //                -> Button (Show More)
    //                -> List (Remaining, v-if expanded)

    // Global fields are empty in mockFilterOptions.
    // Impact has items.

    // Find impact section
    const impactSection = wrapper.findAll('.category-filters__section')[1]
    const primaryList = impactSection.find('.category-filter-list-stub')

    expect(primaryList.text()).toContain('ecoscore')
    expect(primaryList.text()).not.toContain('carbon_footprint')

    // Check if remaining list is absent
    const remainingList = impactSection.findAll('.category-filter-list-stub')[1]
    expect(remainingList).toBeUndefined()
  })

  it('shows remaining filters when expanded', () => {
    const wrapper = mount(CategoryFiltersPanel, {
      props: {
        filterOptions: mockFilterOptions,
        aggregations: [],
        filters: { filters: [] },
        impactExpanded: true,
        technicalExpanded: false,
      },
      global: {
        stubs: {
          CategoryFilterList: CategoryFilterListStub,
          VIcon: true,
          VBtn: true,
        },
      },
    })

    const impactSection = wrapper.findAll('.category-filters__section')[1]
    const lists = impactSection.findAll('.category-filter-list-stub')

    expect(lists.length).toBe(2)
    expect(lists[1].text()).toContain('carbon_footprint')
  })

  it('emits update:impactExpanded when button is clicked', async () => {
    const wrapper = mount(CategoryFiltersPanel, {
      props: {
        filterOptions: mockFilterOptions,
        aggregations: [],
        filters: { filters: [] },
        impactExpanded: false,
        technicalExpanded: false,
      },
      global: {
        stubs: {
          CategoryFilterList: CategoryFilterListStub,
          VIcon: true,
          VBtn: {
            template: '<button @click="$emit(\'click\')"><slot/></button>',
          },
        },
      },
    })

    // Find the button in the see-more section
    const button = wrapper.find('.category-filters__see-more button')
    expect(button.exists()).toBe(true)

    await button.trigger('click')

    expect(wrapper.emitted('update:impactExpanded')).toBeTruthy()
    expect(wrapper.emitted('update:impactExpanded')![0]).toEqual([true])
  })

  it('emits update:impactExpanded false when expanded and button is clicked', async () => {
    const wrapper = mount(CategoryFiltersPanel, {
      props: {
        filterOptions: mockFilterOptions,
        aggregations: [],
        filters: { filters: [] },
        impactExpanded: true,
        technicalExpanded: false,
      },
      global: {
        stubs: {
          CategoryFilterList: CategoryFilterListStub,
          VIcon: true,
          VBtn: {
            template: '<button @click="$emit(\'click\')"><slot/></button>',
          },
        },
      },
    })

    const button = wrapper.find('.category-filters__see-more button')

    await button.trigger('click')

    expect(wrapper.emitted('update:impactExpanded')).toBeTruthy()
    expect(wrapper.emitted('update:impactExpanded')![0]).toEqual([false])
  })
})
