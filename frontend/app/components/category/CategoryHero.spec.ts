import { mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'

vi.mock('vue-i18n', () => ({
  useI18n: () => ({
    t: (key: string) => {
      const map: Record<string, string> = {
        'category.hero.breadcrumbAriaLabel': 'Category navigation breadcrumb',
        'category.hero.missingBreadcrumbTitle': 'Category',
      }

      return map[key] ?? key
    },
  }),
}))

describe('CategoryHero', () => {
  const mountComponent = async (props: Record<string, unknown>) => {
    const module = await import('./CategoryHero.vue')
    const CategoryHero = module.default

    return mount(CategoryHero, {
      props,
      global: {
        stubs: {
          VSheet: { template: '<div class="v-sheet"><slot /></div>' },
          VImg: {
            template: '<div class="v-img"><slot /></div>',
            props: ['src', 'alt', 'cover'],
          },
          VBreadcrumbs: { template: '<nav class="v-breadcrumbs"><slot /></nav>' },
          VBreadcrumbsItem: { template: '<span class="v-breadcrumbs-item"><slot /></span>' },
          VSkeletonLoader: { template: '<div class="v-skeleton" />' },
        },
      },
    })
  }

  it('renders hero title, description and breadcrumbs', async () => {
    const wrapper = await mountComponent({
      title: 'Energy efficient dishwashers',
      description: 'Compare eco-designed dishwashers to reduce energy consumption.',
      image: 'https://cdn.example.com/hero.jpg',
      breadcrumbs: [
        { title: 'Home', link: '/' },
        { title: 'Appliances', link: '/appliances' },
      ],
    })

    expect(wrapper.get('h1').text()).toBe('Energy efficient dishwashers')
    expect(wrapper.text()).toContain('Compare eco-designed dishwashers to reduce energy consumption.')

    const breadcrumbItems = wrapper.findAll('.v-breadcrumbs-item')
    expect(breadcrumbItems).toHaveLength(2)
    expect(breadcrumbItems[0].text()).toBe('Home')

    const section = wrapper.get('section')
    expect(section.attributes('aria-labelledby')).toBeTruthy()
  })
})
