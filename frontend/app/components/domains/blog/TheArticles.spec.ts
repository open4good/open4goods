import { mountSuspended } from '@nuxt/test-utils/runtime'
import { defineComponent, h, nextTick, reactive, ref } from 'vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'

const useHeadMock = vi.fn()
const useSeoMetaMock = vi.fn()
let requestUrlValue = new URL('https://example.com/blog')

vi.mock('#imports', () => ({
  useHead: (input: unknown) => {
    useHeadMock(input)
  },
  useSeoMeta: (input: unknown) => {
    useSeoMetaMock(input)
  },
  useRequestURL: () => requestUrlValue,
}))

const localeRef = ref('en')
const translations: Record<string, (params?: Record<string, any>) => string> = {
  'blog.pagination.info': (params = {}) =>
    `Page ${params.current} of ${params.total} – ${params.count} articles`,
  'blog.pagination.pageLink': (params = {}) => `Go to page ${params.page}`,
  'blog.pagination.ariaLabel': () => 'Pagination',
  'blog.seo.baseTitle': () => 'Blog',
  'blog.seo.tagTitle': (params = {}) => `Blog – ${params.tag}`,
  'blog.seo.pageTitle': (params = {}) => `${params.title} – Page ${params.page}`,
  'blog.seo.description': () => 'Latest news and tutorials.',
  'blog.seo.tagDescription': (params = {}) => `Articles about ${params.tag}.`,
  'blog.list.tagsAriaLabel': () => 'Filter by tag',
  'blog.list.tagsTitle': () => 'Popular tags',
  'blog.list.tagsAll': () => 'All articles',
  'blog.list.tagWithCount': (params = {}) => `${params.tag} (${params.count})`,
  'blog.list.tagsLoading': () => 'Loading tags…',
  'blog.list.loading': () => 'Loading articles…',
  'blog.list.readMore': () => 'Read more',
  'common.actions.retry': () => 'Retry',
}

vi.mock('vue-i18n', () => ({
  useI18n: () => ({
    t: (key: string, params: Record<string, unknown> = {}) =>
      translations[key]?.(params) ?? key,
    locale: localeRef,
  }),
}))

const routeQuery = reactive<Record<string, any>>({})
const routeMock = reactive({ query: routeQuery })
let routerPushMock: ReturnType<typeof vi.fn>

vi.mock('#app', () => ({
  useRoute: () => routeMock,
  useRouter: () => ({
    push: (...args: unknown[]) => routerPushMock(...args),
  }),
}))

type BlogState = ReturnType<typeof createBlogState>
let blogState: BlogState

function createBlogState() {
  const paginatedArticles = ref([
    {
      url: 'https://blog.example.com/articles/first-post',
      title: 'First post',
      summary: 'Introductory article',
      author: 'Jane Doe',
      image: 'https://cdn.example.com/first.jpg',
      createdMs: Date.UTC(2024, 0, 10),
    },
  ])
  const loading = ref(false)
  const error = ref<string | null>(null)
  const pagination = ref({
    page: 1,
    size: 6,
    totalElements: 12,
    totalPages: 2,
  })
  const tags = ref<any[]>([])
  const selectedTag = ref<string | null>(null)
  const fetchArticles = vi.fn(async () => undefined)
  const fetchTags = vi.fn(async () => {
    tags.value = []
  })

  return {
    paginatedArticles,
    loading,
    error,
    pagination,
    fetchArticles,
    tags,
    selectedTag,
    fetchTags,
  }
}

vi.mock('~/composables/blog/useBlog', () => ({
  useBlog: () => blogState,
}))

const NuxtLinkStub = defineComponent({
  name: 'NuxtLinkStub',
  props: {
    to: { type: [String, Object], default: undefined },
  },
  setup(props, { slots, attrs }) {
    return () =>
      h(
        'a',
        {
          ...attrs,
          href: typeof props.to === 'string' ? props.to : '#',
          'data-to': typeof props.to === 'object' ? JSON.stringify(props.to) : undefined,
        },
        slots.default?.(),
      )
  },
})

