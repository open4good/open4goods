import { mountSuspended } from '@nuxt/test-utils/runtime'
import { flushPromises } from '@vue/test-utils'
import { computed, reactive, ref, nextTick } from 'vue'
import { beforeEach, describe, expect, test, vi } from 'vitest'

import type { BlogTagDto } from '~~/shared/api-client'

type BlogArticle = {
  url?: string | null
  title?: string | null
  summary?: string | null
  author?: string | null
  image?: string | null
  createdMs?: number | null
}

const useHeadMock = vi.fn()
const useSeoMetaMock = vi.fn()
const useRequestURLMock = vi.fn()

const routerPushMock = vi.fn()

const route = reactive({
  query: reactive<{ page?: string; tag?: string | null }>({
    page: '2',
    tag: ' Tech ',
  }),
})

vi.mock('#app', () => ({
  useRoute: () => route,
  useRouter: () => ({
    push: routerPushMock,
  }),
}))

vi.mock('#imports', () => ({
  useHead: useHeadMock,
  useSeoMeta: useSeoMetaMock,
  useRequestURL: () => useRequestURLMock(),
}))

const translate = (key: string, params: Record<string, unknown> = {}) => {
  switch (key) {
    case 'blog.list.tagsTitle':
      return 'Browse by tag'
    case 'blog.list.tagsAriaLabel':
      return 'Blog tags'
    case 'blog.list.tagsAll':
      return 'All articles'
    case 'blog.list.tagsLoading':
      return 'Loading tags...'
    case 'blog.list.loading':
      return 'Loading articles...'
    case 'blog.list.readMore':
      return 'Read more'
    case 'blog.list.tagWithCount':
      return `${params.tag} (${params.count})`
    case 'blog.pagination.info':
      return `Page ${params.current} of ${params.total} (${params.count} articles)`
    case 'blog.pagination.ariaLabel':
      return 'Blog pagination'
    case 'blog.pagination.pageLink':
      return `Go to page ${params.page}`
    case 'blog.seo.baseTitle':
      return 'Blog'
    case 'blog.seo.tagTitle':
      return `Blog - ${params.tag}`
    case 'blog.seo.pageTitle':
      return `${params.title} - Page ${params.page}`
    case 'blog.seo.description':
      return 'Latest updates from our team.'
    case 'blog.seo.tagDescription':
      return `Articles about ${params.tag}`
    case 'common.actions.retry':
      return 'Retry'
    default:
      return key
  }
}

vi.mock('vue-i18n', () => ({
  useI18n: () => ({
    t: translate,
    locale: ref('en'),
  }),
}))

const createBlogState = () => {
  const articlesSource = ref<BlogArticle[]>([
    {
      url: 'https://example.com/blog/first-article',
      title: 'First article',
      summary: 'A short introduction to the blog.',
      author: 'Alice',
      image: 'https://example.com/image-1.jpg',
      createdMs: Date.UTC(2024, 0, 10),
    },
    {
      url: 'second-article',
      title: 'Second article',
      summary: 'Another piece of content.',
      author: 'Bob',
      image: null,
      createdMs: Date.UTC(2024, 0, 12),
    },
  ])

  const articles = ref<BlogArticle[]>(articlesSource.value)
  const tagsSource = ref<BlogTagDto[]>([
    { name: ' Tech ', count: 5 },
    { name: 'Nuxt', count: 2 },
  ])
  const tags = ref<BlogTagDto[]>([])
  const selectedTag = ref<string | null>(null)
  const pagination = ref({
    page: 1,
    size: 10,
    totalPages: 3,
    totalElements: articlesSource.value.length,
  })
  const loading = ref(false)
  const error = ref<string | null>(null)

  const fetchArticlesMock = vi.fn(
    async (
      page = pagination.value.page,
      size = pagination.value.size,
      tag = selectedTag.value,
    ) => {
      pagination.value = {
        ...pagination.value,
        page,
        size,
        totalElements: articlesSource.value.length,
      }
      selectedTag.value = (tag ?? null) as string | null
      articles.value = articlesSource.value
    },
  )

  const fetchTagsMock = vi.fn(async () => {
    tags.value = tagsSource.value
  })

  return {
    paginatedArticles: computed(() => articles.value),
    loading,
    error,
    pagination,
    fetchArticles: fetchArticlesMock,
    tags,
    selectedTag,
    fetchTags: fetchTagsMock,
    changePage: vi.fn(),
    __articles: articlesSource,
    __tags: tagsSource,
  }
}

