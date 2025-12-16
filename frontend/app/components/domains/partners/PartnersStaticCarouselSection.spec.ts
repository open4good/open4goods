import { mountSuspended } from '@nuxt/test-utils/runtime'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import PartnersStaticCarouselSection from './PartnersStaticCarouselSection.vue'

const displayMock = {
  xlAndUp: { value: false },
  lgAndUp: { value: false },
  mdAndUp: { value: false },
  smAndUp: { value: false },
}

vi.mock('vuetify', () => ({
  useDisplay: () => displayMock,
}))

describe('PartnersStaticCarouselSection', () => {
  const baseProps = {
    title: 'Ecosystème',
    subtitle: 'Des partenaires engagés',
    carouselAriaLabel: 'Carrousel',
    emptyStateLabel: 'Bientôt disponible',
    linkLabel: 'Découvrir',
    fallbackDescription: 'Description à venir',
    partners: [
      {
        name: 'Collectif A',
        blocId: 'bloc:a',
        url: 'https://collectif-a.example',
        imageUrl: 'https://cdn.example/collectif-a.png',
      },
      {
        name: 'Fondation B',
        blocId: 'bloc:b',
        url: 'https://fondation-b.example',
        imageUrl: 'https://cdn.example/fondation-b.png',
      },
    ],
  }

  beforeEach(() => {
    vi.clearAllMocks()

    displayMock.xlAndUp.value = false
    displayMock.lgAndUp.value = false
    displayMock.mdAndUp.value = false
    displayMock.smAndUp.value = false
  })

  const mountComponent = async (props = {}) =>
    await mountSuspended(PartnersStaticCarouselSection, {
      props: {
        ...baseProps,
        ...props,
      },
      global: {
        stubs: {
          TextContent: {
            template: '<div class="text-content-stub" />',
            props: ['blocId', 'ipsumLength'],
          },
        },
      },
    })

  it('renders static partner cards with titles', async () => {
    displayMock.smAndUp.value = true

    const wrapper = await mountComponent()

    const titles = wrapper
      .findAll('.partners-static__card h3')
      .map(node => node.text())
    expect(titles).toEqual(['Collectif A', 'Fondation B'])

    await wrapper.unmount()
  })

  it('displays fallback description when blocId is missing', async () => {
    const wrapper = await mountComponent({
      partners: [
        {
          name: 'Sans contenu',
          url: 'https://example.com',
        },
      ],
    })

    expect(wrapper.text()).toContain(baseProps.fallbackDescription)

    await wrapper.unmount()
  })

  it('renders an empty state when no partners are provided', async () => {
    const wrapper = await mountComponent({ partners: [] })

    const alert = wrapper.find('.v-alert')
    expect(alert.exists()).toBe(true)
    expect(alert.text()).toContain(baseProps.emptyStateLabel)

    await wrapper.unmount()
  })

  it('disables carousel controls when all partners fit a single slide', async () => {
    displayMock.mdAndUp.value = true
    displayMock.smAndUp.value = true

    const partners = [
      { name: 'Collectif A' },
      { name: 'Fondation B' },
      { name: 'Coopérative C' },
    ]

    const wrapper = await mountComponent({ partners })

    const carousel = wrapper.findComponent({ name: 'VCarousel' })
    expect(carousel.props('showArrows')).toBe(false)
    expect(carousel.props('cycle')).toBe(false)

    const cards = wrapper.findAll('.partners-static__card')
    expect(cards).toHaveLength(3)

    await wrapper.unmount()
  })

  it('enables carousel controls when multiple slides are required', async () => {
    displayMock.mdAndUp.value = true
    displayMock.smAndUp.value = true

    const partners = [
      { name: 'Collectif A' },
      { name: 'Fondation B' },
      { name: 'Coopérative C' },
      { name: 'Association D' },
    ]

    const wrapper = await mountComponent({ partners })

    const carousel = wrapper.findComponent({ name: 'VCarousel' })
    expect(carousel.props('showArrows')).toBe(true)
    expect(carousel.props('cycle')).toBe(true)

    await wrapper.unmount()
  })
})
