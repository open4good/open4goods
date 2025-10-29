import { mountSuspended, mockNuxtImport } from '@nuxt/test-utils/runtime'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent, h, ref } from 'vue'
import { flushPromises } from '@vue/test-utils'
import type { VerticalConfigFullDto } from '~~/shared/api-client'

const selectCategoryBySlugMock = vi.fn<[], Promise<VerticalConfigFullDto>>()
const routerPushMock = vi.fn()
const routerReplaceMock = vi.fn()
const useSeoMetaMock = vi.hoisted(() => vi.fn())
const mdAndDown = ref(false)
const localeRef = ref('en-US')

const messages: Record<string, string> = {
  'siteIdentity.siteName': 'Nudger',
  'category.guidePage.breadcrumbs.ariaLabel': 'Guide breadcrumb navigation',
  'category.guidePage.navigation.ariaLabel': 'Guide sections navigation',
  'category.guidePage.navigation.title': 'More guides',
  'category.guidePage.cta.title': 'Back to {category}',
  'category.guidePage.cta.description': 'Return to the {category} overview to browse every resource.',
  'category.guidePage.cta.button': 'Back to {category}',
  'category.guidePage.cta.imageAlt': 'Illustration for {category}',
}

const translate = (key: string, params: Record<string, unknown> = {}) => {
  const template = messages[key] ?? key
  return template.replace(/\{(\w+)\}/g, (_, match) => String(params[match] ?? ''))
}

vi.mock('vue-i18n', () => ({
  useI18n: () => ({
    t: (key: string, params: Record<string, unknown> = {}) => translate(key, params),
    locale: localeRef,
  }),
}))

const route = {
  params: { categorySlug: 'televisions', guideSlug: ['sustainability', 'best-practices'] },
  fullPath: '/televisions/sustainability/best-practices',
}

mockNuxtImport('useRoute', () => () => route)
mockNuxtImport('useRouter', () => () => ({ push: routerPushMock, replace: routerReplaceMock }))
mockNuxtImport('useSeoMeta', () => useSeoMetaMock)
mockNuxtImport('createError', () => (input: { statusMessage?: string } & Record<string, unknown>) => {
  const error = new Error(input?.statusMessage ?? 'Error')
  Object.assign(error, input)
  return error
})

vi.mock('vuetify', () => ({
  useDisplay: () => ({ mdAndDown }),
}))

vi.mock('~/components/category/navigation/CategoryNavigationBreadcrumbs.vue', () => ({
  default: defineComponent({
    name: 'CategoryNavigationBreadcrumbsStub',
    props: {
      items: { type: Array as () => Array<{ title: string; link?: string }>, default: () => [] },
      ariaLabel: { type: String, default: '' },
    },
    setup(props) {
      return () =>
        h(
          'nav',
          {
            'data-test': 'breadcrumbs',
            'data-aria-label': props.ariaLabel,
          },
          (props.items as Array<{ title: string; link?: string }>).map((item, index, all) =>
            item.link
              ? h(
                  'a',
                  {
                    'data-breadcrumb-item': 'link',
                    href: item.link,
                  },
                  item.title,
                )
              : h(
                  'span',
                  {
                    'data-breadcrumb-item': 'current',
                    'data-current': index === all.length - 1 ? 'true' : 'false',
                  },
                  item.title,
                ),
          ),
        )
    },
  }),
}))

vi.mock('~/components/shared/ui/StickySectionNavigation.vue', () => ({
  default: defineComponent({
    name: 'StickySectionNavigationStub',
    props: {
      sections: { type: Array as () => Array<{ id: string; label: string }>, default: () => [] },
      activeSection: { type: String, default: '' },
      orientation: { type: String, default: 'vertical' },
      ariaLabel: { type: String, default: '' },
    },
    emits: ['navigate'],
    setup(props, { emit }) {
      return () =>
        h(
          'nav',
          {
            'data-test': 'sticky-nav',
            'data-active': props.activeSection,
            'data-orientation': props.orientation,
            'data-sections': JSON.stringify(props.sections),
            'data-aria-label': props.ariaLabel,
          },
          (props.sections as Array<{ id: string; label: string }>).map((section) =>
            h(
              'button',
              {
                type: 'button',
                'data-id': section.id,
                onClick: () => emit('navigate', section.id),
              },
              section.label,
            ),
          ),
        )
    },
  }),
}))

vi.mock('~/components/cms/XwikiFullPageRenderer.vue', () => ({
  default: defineComponent({
    name: 'XwikiFullPageRendererStub',
    props: { pageId: { type: String, default: '' } },
    setup(props) {
      return () => h('article', { 'data-test': 'xwiki-renderer' }, `page:${props.pageId}`)
    },
  }),
}))

vi.mock('~/composables/categories/useCategories', () => ({
  useCategories: () => ({
    selectCategoryBySlug: selectCategoryBySlugMock,
  }),
}))

