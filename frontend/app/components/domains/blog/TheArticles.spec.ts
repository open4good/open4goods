import { mountSuspended } from '@nuxt/test-utils/runtime'
import { flushPromises } from '@vue/test-utils'
import { defineComponent, h, reactive, ref, type ComputedRef, type Ref } from 'vue'
import { beforeEach, describe, expect, test, vi } from 'vitest'
import type { BlogPostDto } from '~~/shared/api-client'

const route = reactive({
  query: {} as Record<string, unknown>,
})

const pushMock = vi.fn<[], Promise<void>>().mockResolvedValue()

vi.mock('#app', () => ({
  useRoute: () => route,
  useRouter: () => ({
    push: pushMock,
  }),
}))

const useHeadMock = vi.fn()
const useSeoMetaMock = vi.fn()
const useRequestURLMock = vi.fn(() => new URL('https://example.com/blog'))

vi.mock('#imports', () => ({
  useHead: useHeadMock,
  useSeoMeta: useSeoMetaMock,
  useRequestURL: useRequestURLMock,
}))

const localeRef = ref('en')

vi.mock('vue-i18n', () => ({
  useI18n: () => ({
    t: (key: string, params: Record<string, any> = {}) => {
      switch (key) {
        case 'blog.pagination.info':
          return `Page ${params.current} of ${params.total} (${params.count} articles)`
        case 'blog.pagination.pageLink':
          return `Go to page ${params.page}`
        case 'blog.list.tagsAriaLabel':
          return 'Filter blog posts by tag'
        case 'blog.list.tagsTitle':
          return 'Tags'
        case 'blog.list.tagsAll':
          return 'All'
        case 'blog.list.tagWithCount':
          return `${params.tag} (${params.count})`
        case 'blog.list.tagsLoading':
          return 'Loading tags'
        case 'blog.list.loading':
          return 'Loading articles...'
        case 'blog.list.readMore':
          return 'Read more'
        case 'common.actions.retry':
          return 'Retry'
        case 'blog.seo.baseTitle':
          return 'Nudger Blog'
        case 'blog.seo.tagTitle':
          return `${params.tag} – Nudger Blog`
        case 'blog.seo.pageTitle':
          return `${params.title} – Page ${params.page}`
        case 'blog.seo.description':
          return 'Discover the latest sustainable appliances insights.'
        case 'blog.seo.tagDescription':
          return `Articles about ${params.tag}.`
        default:
          return key
      }
    },
    locale: localeRef,
  }),
}))

type BlogComposableState = {
  paginatedArticles: Ref<BlogPostDto[] | null>
  loading: Ref<boolean>
  error: Ref<string | null>
  pagination: Ref<{
    page: number
    size: number
    totalElements: number
    totalPages: number
  }>
  fetchArticles: ReturnType<typeof vi.fn>
  tags: Ref<Array<{ name?: string | null; count?: number | null }>>
  selectedTag: Ref<string | null>
  fetchTags: ReturnType<typeof vi.fn>
}

let blogComposableState: BlogComposableState

vi.mock('~/composables/blog/useBlog', () => ({
  useBlog: () => blogComposableState,
}))

const NuxtLinkStub = defineComponent({
  name: 'NuxtLink',
  props: {
    to: { type: [String, Object], default: undefined },
    href: { type: String, default: undefined },
    title: { type: String, default: undefined },
    ariaLabel: { type: String, default: undefined },
    ariaLabelledby: { type: String, default: undefined },
    ariaDescribedby: { type: String, default: undefined },
  },
  setup(props, { slots }) {
    return () =>
      h(
        'a',
        {
          class: 'nuxt-link-stub',
          href: typeof props.to === 'string' ? props.to : props.href ?? '#',
          'data-to':
            typeof props.to === 'string' ? props.to : props.to ? JSON.stringify(props.to) : '',
          title: props.title,
          'aria-label': props.ariaLabel,
          'aria-labelledby': props.ariaLabelledby,
          'aria-describedby': props.ariaDescribedby,
        },
        slots.default?.(),
      )
  },
})