const createSimpleStub = (name: string, element = 'div') =>
  defineComponent({
    name,
    inheritAttrs: false,
    setup(_, { slots, attrs }) {
      return () => h(element, attrs, slots.default?.())
    },
  })

const VCardStub = defineComponent({
  name: 'VCardStub',
  inheritAttrs: false,
  props: {
    tag: { type: String, default: 'div' },
    to: { type: [String, Object], default: undefined },
  },
  setup(props, { slots, attrs }) {
    const data = { ...attrs }
    if (props.to !== undefined) {
      data['data-to'] = typeof props.to === 'string' ? props.to : JSON.stringify(props.to)
    }

    return () => h(props.tag || 'div', data, slots.default?.())
  },
})

const VBtnStub = defineComponent({
  name: 'VBtnStub',
  inheritAttrs: false,
  emits: ['click'],
  props: {
    type: { type: String, default: 'button' },
  },
  setup(props, { slots, attrs, emit }) {
    return () =>
      h(
        'button',
        {
          ...attrs,
          type: props.type,
          onClick: (event: MouseEvent) => emit('click', event),
        },
        slots.default?.(),
      )
  },
})

const VImgStub = defineComponent({
  name: 'VImgStub',
  inheritAttrs: false,
  props: {
    src: { type: String, default: '' },
    alt: { type: String, default: '' },
  },
  setup(props, { slots, attrs }) {
    return () =>
      h(
        'figure',
        attrs,
        [
          h('img', { src: props.src, alt: props.alt }),
          slots.placeholder?.(),
          slots.default?.(),
        ],
      )
  },
})

const VIconStub = defineComponent({
  name: 'VIconStub',
  inheritAttrs: false,
  props: {
    icon: { type: String, default: '' },
  },
  setup(props, { slots, attrs }) {
    return () =>
      h(
        'span',
        {
          ...attrs,
          'data-icon': props.icon,
        },
        slots.default?.(),
      )
  },
})

const VChipStub = defineComponent({
  name: 'VChipStub',
  inheritAttrs: false,
  emits: ['click'],
  props: {
    value: { type: [String, Number, Boolean], default: undefined },
  },
  setup(props, { slots, attrs, emit }) {
    return () =>
      h(
        'button',
        {
          ...attrs,
          type: 'button',
          'data-value': props.value,
          onClick: (event: MouseEvent) => emit('click', event),
        },
        slots.default?.(),
      )
  },
})

const VChipGroupStub = defineComponent({
  name: 'VChipGroupStub',
  inheritAttrs: false,
  emits: ['update:modelValue'],
  props: {
    modelValue: { type: [String, Number, Boolean], default: undefined },
  },
  setup(props, { slots, attrs, emit }) {
    return () =>
      h(
        'div',
        {
          ...attrs,
          'data-chip-group-value': props.modelValue,
          onClick: (event: MouseEvent & { target: HTMLElement }) => {
            const target = event.target as HTMLElement
            const value = target?.dataset?.value
            if (value) {
              emit('update:modelValue', value)
            }
          },
        },
        slots.default?.(),
      )
  },
})

const VPaginationStub = defineComponent({
  name: 'VPaginationStub',
  inheritAttrs: false,
  emits: ['update:modelValue'],
  props: {
    modelValue: { type: Number, default: 1 },
  },
  setup(props, { slots, attrs, emit }) {
    return () =>
      h(
        'nav',
        {
          ...attrs,
        },
        [
          slots.default?.(),
          h(
            'button',
            {
              type: 'button',
              'data-test': 'emit-next-page',
              onClick: () => emit('update:modelValue', props.modelValue + 1),
            },
            'next',
          ),
        ],
      )
  },
})

