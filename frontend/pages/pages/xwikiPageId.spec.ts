import { mountSuspended } from '@nuxt/test-utils/runtime'
import { ref } from 'vue'
import { beforeAll, beforeEach, describe, expect, test, vi } from 'vitest'

const mockPage = {
  htmlContent: '<p>Hello page</p>',
  editLink: '/edit',
}

const mockUseFullPage = {
  htmlContent: mockPage.htmlContent,
  editLink: mockPage.editLink,
  loading: false,
  error: null,
  fetchPage: vi.fn(),
}

vi.mock('~/composables/content/useFullPage', () => ({
  useFullPage: () => mockUseFullPage,
}))

const mockUseAuth = {
  isLoggedIn: ref(true),
  hasRole: vi.fn(() => true),
}
vi.mock('~/composables/useAuth', () => ({
  useAuth: () => mockUseAuth,
}))

vi.mock('#app', () => ({
  useRuntimeConfig: () => ({
    public: { editRoles: ['editor'] },
  }),
}))

let PageComponent: typeof import('./[...xwikiPageId].vue')['default']
beforeAll(async () => {
  PageComponent = (await import('./[...xwikiPageId].vue')).default
})

describe('dynamic page', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  test('renders page content and calls fetchPage on mount', async () => {
    const wrapper = await mountSuspended(PageComponent, {
      route: { params: { xwikiPageId: 'Main.WebHome' } },
    })
    expect(mockUseFullPage.fetchPage).toHaveBeenCalledWith('Main.WebHome')
    expect(wrapper.html()).toContain(mockPage.htmlContent)
    expect(wrapper.find('a.edit-link').exists()).toBe(true)
  })

  test('shows edit link only for authorized users', async () => {
    const wrapper = await mountSuspended(PageComponent, {
      route: { params: { xwikiPageId: 'Main.WebHome' } },
    })
    const link = wrapper.find('a.edit-link')
    expect(link.text()).toBe('Edit')
    expect(link.attributes('target')).toBe('_blank')
    expect(link.attributes('rel')).toBe('noopener')
    expect(wrapper.find('.page-content').classes()).toContain('editable')

    mockUseAuth.hasRole.mockReturnValueOnce(false)
    const wrapper2 = await mountSuspended(PageComponent, {
      route: { params: { xwikiPageId: 'Main.WebHome' } },
    })
    expect(wrapper2.find('a.edit-link').exists()).toBe(false)
    expect(wrapper2.find('.page-content').classes()).not.toContain('editable')
  })

  test('shows loader when loading', async () => {
    mockUseFullPage.loading = true
    const wrapper = await mountSuspended(PageComponent, {
      route: { params: { xwikiPageId: 'Main.WebHome' } },
    })
    expect(wrapper.find('.v-progress-circular').exists()).toBe(true)
    mockUseFullPage.loading = false
  })
})
