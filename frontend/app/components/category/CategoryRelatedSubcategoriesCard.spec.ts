import { mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import { createVuetify } from 'vuetify'

vi.mock('vue-i18n', () => ({
  useI18n: () => ({
    t: (key: string) => {
      const map: Record<string, string> = {
        'category.relatedSubcategories.title': 'Read more',
        'category.relatedSubcategories.subtitle': 'Other focused selections',
        'category.relatedSubcategories.itemSubtitle': 'Explore this subcategory',
        'category.relatedSubcategories.fallbackTitle': 'Related selection',
      }

      return map[key] ?? key
    },
  }),
}))

describe('CategoryRelatedSubcategoriesCard', () => {
  const vuetify = createVuetify()

  const mountComponent = async () => {
    const module = await import('./CategoryRelatedSubcategoriesCard.vue')
    const CategoryRelatedSubcategoriesCard = module.default

    return mount(CategoryRelatedSubcategoriesCard, {
      props: {
        parentUrl: '/dishwashers',
        activeSubcategoryId: 'compact',
        subcategories: [
          {
            id: 'compact',
            slug: 'compact-dishwashers',
            h1Title: 'Compact dishwashers',
          },
          {
            id: 'under_sink',
            slug: 'under-sink-dishwashers',
            h1Title: 'Under-sink dishwashers',
          },
        ],
      },
      global: {
        plugins: [vuetify],
        stubs: {
          VCard: { template: '<article><slot /></article>' },
          VCardItem: { template: '<header><slot name="prepend" /><slot /></header>' },
          VCardTitle: { template: '<h2><slot /></h2>' },
          VCardSubtitle: { template: '<p><slot /></p>' },
          VAvatar: { template: '<span><slot /></span>' },
          VIcon: { template: '<i />' },
          VDivider: { template: '<hr>' },
          VList: { template: '<nav><slot /></nav>' },
          VListItem: {
            props: ['title', 'subtitle', 'to'],
            template:
              '<a :href="to"><span class="title">{{ title }}</span><span class="subtitle">{{ subtitle }}</span></a>',
          },
        },
      },
    })
  }

  it('renders sibling subcategories and excludes the active one', async () => {
    const wrapper = await mountComponent()

    expect(wrapper.text()).toContain('Read more')
    expect(wrapper.text()).toContain('Under-sink dishwashers')
    expect(wrapper.text()).not.toContain('Compact dishwashers')
    expect(wrapper.get('a').attributes('href')).toBe(
      '/dishwashers/under-sink-dishwashers'
    )
  })
})