const categoryFixture: VerticalConfigFullDto = {
  id: 'tv',
  verticalHomeTitle: 'Televisions',
  verticalHomeDescription: 'Eco guides for televisions.',
  imageSmall: 'https://example.com/images/tv-small.jpg',
  breadCrumb: [
    { title: 'Electronics', link: '/electronics' },
    { title: 'Televisions', link: '/televisions' },
  ],
  wikiPages: [
    {
      title: 'Getting started with energy efficient TVs',
      verticalUrl: 'getting-started',
      wikiUrl: 'pages:tv/getting-started',
    },
    {
      title: 'Sustainability guide for televisions and energy conscious households worldwide',
      verticalUrl: 'sustainability/best-practices',
      wikiUrl: 'pages:tv/sustainability',
    },
  ],
} as unknown as VerticalConfigFullDto

const vuetifyStubs = {
  'v-container': defineComponent({
    name: 'VContainerStub',
    setup(_props, { slots, attrs }) {
      return () => h('div', { class: 'v-container-stub', ...attrs }, slots.default?.())
    },
  }),
  'v-card': defineComponent({
    name: 'VCardStub',
    setup(_props, { slots, attrs }) {
      return () => h('div', { class: 'v-card-stub', ...attrs }, slots.default?.())
    },
  }),
  'v-btn': defineComponent({
    name: 'VBtnStub',
    props: { to: { type: [String, Object], default: undefined } },
    setup(props, { slots, attrs }) {
      return () =>
        h(
          'button',
          {
            class: 'v-btn-stub',
            type: 'button',
            ...attrs,
            'data-to': typeof props.to === 'string' ? props.to : attrs['data-to'],
          },
          slots.default?.(),
        )
    },
  }),
  'v-icon': defineComponent({
    name: 'VIconStub',
    props: { icon: { type: String, default: '' } },
    setup(props, { slots, attrs }) {
      return () => h('span', { class: 'v-icon-stub', 'data-icon': props.icon, ...attrs }, slots.default?.())
    },
  }),
  'v-img': defineComponent({
    name: 'VImgStub',
    props: { src: { type: String, default: '' }, alt: { type: String, default: '' } },
    setup(props, { attrs }) {
      return () => h('img', { class: 'v-img-stub', src: props.src, alt: props.alt, ...attrs })
    },
  }),
}

const truncate = (value: string, limit = 54) =>
  value.length > limit ? `${value.slice(0, limit - 1)}…` : value

const mountGuidePage = async () => {
  selectCategoryBySlugMock.mockResolvedValueOnce(categoryFixture)
  const component = (await import('./[...guideSlug].vue')).default
  const wrapper = await mountSuspended(component, {
    global: {
      stubs: vuetifyStubs,
    },
  })
  await flushPromises()
  return wrapper
}

describe('Category wiki guide page', () => {
  beforeEach(() => {
    selectCategoryBySlugMock.mockReset()
    routerPushMock.mockReset()
    routerReplaceMock.mockReset()
    useSeoMetaMock.mockReset()
    mdAndDown.value = false
  })

  it('renders breadcrumb trail with truncated guide leaf', async () => {
    const wrapper = await mountGuidePage()

    const breadcrumb = wrapper.get('[data-test="breadcrumbs"]')
    expect(breadcrumb.attributes('data-aria-label')).toBe('Guide breadcrumb navigation')

    const items = breadcrumb.findAll('[data-breadcrumb-item]')
    expect(items).toHaveLength(3)

    expect(items[0].attributes('href')).toBe('/electronics')
    expect(items[1].attributes('href')).toBe('/televisions')

    const current = breadcrumb.find('[data-breadcrumb-item="current"]')
    const expectedTitle = truncate(
      'Sustainability guide for televisions and energy conscious households worldwide',
    )
    expect(current.text()).toBe(expectedTitle)
  })

  it('provides sticky navigation for other guides and routes when selecting one', async () => {
    const wrapper = await mountGuidePage()

    const stickyNav = wrapper.get('[data-test="guide-navigation"]')
    expect(stickyNav.attributes('data-aria-label')).toBe('Guide sections navigation')
    expect(stickyNav.attributes('data-orientation')).toBe('vertical')

    const sections = JSON.parse(stickyNav.attributes('data-sections') ?? '[]') as Array<{
      id: string
      label: string
    }>
    expect(sections).toHaveLength(2)
    expect(sections[0]).toMatchObject({ id: 'getting-started' })
    expect(stickyNav.attributes('data-active')).toBe('sustainability/best-practices')

    await stickyNav.get('button[data-id="getting-started"]').trigger('click')
    expect(routerPushMock).toHaveBeenCalledWith({ path: '/televisions/getting-started' })
  })

  it('displays a CTA linking back to the category overview', async () => {
    const wrapper = await mountGuidePage()

    const cta = wrapper.get('[data-test="guide-cta"]')
    expect(cta.text()).toContain('Back to Televisions')

    const button = cta.get('[data-test="guide-cta-button"]')
    expect(button.text()).toContain('Back to Televisions')
    expect(button.attributes('data-to')).toBe('/televisions')
  })

  it('switches navigation orientation on smaller viewports', async () => {
    mdAndDown.value = true
    const wrapper = await mountGuidePage()

    const stickyNav = wrapper.get('[data-test="guide-navigation"]')
    expect(stickyNav.attributes('data-orientation')).toBe('horizontal')
  })
})
