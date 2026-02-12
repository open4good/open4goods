import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import CategoryFiltersPanel from './CategoryFiltersPanel.vue'
import { createVuetify } from 'vuetify'
import * as components from 'vuetify/components'
import * as directives from 'vuetify/directives'

vi.mock('vue-i18n', () => ({
  useI18n: () => ({
    t: (key: string) => key,
  }),
}))

const vuetify = createVuetify({
  components,
  directives,
})

const mockFilterOptions = {
  impact: [
    { mapping: 'scores.ECOSCORE.value', label: 'Eco-score' },
    { mapping: 'scores.ECOSCORE.ranking', label: 'Eco-ranking' },
    { mapping: 'scores.ECOSCORE.relativ.value', label: 'Eco-relative' },
    { mapping: 'carbon_footprint', label: 'Carbon Footprint' },
  ],
  technical: [
    { mapping: 'repairability', label: 'Repairability' },
    { mapping: 'durability', label: 'Durability' },
    { mapping: 'spare_parts', label: 'Spare Parts' },
    { mapping: 'spare_parts', label: 'Spare Parts' }, // Duplicate
    { mapping: 'energy_class', label: 'Energy Class' },
  ],
  global: [],
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
const mockAggregations: any[] = []

describe('CategoryFiltersPanel Reproduction', () => {
  it('should exclude specific impact filters', () => {
    const wrapper = mount(CategoryFiltersPanel, {
      global: {
        plugins: [vuetify],
        mocks: {
          t: (key: string) => key,
        },
        stubs: {
          CategoryFilterList: true,
        },
      },
      props: {
        filterOptions: mockFilterOptions,
        aggregations: mockAggregations,
        filters: null,
        impactExpanded: true,
        technicalExpanded: true,
      },
    })

    // Access computed properties via vm
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const impactPrimary = (wrapper.vm as any).impactPrimary
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const impactRemaining = (wrapper.vm as any).impactRemaining

    const allImpactMappings = [
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      ...impactPrimary.map((i: any) => i.mapping),
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      ...impactRemaining.map((i: any) => i.mapping),
    ]

    expect(allImpactMappings).not.toContain('scores.ECOSCORE.ranking')
    expect(allImpactMappings).not.toContain('scores.ECOSCORE.relativ.value')
  })

  it('should have duplicated technical filters', () => {
    const wrapper = mount(CategoryFiltersPanel, {
      global: {
        plugins: [vuetify],
        mocks: {
          t: (key: string) => key,
        },
        stubs: {
          CategoryFilterList: true,
        },
      },
      props: {
        filterOptions: mockFilterOptions,
        aggregations: mockAggregations,
        filters: null,
        impactExpanded: true,
        technicalExpanded: true,
      },
    })

    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const technicalPrimary = (wrapper.vm as any).technicalPrimary
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const technicalRemaining = (wrapper.vm as any).technicalRemaining

    const allTechnicalMappings = [
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      ...technicalPrimary.map((i: any) => i.mapping),
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      ...technicalRemaining.map((i: any) => i.mapping),
    ]

    const sparePartsCount = allTechnicalMappings.filter(
      (m: string) => m === 'spare_parts'
    ).length
    expect(sparePartsCount).toBe(1)
  })
})
