import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

const pushMock = vi.fn()

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: pushMock,
  }),
}))

vi.mock('vue-i18n', () => ({
  useI18n: () => ({
    t: (key: string, params?: Record<string, string>) => {
      const dictionary: Record<string, string> = {
        'category.documentation.backToCategory':
          `Back to ${params?.category ?? ''}`.trim(),
        'category.documentation.guidesTitle': 'Helpful guides',
        'category.documentation.postsTitle': 'Related articles',
        'category.documentation.sidebarAria':
          `${params?.category ?? ''} documentation navigation`.trim(),
        'category.documentation.guidesAria':
          `Other guides for ${params?.category ?? ''}`.trim(),
        'category.documentation.postsAria':
          `Related blog posts about ${params?.category ?? ''}`.trim(),
      }

      return dictionary[key] ?? key
    },
  }),
}))

const mountComponent = async (props = {}) => {
  const module = await import('./GuideStickySidebar.vue')
  const Component = module.default

  return mount(Component, {
    props: {
      categoryName: 'Eco tech',
      categoryPath: '/electronics',
      guides: [
        { title: 'Energy saving tips', to: '/electronics/energy' },
        { title: 'Zero waste appliances', to: '/electronics/zero-waste' },
      ],
      posts: [
        { title: 'How to reduce consumption', to: '/blog/reduce-consumption' },
      ],
      ...props,
    },
    global: {
      stubs: {
        NuxtLink: { template: '<a><slot /></a>' },
        VBtn: { template: '<button><slot /></button>' },
        VIcon: { template: '<i />' },
      },
    },
  })
}

describe('GuideStickySidebar', () => {
  beforeEach(() => {
    pushMock.mockReset()
  })

  it('renders CTA and navigation panels', async () => {
    const wrapper = await mountComponent()

    const backCta = wrapper.get('[data-test="guide-sidebar-back"] button')
    expect(backCta.text()).toContain('Back to Eco tech')

    const guidePanel = wrapper.find('[data-test="guide-sidebar-guides"]')
    expect(guidePanel.text()).toContain('Helpful guides')
    expect(guidePanel.text()).toContain('Energy saving tips')

    const postPanel = wrapper.find('[data-test="guide-sidebar-posts"]')
    expect(postPanel.text()).toContain('Related articles')
    expect(postPanel.text()).toContain('How to reduce consumption')
  })

  it('navigates to a guide when a guide item is selected', async () => {
    const wrapper = await mountComponent()

    const firstGuideButton = wrapper.get(
      '[data-test="guide-sidebar-guides"] .sticky-section-navigation__link'
    )
    await firstGuideButton.trigger('click')

    expect(pushMock).toHaveBeenCalledWith('/electronics/energy')
  })

  it('truncates long titles in navigation sections', async () => {
    const longTitle =
      'This is a very long guide title that should be truncated gracefully by the sidebar component'
    const wrapper = await mountComponent({
      guides: [{ title: longTitle, to: '/electronics/long-guide' }],
      posts: [],
    })

    const guideLabel = wrapper.get('.sticky-section-navigation__label')
    expect(guideLabel.text().endsWith('…')).toBe(true)
    expect(guideLabel.text().length).toBeLessThan(longTitle.length)
  })
})