const stubs = {
  NuxtLink: NuxtLinkStub,
  VContainer: createSimpleStub('VContainerStub'),
  VSheet: createSimpleStub('VSheetStub'),
  VIcon: VIconStub,
  VChipGroup: VChipGroupStub,
  VChip: VChipStub,
  VProgressCircular: createSimpleStub('VProgressCircularStub', 'span'),
  VBtn: VBtnStub,
  VAlert: createSimpleStub('VAlertStub'),
  VRow: createSimpleStub('VRowStub'),
  VCol: createSimpleStub('VColStub'),
  VCard: VCardStub,
  VImg: VImgStub,
  VCardTitle: createSimpleStub('VCardTitleStub', 'h3'),
  VCardText: createSimpleStub('VCardTextStub'),
  VCardActions: createSimpleStub('VCardActionsStub'),
  VSpacer: createSimpleStub('VSpacerStub'),
  VPagination: VPaginationStub,
}

const flushAll = async () => {
  await nextTick()
  await nextTick()
}

describe('TheArticles.vue', () => {
  const mountComponent = async () => {
    const module = await import('./TheArticles.vue')
    const component = module.default

    return mountSuspended(component, {
      global: {
        stubs,
      },
    })
  }

  beforeEach(() => {
    blogState = createBlogState()
    localeRef.value = 'en'
    routerPushMock = vi.fn(async () => undefined)
    requestUrlValue = new URL('https://example.com/blog')

    Object.keys(routeQuery).forEach((key) => {
      delete routeQuery[key]
    })
  })

  it('loads tags and articles using sanitized route parameters on mount', async () => {
    Object.assign(routeQuery, { page: ' 2 ', tag: ' Nuxt ' })
    blogState.pagination.value.size = 5
    blogState.fetchTags.mockImplementation(async () => {
      blogState.tags.value = [{ name: 'Nuxt', count: 2 }]
    })

    const wrapper = await mountComponent()
    await flushAll()

    expect(blogState.fetchTags).toHaveBeenCalled()
    const lastCall = blogState.fetchArticles.mock.calls.at(-1) ?? []
    expect(lastCall[0]).toBe(2)
    expect(lastCall[1]).toBe(5)
    expect(lastCall[2]).toBe('Nuxt')

    blogState.pagination.value.page = 3
    await flushAll()

    expect((wrapper.vm as any).currentPage.value).toBe(3)
  })

  it('renders article cards with metadata and navigates when clicking read more', async () => {
    blogState.tags.value = [
      { name: 'Nuxt', count: 3 },
      { name: 'Vue', count: 0 },
    ]
    blogState.paginatedArticles.value = [
      {
        url: 'https://blog.example.com/articles/advanced-routing',
        title: 'Advanced routing',
        summary: 'How to handle nested routes in Nuxt.',
        author: 'John Smith',
        image: 'https://cdn.example.com/routing.png',
        createdMs: Date.UTC(2024, 1, 5),
      },
      {
        url: 'invalid-url',
        title: '',
        summary: 'Short summary only.',
        author: null,
        image: null,
        createdMs: undefined,
      },
    ]

    const wrapper = await mountComponent()
    await flushAll()

    const items = wrapper.findAll('[role="listitem"]')
    expect(items).toHaveLength(2)

    const firstTitle = wrapper.get('h3').text()
    expect(firstTitle).toBe('Advanced routing')

    const timeElement = wrapper.get('time')
    expect(timeElement.attributes('datetime')).toBe(new Date(Date.UTC(2024, 1, 5)).toISOString())

    const image = wrapper.get('img')
    expect(image.attributes('alt')).toBe('Advanced routing')

    const readMoreButton = wrapper.get('button[aria-label*="Read more"]')
    await readMoreButton.trigger('click')

    expect(routerPushMock).toHaveBeenCalledWith('/blog/advanced-routing')
  })

  it('displays a loading state while articles are being fetched', async () => {
    blogState.loading.value = true

    const wrapper = await mountComponent()
    await flushAll()

    const statusRegion = wrapper.get('[role="status"]')
    expect(statusRegion.text()).toContain('Loading articles…')
  })

  it('shows the error state and retries when requested', async () => {
    blogState.error.value = 'Something went wrong'
    blogState.loading.value = false

    const wrapper = await mountComponent()
    await flushAll()

    expect(wrapper.text()).toContain('Something went wrong')

    const retryButton = wrapper.get('button')
    blogState.fetchArticles.mockClear()
    await retryButton.trigger('click')

    expect(blogState.fetchArticles).toHaveBeenCalledTimes(1)
  })

  it('updates the route when pagination changes', async () => {
    Object.assign(routeQuery, { tag: 'Nuxt' })

    const wrapper = await mountComponent()
    await flushAll()

    await (wrapper.vm as any).handlePageChange(3)
    expect(routerPushMock).toHaveBeenLastCalledWith({ query: { tag: 'Nuxt', page: '3' } })

    routerPushMock.mockClear()
    await (wrapper.vm as any).handlePageChange(1)
    expect(routerPushMock).toHaveBeenLastCalledWith({ query: { tag: 'Nuxt' } })
  })

  it('navigates through tag selection via the computed setter', async () => {
    blogState.tags.value = [
      { name: 'Nuxt', count: 2 },
      { name: 'Vue', count: 1 },
    ]
    Object.assign(routeQuery, { page: '4' })

    const wrapper = await mountComponent()
    await flushAll()

    ;((wrapper.vm as any).tagGroupValue as { value: string }).value = 'Nuxt'
    await flushAll()

    expect(routerPushMock).toHaveBeenLastCalledWith({ path: '/blog', query: { tag: 'Nuxt' } })

    routerPushMock.mockClear()
    ;((wrapper.vm as any).tagGroupValue as { value: string }).value = (wrapper.vm as any).allTagValue
    await flushAll()

    expect(routerPushMock).toHaveBeenLastCalledWith({ path: '/blog', query: {} })
  })

  it('computes canonical URL, SEO metadata and structured data', async () => {
    Object.assign(routeQuery, { page: '2', tag: 'Nuxt' })
    requestUrlValue = new URL('https://example.com/blog?foo=bar')
    blogState.selectedTag.value = 'Nuxt'
    blogState.pagination.value.page = 2
    blogState.paginatedArticles.value = [
      {
        url: 'https://blog.example.com/articles/nuxt-seo',
        title: 'Nuxt SEO best practices',
        summary: 'Learn how to optimise SEO in Nuxt applications.',
        author: 'Alex Doe',
        image: 'https://cdn.example.com/seo.png',
        createdMs: Date.UTC(2024, 2, 12),
      },
    ]

    const wrapper = await mountComponent()
    await flushAll()

    const canonical = (wrapper.vm as any).canonicalUrl
    expect(canonical).toBeDefined()

    const canonicalUrl = new URL(canonical)
    expect(canonicalUrl.pathname).toBe('/blog')
    expect(canonicalUrl.searchParams.get('page')).toBe('2')
    expect(canonicalUrl.searchParams.get('tag')).toBe('Nuxt')

    const structuredData = (wrapper.vm as any).structuredData
    expect(structuredData.url).toBe(canonical)
    expect(structuredData.name).toBe('Blog – Nuxt – Page 2')
    expect(structuredData.description).toBe(
      'Articles about Nuxt. Learn how to optimise SEO in Nuxt applications.',
    )
    expect(structuredData.hasPart).toHaveLength(1)
    expect(structuredData.hasPart[0].image?.[0]).toBe('https://cdn.example.com/seo.png')
    const expectedArticleUrl = new URL('/blog/nuxt-seo', canonical).toString()
    expect(structuredData.hasPart[0].url).toBe(expectedArticleUrl)
  })

  it('sanitises article links and slugs correctly', async () => {
    const wrapper = await mountComponent()
    await flushAll()

    const vm = wrapper.vm as any

    expect(vm.buildArticleLink('https://blog.example.com/articles/awesome')).toBe('/blog/awesome')
    expect(vm.buildArticleLink('   /blog/awesome  ')).toBe('/blog/awesome')
    expect(vm.buildArticleLink('')).toBeUndefined()
    expect(vm.buildArticleLink(null)).toBeUndefined()

    expect(vm.buildArticleImageAlt('   ')).toBe('Blog article illustration')
    expect(vm.buildArticleImageAlt('Great title')).toBe('Great title')
  })
})
