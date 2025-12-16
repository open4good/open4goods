import { mountSuspended } from '@nuxt/test-utils/runtime'
import { flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { h, ref } from 'vue'

const useFullPageMock = vi.hoisted(() => vi.fn())

vi.mock('~/assets/css/text-content.css', () => ({}))

vi.mock('~/composables/cms/useFullPage', () => ({
  useFullPage: useFullPageMock,
}))

vi.mock(
  '~/components/category/navigation/CategoryNavigationBreadcrumbs.vue',
  () => ({
    default: {
      name: 'CategoryNavigationBreadcrumbs',
      props: {
        items: {
          type: Array,
          default: () => [],
        },
        ariaLabel: {
          type: String,
          default: '',
        },
      },
      template: `
      <nav class="category-navigation-breadcrumbs" :aria-label="ariaLabel">
        <ol>
          <li
            v-for="(item, index) in items"
            :key="index"
            class="category-navigation-breadcrumbs__item"
          >
            <a v-if="item.link" :href="item.link">{{ item.title }}</a>
            <span v-else>{{ item.title }}</span>
          </li>
        </ol>
      </nav>
    `,
    },
  })
)

vi.mock('~/composables/useAuth', () => ({
  useAuth: () => ({
    isLoggedIn: ref(false),
    hasRole: vi.fn(() => false),
  }),
}))

vi.mock('#app', () => ({
  useRuntimeConfig: () => ({ public: { editRoles: [] } }),
}))

const useSeoMetaMock = vi.fn()
const useHeadMock = vi.fn()
const useCanonicalUrlMock = vi.fn(() => ref('https://example.com/cms/page'))

vi.mock('#imports', () => ({
  useSeoMeta: (...args: unknown[]) => useSeoMetaMock(...args),
  useHead: (...args: unknown[]) => useHeadMock(...args),
  useCanonicalUrl: (...args: unknown[]) => useCanonicalUrlMock(...args),
  useI18n: () => ({
    t: (key: string) => {
      const dictionary: Record<string, string> = {
        'cms.page.error': 'Unable to load content',
        'common.actions.retry': 'Retry',
        'cms.page.loading': 'Loading',
        'cms.page.edit': 'Edit',
        'category.hero.breadcrumbAriaLabel': 'Category navigation breadcrumb',
      }

      return dictionary[key] ?? key
    },
  }),
}))

vi.mock('vue-i18n', () => ({
  useI18n: () => ({
    t: (key: string) => {
      const dictionary: Record<string, string> = {
        'cms.page.error': 'Unable to load content',
        'common.actions.retry': 'Retry',
        'cms.page.loading': 'Loading',
        'cms.page.edit': 'Edit',
        'category.hero.breadcrumbAriaLabel': 'Category navigation breadcrumb',
      }

      return dictionary[key] ?? key
    },
  }),
}))

const createFullPageResponse = () => ({
  width: ref('container'),
  pageTitle: ref('Guide Title'),
  metaTitle: ref('Guide Title'),
  metaDescription: ref('Meta description'),
  htmlContent: ref('<p>Guide content</p>'),
  editLink: ref<string | null>(null),
  pending: ref(false),
  error: ref(null),
  refresh: vi.fn(),
})

const mountComponent = async (
  props: Record<string, unknown> = {},
  slots: Record<string, () => ReturnType<typeof h>> = {}
) => {
  const module = await import('./XwikiFullPageRenderer.vue')
  const Component = module.default

  const wrapper = await mountSuspended(Component, {
    props: {
      pageId: 'Main.WebHome',
      ...props,
    },
    slots,
    global: {
      stubs: {
        VContainer: { template: '<div class="v-container"><slot /></div>' },
        VSheet: { template: '<div class="v-sheet"><slot /></div>' },
        VBtn: { template: '<button><slot /></button>' },
        VProgressLinear: { template: '<div class="v-progress-linear" />' },
        VAlert: { template: '<div class="v-alert"><slot /></div>' },
        VImg: {
          name: 'VImg',
          props: { src: { type: String, default: '' } },
          template:
            '<div class="v-img" v-bind="$attrs" :data-src="src"><slot name="placeholder" /></div>',
        },
        VSkeletonLoader: { template: '<div class="v-skeleton-loader" />' },
        NuxtLink: { template: '<a><slot /></a>' },
      },
    },
  })

  await flushPromises()
  return wrapper
}

describe('XwikiFullPageRenderer', () => {
  beforeEach(() => {
    useFullPageMock.mockResolvedValue(createFullPageResponse())
  })

  it('renders hero breadcrumbs when provided', async () => {
    const wrapper = await mountComponent({
      breadcrumbs: [
        { title: 'Home', link: '/' },
        { title: 'Appliances', link: '/appliances' },
        { title: 'Deep dive' },
      ],
    })

    const breadcrumbItems = wrapper.findAll(
      '.category-navigation-breadcrumbs__item'
    )
    expect(breadcrumbItems).toHaveLength(3)

    const firstLink = breadcrumbItems.at(0)?.find('a')
    expect(firstLink?.exists()).toBe(true)
    expect(firstLink?.text()).toBe('Home')

    const lastItem = breadcrumbItems.at(2)
    expect(lastItem?.find('a').exists()).toBe(false)
    expect(lastItem?.text()).toContain('Deep dive')
  })

  it('renders sidebar content when provided through slot', async () => {
    const wrapper = await mountComponent(
      {},
      {
        sidebar: () =>
          h('div', { 'data-test': 'sidebar-slot' }, 'Sidebar content'),
      }
    )

    const layout = wrapper.get('.cms-page__layout')
    expect(layout.classes()).toContain('cms-page__layout--with-sidebar')

    const sidebar = wrapper.get('.cms-page__sidebar')
    expect(sidebar.text()).toContain('Sidebar content')
  })

  it('renders hero media when an image is provided', async () => {
    const wrapper = await mountComponent({
      heroImage: 'https://cdn.example.com/hero.png',
    })

    const media = wrapper.find('.cms-page__hero-media')
    expect(media.exists()).toBe(true)

    const image = wrapper.findComponent({ name: 'VImg' })
    expect(image.exists()).toBe(true)
    expect(image.props('src')).toBe('https://cdn.example.com/hero.png')
  })

  it('enables wide layout variant when requested', async () => {
    const wrapper = await mountComponent(
      { layoutVariant: 'wide' },
      {
        sidebar: () =>
          h('div', { 'data-test': 'sidebar-slot' }, 'Sidebar content'),
      }
    )

    expect(wrapper.classes()).toContain('cms-page--wide')

    const layout = wrapper.get('.cms-page__layout')
    expect(layout.classes()).toContain('cms-page__layout--wide')
  })
})