type BlogState = ReturnType<typeof createBlogState>

const blogState: BlogState = createBlogState()

const resetBlogState = () => {
  const fresh = createBlogState()
  Object.assign(blogState, fresh)
}

vi.mock('~/composables/blog/useBlog', () => ({
  useBlog: () => blogState,
}))

const mountComponent = async () => {
  const componentModule = await import('./TheArticles.vue')
  const TheArticles = componentModule.default

  return mountSuspended(TheArticles, {
    global: {
      stubs: {
        NuxtLink: {
          name: 'NuxtLinkStub',
          props: ['to', 'href', 'title', 'ariaLabel', 'ariaDescribedby', 'ariaLabelledby'],
          template:
            "<a class=\"nuxt-link-stub\" :href='typeof to === \"string\" ? to : \"#\"'><slot /></a>",
        },
        VContainer: {
          name: 'VContainerStub',
          template: '<div class="v-container"><slot /></div>',
        },
        VSheet: {
          name: 'VSheetStub',
          props: ['ariaLabel', 'ariaBusy', 'ariaControls'],
          template: '<section class="v-sheet"><slot /></section>',
        },
        VChipGroup: {
          name: 'VChipGroupStub',
          props: ['modelValue'],
          emits: ['update:modelValue'],
          template: '<div class="v-chip-group"><slot /></div>',
        },
        VChip: {
          name: 'VChipStub',
          props: ['value'],
          emits: ['click'],
          template: '<button class="v-chip" type="button" @click="$emit(\'click\')"><slot /></button>',
        },
        VIcon: {
          name: 'VIconStub',
          template: '<span class="v-icon"><slot /></span>',
        },
        VProgressCircular: {
          name: 'VProgressCircularStub',
          template: '<span class="v-progress-circular" aria-hidden="true"></span>',
        },
        VRow: {
          name: 'VRowStub',
          template: '<div class="v-row"><slot /></div>',
        },
        VCol: {
          name: 'VColStub',
          template: '<div class="v-col"><slot /></div>',
        },
        VCard: {
          name: 'VCardStub',
          template: '<article class="v-card"><slot /></article>',
        },
        VImg: {
          name: 'VImgStub',
          props: ['src', 'alt'],
          template: '<img class="v-img" :src="src" :alt="alt" />',
        },
        VCardTitle: {
          name: 'VCardTitleStub',
          template: '<header class="v-card-title"><slot /></header>',
        },
        VCardText: {
          name: 'VCardTextStub',
          template: '<div class="v-card-text"><slot /></div>',
        },
        VCardActions: {
          name: 'VCardActionsStub',
          template: '<footer class="v-card-actions"><slot /></footer>',
        },
        VSpacer: {
          name: 'VSpacerStub',
          template: '<span class="v-spacer"></span>',
        },
        VBtn: {
          name: 'VBtnStub',
          emits: ['click'],
          template: '<button class="v-btn" type="button" @click="$emit(\'click\')"><slot /></button>',
        },
        VAlert: {
          name: 'VAlertStub',
          template: '<div class="v-alert"><slot /></div>',
        },
        VPagination: {
          name: 'VPaginationStub',
          props: ['modelValue'],
          emits: ['update:modelValue'],
          template:
            '<div class="v-pagination"><button type="button" data-test="pagination-next" @click="$emit(\'update:modelValue\', modelValue + 1)">Next</button></div>',
        },
      },
    },
  })
}

