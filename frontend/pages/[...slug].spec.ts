import { mountSuspended } from '@nuxt/test-utils/runtime'
import { ref } from 'vue'
import { beforeAll, beforeEach, describe, expect, test, vi } from 'vitest'

const mockPage = {
  metaTitle: 'title',
  metaDescription: 'desc',
  html: '<p>Page</p>',
  editLink: '/edit',
}

const mockUseWikiPage = {
  data: ref(mockPage),
  loading: ref(false),
  error: ref(null),
  fetchPage: vi.fn(),
}

vi.mock('~/composables/wiki/useWikiPage', () => ({
  useWikiPage: () => mockUseWikiPage,
}))

vi.mock('~/composables/useAuth', () => ({
  useAuth: () => ({ isLoggedIn: ref(true), hasRole: vi.fn(() => true) }),
}))

vi.mock('#app', () => ({
  useRuntimeConfig: () => ({ public: { editRoles: ['editor'] } }),
}))

let WikiPage: typeof import('./[...slug].vue')['default']
beforeAll(async () => {
  WikiPage = (await import('./[...slug].vue')).default
})

describe('wiki dynamic page', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  test('renders page and calls fetch', async () => {
    const wrapper = await mountSuspended(WikiPage, {
      route: { params: { slug: ['Main', 'WebHome'] } },
    })
    expect(mockUseWikiPage.fetchPage).toHaveBeenCalledWith('Main.WebHome')
    expect(wrapper.html()).toContain(mockPage.html)
    expect(wrapper.find('a.edit-link').exists()).toBe(true)
  })
})
