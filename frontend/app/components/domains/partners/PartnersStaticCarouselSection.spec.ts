import { mountSuspended } from '@nuxt/test-utils/runtime'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import PartnersStaticCarouselSection from './PartnersStaticCarouselSection.vue'

vi.mock('vuetify', () => ({
  useDisplay: () => ({
    xlAndUp: { value: false },
    lgAndUp: { value: false },
    mdAndUp: { value: true },
    smAndUp: { value: true },
  }),
}))

describe('PartnersStaticCarouselSection', () => {
  const baseProps = {
    title: 'Ecosystème',
    subtitle: 'Des partenaires engagés',
    eyebrow: 'Ecosystème',
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
    const wrapper = await mountComponent()

    const titles = wrapper.findAll('.partners-static__card h3').map((node) => node.text())
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
})