const stubWithSlot = (name: string) =>
  defineComponent({
    name: `${name}Stub`,
    props: {
      modelValue: { type: [String, Number, Array, Object, Boolean], default: undefined },
      value: { type: [String, Number, Object], default: undefined },
      length: { type: Number, default: undefined },
      cols: { type: [String, Number], default: undefined },
      sm: { type: [String, Number], default: undefined },
      md: { type: [String, Number], default: undefined },
      maxWidth: { type: [String, Number], default: undefined },
      color: { type: String, default: undefined },
      variant: { type: String, default: undefined },
      elevation: { type: [String, Number], default: undefined },
      rounded: { type: [String, Number, Boolean], default: undefined },
      tag: { type: String, default: undefined },
      to: { type: [String, Object], default: undefined },
      title: { type: String, default: undefined },
      ariaLabel: { type: String, default: undefined },
      ariaLabelledby: { type: String, default: undefined },
      ariaDescribedby: { type: String, default: undefined },
      alt: { type: String, default: undefined },
      src: { type: String, default: undefined },
      height: { type: [String, Number], default: undefined },
      cover: { type: Boolean, default: undefined },
    },
    emits: ['update:modelValue', 'click'],
    setup(props, { slots, emit }) {
      const onClick = (event: Event) => {
        emit('click', event)
      }

      return () =>
        h(
          'div',
          {
            class: `${name.replace(/([a-z])([A-Z])/g, '$1-$2').toLowerCase()}-stub`,
            'data-model-value': props.modelValue as any,
            'data-value': props.value as any,
            'data-length': props.length as any,
            'data-alt': props.alt,
            'data-src': props.src,
            onClick,
          },
          [slots.default?.(), slots.placeholder?.()].flat().filter(Boolean),
        )
    },
  })

const stubs = {
  NuxtLink: NuxtLinkStub,
  VContainer: stubWithSlot('VContainer'),
  VSheet: stubWithSlot('VSheet'),
  VChipGroup: stubWithSlot('VChipGroup'),
  VChip: stubWithSlot('VChip'),
  VIcon: stubWithSlot('VIcon'),
  VProgressCircular: stubWithSlot('VProgressCircular'),
  VRow: stubWithSlot('VRow'),
  VCol: stubWithSlot('VCol'),
  VCard: stubWithSlot('VCard'),
  VImg: stubWithSlot('VImg'),
  VCardTitle: stubWithSlot('VCardTitle'),
  VCardText: stubWithSlot('VCardText'),
  VCardActions: stubWithSlot('VCardActions'),
  VSpacer: stubWithSlot('VSpacer'),
  VBtn: stubWithSlot('VBtn'),
  VAlert: stubWithSlot('VAlert'),
  VPagination: stubWithSlot('VPagination'),
}

const resetRouteQuery = (query: Record<string, unknown> = {}) => {
  Object.keys(route.query).forEach((key) => {
    delete (route.query as Record<string, unknown>)[key]
  })
  Object.assign(route.query, query)
}

const mountComponent = async () => {
  const componentModule = await import('./TheArticles.vue')
  const TheArticles = componentModule.default

  return mountSuspended(TheArticles, {
    global: {
      stubs,
    },
  })
}

const createArticle = (overrides: Partial<BlogPostDto> = {}): BlogPostDto => ({
  url: 'eco-washer',
  title: 'Energy efficient washer',
  summary: 'A quick look at an energy efficient washer.',
  author: 'Jane Doe',
  image: 'https://example.com/image.jpg',
  createdMs: Date.UTC(2024, 0, 12),
  ...overrides,
})

