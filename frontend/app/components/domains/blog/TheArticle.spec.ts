import { mountSuspended } from '@nuxt/test-utils/runtime'
import { ref } from 'vue'
import { beforeEach, describe, expect, test, vi } from 'vitest'

vi.mock('#imports', () => {
  const useHeadMock = vi.fn()
  const useSeoMetaMock = vi.fn()
  const useRequestURLMock = () =>
    new URL('https://example.com/blog/test-article')

  return {
    useHead: useHeadMock,
    useSeoMeta: useSeoMetaMock,
    useRequestURL: useRequestURLMock,
  }
})

vi.mock('~/components/shared/images/RobustImage.vue', () => ({
  default: {
    name: 'RobustImageStub',
    props: ['src', 'alt', 'width', 'height'],
    template: '<div class="robust-image-stub" />',
  },
}))

const isLoggedInRef = ref(true)

vi.mock('~/composables/useAuth', () => ({
  useAuth: () => ({
    isLoggedIn: isLoggedInRef,
  }),
}))

vi.mock('vue-i18n', () => ({
  useI18n: () => ({
    t: (key: string, params: Record<string, unknown> = {}) => {
      switch (key) {
        case 'blog.article.readingTime':
          return `Approx. ${params.minutes} min read`
        case 'blog.article.updated':
          return `Updated ${params.date}`
        case 'blog.article.featuredImageAlt':
          return `Featured image for ${params.title}`
        case 'blog.article.empty':
          return 'The content of this article will be available soon.'
        case 'blog.article.edit':
          return 'Edit this article'
        default:
          return key
      }
    },
    locale: ref('en-US'),
  }),
}))

describe('TheArticle.vue', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    isLoggedInRef.value = true
  })

  const baseArticle = {
    url: 'test-article',
    title: 'A Nuxt powered article',
    summary: 'Short summary of the article for SEO and preview cards.',
    author: 'Jane Doe',
    category: ['Nuxt', 'Vue'],
    image: 'https://example.com/image.jpg',
    body: '<p>Hello <strong>world</strong>!</p>',
    createdMs: Date.UTC(2024, 0, 10),
    modifiedMs: Date.UTC(2024, 0, 12),
    editLink: 'https://cms.example.com/edit',
  }

  const mountComponent = async (overrides: Record<string, unknown> = {}) => {
    const props = { article: { ...baseArticle, ...overrides } }
    const componentModule = await import('./TheArticle.vue')
    const TheArticle = componentModule.default

    return mountSuspended(TheArticle, {
      props,
      global: {
        stubs: {
          VChip: {
            template: '<span class="v-chip"><slot /></span>',
            props: ['to', 'link', 'color', 'variant', 'size'],
          },
          VIcon: {
            template: '<span class="v-icon"><slot /></span>',
          },
          VDivider: {
            template: '<hr />',
          },
          VBtn: {
            template: '<button><slot /></button>',
            props: ['href', 'target', 'rel', 'prependIcon', 'variant', 'size'],
          },
          VAlert: {
            template: '<div class="v-alert"><slot /></div>',
            props: ['type', 'variant'],
          },
        },
      },
    })
  }

  test('renders title, metadata and body content', async () => {
    const wrapper = await mountComponent()

    expect(wrapper.get('[data-test="article-title"]').text()).toBe(
      baseArticle.title
    )
    expect(wrapper.get('[data-test="article-summary"]').text()).toBe(
      baseArticle.summary
    )
    expect(wrapper.get('[data-test="article-author"]').text()).toContain(
      baseArticle.author
    )

    const contentHtml = wrapper.get('[data-test="article-body"]').html()
    expect(contentHtml).toContain('<p>Hello <strong>world</strong>!</p>')
    expect(contentHtml).not.toContain('<script>')

    const categories = wrapper.findAll('[data-test="article-category"]')
    expect(categories).toHaveLength(baseArticle.category.length)
  })

  test('falls back to informational message when body is missing', async () => {
    const wrapper = await mountComponent({ body: '' })

    expect(wrapper.find('[data-test="article-body"]').exists()).toBe(false)
    expect(wrapper.get('[data-test="article-empty"]').text()).toContain(
      'The content of this article will be available soon.'
    )
  })

  test('computes reading time when enough text is provided', async () => {
    const longBody = '<p>' + 'word '.repeat(450) + '</p>'
    const wrapper = await mountComponent({ body: longBody })

    const readingTime = wrapper.get('[data-test="article-reading-time"]').text()
    expect(readingTime).toContain('Approx. ')
  })

  test('hides the edit link when the user is not authenticated', async () => {
    isLoggedInRef.value = false

    const wrapper = await mountComponent()

    expect(wrapper.find('[data-test="article-edit-link"]').exists()).toBe(false)
  })
})
