import { mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import { createVuetify } from 'vuetify'
import type { CategoryBreadcrumbItemDto } from '~~/shared/api-client'

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

interface MountProps {
  title: string
  description?: string | null
  image?: string | null
  breadcrumbs?: CategoryBreadcrumbItemDto[]
  eyebrow?: string | null
  showImage?: boolean
}

describe('CategoryHero', () => {
  const vuetify = createVuetify()

  const mountComponent = async (props: MountProps) => {
    const module = await import('./CategoryHero.vue')
    const CategoryHero = module.default

    return mount(CategoryHero, {
      props,
      global: {
        plugins: [vuetify],
        stubs: {
          VSheet: { template: '<div class="v-sheet"><slot /></div>' },
          VImg: {
            template: '<div class="v-img"><slot /></div>',
            props: ['src', 'alt', 'cover'],
          },
          VSkeletonLoader: { template: '<div class="v-skeleton" />' },
          NuxtLink: { template: '<a><slot /></a>' },
        },
      },
    })
  }

  it('renders hero title, description and breadcrumbs', async () => {
    const wrapper = await mountComponent({
      title: 'Energy efficient dishwashers',
      description:
        'Compare eco-designed dishwashers to reduce energy consumption.',
      image: 'https://cdn.example.com/hero.jpg',
      breadcrumbs: [
        { title: 'Home', link: '/' },
        { title: 'Appliances', link: '/appliances' },
      ],
    })

    expect(wrapper.get('h1').text()).toBe('Energy efficient dishwashers')
    expect(wrapper.text()).toContain(
      'Compare eco-designed dishwashers to reduce energy consumption.'
    )

    const breadcrumbItems = wrapper.findAll(
      '.category-navigation-breadcrumbs__item'
    )
    expect(breadcrumbItems).toHaveLength(2)
    const firstBreadcrumbLink = breadcrumbItems.at(0)?.find('a')
    expect(firstBreadcrumbLink?.text()).toBe('Home')

    const lastBreadcrumb = breadcrumbItems.at(1)
    expect(lastBreadcrumb?.find('a').exists()).toBe(false)
    expect(lastBreadcrumb?.text()).toContain('Appliances')

    const section = wrapper.get('section')
    expect(section.attributes('aria-labelledby')).toBeTruthy()
  })

  it('conditionally renders the image based on showImage prop', async () => {
    const defaultProps = {
      title: 'Test',
      image: 'https://example.com/image.jpg',
    }

    const wrapperWithImage = await mountComponent({ ...defaultProps, showImage: true })
    expect(wrapperWithImage.find('.category-hero__media').exists()).toBe(true)

    const wrapperWithoutImage = await mountComponent({ ...defaultProps, showImage: false })
    expect(wrapperWithoutImage.find('.category-hero__media').exists()).toBe(false)
  })
})