describe('TheArticles.vue', () => {
  beforeEach(() => {
    resetBlogState()
    routerPushMock.mockReset()
    useRequestURLMock.mockReset()
    useRequestURLMock.mockReturnValue(new URL('http://localhost:3000/blog?page=2&tag=Tech'))
    route.query.page = '2'
    route.query.tag = ' Tech '
  })

  test('loads articles based on route parameters and renders cards', async () => {
    blogState.__tags.value = [
      { name: ' Tech ', count: 5 },
      { name: 'Nuxt', count: 2 },
    ]

    const wrapper = await mountComponent()
    await flushPromises()

    expect(blogState.fetchTags).toHaveBeenCalledTimes(1)
    expect(blogState.fetchArticles).toHaveBeenCalledWith(2, 10, 'Tech')

    const titleLinks = wrapper.findAll('[data-test="article-title-link"]')
    expect(titleLinks).toHaveLength(2)
    expect(titleLinks[0]?.text()).toBe('First article')

    const readMoreLink = wrapper.get('[data-test="article-read-more"]')
    expect(readMoreLink.text()).toBe('Read more')

    const imageLink = wrapper.get('[data-test="article-image-link"] img')
    expect(imageLink.attributes('alt')).toBe('First article')

    const vm = wrapper.vm as unknown as {
      canonicalUrl: string | undefined
      structuredData: Record<string, unknown>
      pageSeoTitle: string
      seoDescription: string
    }

    expect(vm.canonicalUrl).toBe('http://localhost:3000/blog?page=2&tag=Tech')
    expect(vm.pageSeoTitle).toBe('Blog - Tech - Page 2')
    expect(vm.seoDescription).toContain('Articles about Tech')

    const structured = vm.structuredData as { hasPart?: unknown[] }
    expect(structured.hasPart).toBeDefined()
    expect(Array.isArray(structured.hasPart)).toBe(true)
    expect((structured.hasPart as unknown[]).length).toBe(2)

    const headFactory = useHeadMock.mock.calls.at(-1)?.[0]
    if (headFactory) {
      const headEntries = typeof headFactory === 'function' ? headFactory() : headFactory
      expect(headEntries.link).toEqual([
        {
          rel: 'canonical',
          href: 'http://localhost:3000/blog?page=2&tag=Tech',
        },
      ])
      const structuredScript = headEntries.script?.[0]
      expect(structuredScript?.type).toBe('application/ld+json')
      expect(structuredScript?.children).toContain('BlogPosting')
    }
  })

  test('updates the router query when selecting or clearing a tag', async () => {
    blogState.__tags.value = [
      { name: ' Tech ', count: 5 },
      { name: 'Nuxt', count: 2 },
    ]

    const wrapper = await mountComponent()
    await flushPromises()

    routerPushMock.mockReset()

    const chipGroup = wrapper.getComponent({ name: 'VChipGroupStub' })
    chipGroup.vm.$emit('update:modelValue', 'Nuxt')
    await flushPromises()

    expect(routerPushMock).toHaveBeenCalledWith({
      path: '/blog',
      query: { tag: 'Nuxt' },
    })

    routerPushMock.mockReset()

    chipGroup.vm.$emit('update:modelValue', '__all__')
    await flushPromises()

    expect(routerPushMock).toHaveBeenCalledWith({
      path: '/blog',
      query: {},
    })
  })

  test('pushes the next page when pagination emits an update', async () => {
    const wrapper = await mountComponent()
    await flushPromises()

    routerPushMock.mockReset()

    const paginationTrigger = wrapper.get('[data-test="pagination-next"]')
    await paginationTrigger.trigger('click')

    expect(routerPushMock).toHaveBeenCalledWith({
      query: { page: '3', tag: ' Tech ' },
    })
  })

  test('displays loading indicator while articles are being fetched', async () => {
    blogState.loading.value = true

    const wrapper = await mountComponent()
    await flushPromises()

    expect(wrapper.text()).toContain('Loading articles...')
  })

  test('renders error message and retries fetching when requested', async () => {
    blogState.error.value = 'Failed to load articles'

    const wrapper = await mountComponent()
    await flushPromises()

    expect(wrapper.text()).toContain('Failed to load articles')

    blogState.fetchArticles.mockClear()

    await wrapper.get('button.v-btn').trigger('click')
    expect(blogState.fetchArticles).toHaveBeenCalled()
    const lastCall = blogState.fetchArticles.mock.calls.at(-1)
    expect(lastCall).toEqual([])
  })

  test('computes pagination information text from translations', async () => {
    blogState.__tags.value = []

    const wrapper = await mountComponent()
    await flushPromises()
    await nextTick()

    expect(wrapper.text()).toContain('Page 2 of 3 (2 articles)')
  })
})