describe('TheArticles.vue', () => {
  beforeEach(() => {
    pushMock.mockClear()
    useHeadMock.mockClear()
    useSeoMetaMock.mockClear()
    useRequestURLMock.mockClear()
    localeRef.value = 'en'
    resetRouteQuery()

    blogComposableState = {
      paginatedArticles: ref<BlogPostDto[] | null>([]),
      loading: ref(false),
      error: ref<string | null>(null),
      pagination: ref({
        page: 1,
        size: 12,
        totalElements: 0,
        totalPages: 1,
      }),
      fetchArticles: vi.fn().mockResolvedValue(undefined),
      tags: ref<Array<{ name?: string | null; count?: number | null }>>([]),
      selectedTag: ref<string | null>(null),
      fetchTags: vi.fn().mockResolvedValue(undefined),
    }
  })

  test('loads tags and paginated articles using the current route query', async () => {
    resetRouteQuery({ page: '2', tag: ' energy ' })
    blogComposableState.pagination.value.size = 24

    await mountComponent()
    await flushPromises()

    expect(blogComposableState.fetchTags).toHaveBeenCalledTimes(1)
    expect(blogComposableState.fetchArticles).toHaveBeenCalledWith(2, 24, 'energy')
  })

  test('renders article cards with sanitized links and metadata', async () => {
    blogComposableState.paginatedArticles.value = [
      createArticle({ url: 'https://blog.nudger.fr/posts//eco-washer' }),
      createArticle({
        url: 'https://blog.nudger.fr/posts//dryer',
        title: '  ',
        image: 'https://example.com/dryer.jpg',
      }),
    ]
    blogComposableState.pagination.value.totalPages = 2
    blogComposableState.pagination.value.totalElements = 2

    const wrapper = await mountComponent()
    await flushPromises()

    const titleLinks = wrapper.findAll('[data-test="article-title-link"]')
    expect(titleLinks).toHaveLength(2)
    expect(titleLinks[0].text()).toBe('Energy efficient washer')
    expect(titleLinks[0].attributes('data-to')).toBe('/blog/eco-washer')

    const readMoreLink = wrapper.get('[data-test="article-read-more"]')
    expect(readMoreLink.attributes('data-to')).toBe('/blog/eco-washer')

    const renderedHtml = wrapper.html()
    expect(renderedHtml).toContain('data-src="https://example.com/image.jpg"')
    expect(renderedHtml).toContain('data-alt="Energy efficient washer"')
    expect(renderedHtml).toContain('data-alt="Blog article illustration"')

    const paginationInfo = wrapper.get('p[aria-live="polite"]').text()
    expect(paginationInfo).toContain('Page 1 of 2 (2 articles)')
  })

  test('emits router navigation when pagination changes', async () => {
    resetRouteQuery({ tag: 'Eco' })
    blogComposableState.pagination.value.totalPages = 3

    const wrapper = await mountComponent()
    await flushPromises()

    pushMock.mockClear()

    const paginationComponent = wrapper.findComponent(stubs.VPagination)
    paginationComponent.vm.$emit('update:modelValue', 3)
    await flushPromises()

    expect(pushMock).toHaveBeenCalledWith({ query: { tag: 'Eco', page: '3' } })
  })

  test('updates router query when selecting or clearing a tag', async () => {
    resetRouteQuery({ page: '4', tag: 'Eco' })
    blogComposableState.tags.value = [
      { name: 'Eco', count: 3 },
      { name: '  ', count: 2 },
    ]
    blogComposableState.selectedTag.value = 'Eco'

    const wrapper = await mountComponent()
    await flushPromises()

    pushMock.mockClear()

    const chipGroup = wrapper.findComponent(stubs.VChipGroup)
    chipGroup.vm.$emit('update:modelValue', 'Eco')
    await flushPromises()

    expect(pushMock).toHaveBeenCalledWith({ path: '/blog', query: { tag: 'Eco' } })

    pushMock.mockClear()
    chipGroup.vm.$emit('update:modelValue', '__all__')
    await flushPromises()

    expect(pushMock).toHaveBeenCalledWith({ path: '/blog', query: {} })
  })

  test('presents loading and error states appropriately', async () => {
    blogComposableState.loading.value = true

    let wrapper = await mountComponent()
    await flushPromises()

    expect(wrapper.get('[role="status"]').text()).toContain('Loading articles...')

    blogComposableState.loading.value = false
    blogComposableState.error.value = 'Failed to fetch articles'
    wrapper = await mountComponent()
    await flushPromises()

    expect(wrapper.get('.valert-stub').text()).toContain('Failed to fetch articles')
    expect(wrapper.get('.vbtn-stub').text()).toContain('Retry')
  })

  test('exposes SEO helpers combining titles, summaries and canonical URL', async () => {
    resetRouteQuery({ page: '2', tag: 'Eco' })
    blogComposableState.pagination.value.page = 2
    blogComposableState.pagination.value.totalPages = 3
    blogComposableState.pagination.value.totalElements = 5
    blogComposableState.selectedTag.value = 'Eco'
    blogComposableState.paginatedArticles.value = [
      createArticle({
        summary: 'Detailed insights about washing efficiently with eco programmes.',
      }),
    ]

    const wrapper = await mountComponent()
    await flushPromises()

    const exposed =
      ((wrapper.vm as { $exposed?: Record<string, unknown> }).$exposed ??
      (wrapper.vm as { $?: { exposed?: Record<string, unknown> } }).$?.exposed ??
      {}) as {
        pageSeoTitle: ComputedRef<string>
        seoDescription: ComputedRef<string>
        canonicalUrl: ComputedRef<string | undefined>
        structuredData: ComputedRef<Record<string, unknown>>
      }

    expect(exposed.pageSeoTitle.value).toBe('Eco – Nudger Blog – Page 2')
    expect(exposed.seoDescription.value).toBe(
      'Articles about Eco. Detailed insights about washing efficiently with eco programmes.',
    )
    expect(exposed.canonicalUrl.value).toMatch(/\/blog\?page=2&tag=Eco$/)

    const structuredData = exposed.structuredData.value
    expect(structuredData).toMatchObject({
      '@type': 'CollectionPage',
      name: 'Eco – Nudger Blog – Page 2',
      description: 'Articles about Eco. Detailed insights about washing efficiently with eco programmes.',
      inLanguage: 'en',
    })
    expect(structuredData.url).toMatch(/\/blog\?page=2&tag=Eco$/)

    const [firstArticle] = (structuredData.hasPart ?? []) as Array<Record<string, unknown>>
    expect(firstArticle).toMatchObject({
      '@type': 'BlogPosting',
      headline: 'Energy efficient washer',
      description: 'Detailed insights about washing efficiently with eco programmes.',
    })
    expect(firstArticle?.url).toMatch(/\/blog\/eco-washer$/)
  })
})
