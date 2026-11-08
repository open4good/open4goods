import { mountSuspended } from '@nuxt/test-utils/runtime'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import PartnersAffiliationSection from './PartnersAffiliationSection.vue'

vi.mock('vuetify', () => ({
  useDisplay: () => ({
    xlAndUp: { value: false },
    lgAndUp: { value: false },
    mdAndUp: { value: true },
    smAndUp: { value: true },
  }),
}))

describe('PartnersAffiliationSection', () => {
  const baseProps = {
    title: 'Nos partenaires',
    subtitle: 'Ils nous font confiance',
    searchLabel: 'Rechercher',
    searchPlaceholder: 'Nom du partenaire',
    emptyStateLabel: 'Aucun résultat',
    carouselAriaLabel: 'Carrousel',
    linkLabel: 'Visiter',
    partners: [
      {
        id: 'alpha',
        name: 'Alpha Market',
        affiliationLink: 'https://alpha.example',
        logoUrl: 'https://cdn.example/alpha.png',
      },
      {
        id: 'beta',
        name: 'Beta Shop',
        affiliationLink: 'https://beta.example',
        logoUrl: 'https://cdn.example/beta.png',
      },
    ],
  }

  beforeEach(() => {
    vi.clearAllMocks()
  })

  const mountComponent = async (props = {}) =>
    await mountSuspended(PartnersAffiliationSection, {
      props: {
        ...baseProps,
        ...props,
      },
    })

  it('renders a searchable carousel of partners', async () => {
    const wrapper = await mountComponent()

    const input = wrapper.find('input[type="text"]')
    expect(input.exists()).toBe(true)

    const cards = wrapper.findAll('.partners-affiliation__card')
    expect(cards.length).toBeGreaterThan(0)

    await wrapper.unmount()
  })

  it('filters partners when the search term changes', async () => {
    const wrapper = await mountComponent()

    const input = wrapper.find('input[type="text"]')
    await input.setValue('Beta')

    await wrapper.vm.$nextTick()

    const visibleNames = wrapper
      .findAll('.partners-affiliation__card h3')
      .map(card => card.text())

    expect(visibleNames).toEqual(['Beta Shop'])

    await wrapper.unmount()
  })

  it('shows an empty state when no partners match', async () => {
    const wrapper = await mountComponent()

    const input = wrapper.find('input[type="text"]')
    await input.setValue('Nonexistent')

    await wrapper.vm.$nextTick()

    const alert = wrapper.find('.v-alert')
    expect(alert.exists()).toBe(true)
    expect(alert.text()).toContain(baseProps.emptyStateLabel)

    await wrapper.unmount()
  })

  it('displays the empty state when there are no partners', async () => {
    const wrapper = await mountComponent({ partners: [] })

    const alert = wrapper.find('.v-alert')
    expect(alert.exists()).toBe(true)
    expect(alert.text()).toContain(baseProps.emptyStateLabel)

    await wrapper.unmount()
  })

  it('renders every partner across all carousel slides, not just the active one', async () => {
    // Regression guard: SEO crawlers (and curl without JS) only ever receive the
    // server-rendered HTML. v-carousel-item lazily mounts its content, so without
    // `eager`, only the active slide's partners end up in the markup even though
    // the full list is available server-side.
    const manyPartners = Array.from({ length: 10 }, (_, index) => ({
      id: `partner-${index}`,
      name: `Partner ${index}`,
      affiliationLink: `https://partner-${index}.example`,
      logoUrl: `https://cdn.example/partner-${index}.png`,
    }))

    const wrapper = await mountComponent({ partners: manyPartners })

    const cards = wrapper.findAll('.partners-affiliation__card')
    expect(cards.length).toBe(manyPartners.length)

    await wrapper.unmount()
  })
})
