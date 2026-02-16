import { mountSuspended } from '@nuxt/test-utils/runtime'
import { describe, expect, it } from 'vitest'
import ProductImpactSection from './ProductImpactSection.vue'

describe('ProductImpactSection', () => {
  const mountComponent = async (props: Record<string, unknown> = {}) => {
    // We don't need to create a new i18n instance here as it's already provided by the Nuxt environment
    // However, for testing specific missing keys, we can rely on mocking or ensuring the key exists if we were loading real locale files.
    // In this unit test context with mockNuxtImport or shallowMount/mountSuspended, we might be getting the global instance.

    // If we want to ADD messages to the existing global i18n or mock one, we should do it carefully.
    // simpler approach: just mock t/n/d if we don't care about actual translation logic, OR allow the global one.
    // The warning "Component ... has already been registered" suggests we are re-registering plugins.

    // Let's try mounting WITHOUT explicitly passing the i18n plugin again, relying on Nuxt test utils to handle it,
    // OR if we strictly need custom messages, we should probably mock useI18n instead.

    return mountSuspended(ProductImpactSection, {
      props: {
        scores: [],
        productName: 'Test Product',
        productBrand: 'Brand A',
        ...props,
      },
      global: {
        // plugins: [[i18n]], // Removing this to avoid "Component ... has already been registered"
        stubs: {
          ProductImpactEcoScoreCard: true,
          ImpactRadar: true,
          ProductAlternatives: true,
          ImpactScore: true,
          EprelDetailsTable: true,
        },
        mocks: {
          t: (key: string) =>
            key === 'product.impact.subtitle' ? 'Impact Subtitle' : key,
        },
      },
    })
  }

  it('renders correctly', async () => {
    const wrapper = await mountComponent()
    expect(wrapper.exists()).toBe(true)
  })
})
