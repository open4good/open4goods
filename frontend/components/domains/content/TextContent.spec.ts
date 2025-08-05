import { mountSuspended } from '@nuxt/test-utils/runtime'
import { ref } from 'vue'
import { beforeAll, beforeEach, describe, expect, test, vi } from 'vitest'

const mockBloc = {
  htmlContent: '<p>Hello bloc</p>',
  editLink: '/edit',
}

const mockUseContentBloc = {
  htmlContent: mockBloc.htmlContent,
  editLink: mockBloc.editLink,
  loading: false,
  error: null,
  fetchBloc: vi.fn(),
}

vi.mock('~/composables/content/useContentBloc', () => ({
  useContentBloc: () => mockUseContentBloc,
}))

// Mock authentication composable
const mockUseAuth = {
  isLoggedIn: ref(true),
  hasRole: vi.fn(() => true),
}
vi.mock('~/composables/useAuth', () => ({
  useAuth: () => mockUseAuth,
}))

// Mock runtime configuration with required edit roles
vi.mock('#app', () => ({
  useRuntimeConfig: () => ({
    public: {
      editRoles: ['editor'],
    },
  }),
}))

// Dynamically import the component to apply mocks before evaluation
let TextContent: typeof import('./TextContent.vue')['default']
beforeAll(async () => {
  TextContent = (await import('./TextContent.vue')).default
})

describe('TextContent', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  test('renders bloc content and calls fetch on mount', async () => {
    const wrapper = await mountSuspended(TextContent, {
      props: { blocId: 'Main.WebHome' },
    })
    expect(mockUseContentBloc.fetchBloc).toHaveBeenCalledWith('Main.WebHome')
    expect(wrapper.html()).toContain(mockBloc.htmlContent)
    expect(wrapper.find('a.edit-link').exists()).toBe(true)
  })

  test('shows edit link only for authorized users', async () => {
    const wrapper = await mountSuspended(TextContent, {
      props: { blocId: 'Main.WebHome' },
    })
    const link = wrapper.find('a.edit-link')
    expect(link.text()).toBe('Edit')
    expect(link.attributes('target')).toBe('_blank')
    expect(link.attributes('rel')).toBe('noopener')
    expect(wrapper.find('.text-content').classes()).toContain('editable')

    // Simulate user without required role
    mockUseAuth.hasRole.mockReturnValueOnce(false)
    const wrapper2 = await mountSuspended(TextContent, {
      props: { blocId: 'Main.WebHome' },
    })
    expect(wrapper2.find('a.edit-link').exists()).toBe(false)
    expect(wrapper2.find('.text-content').classes()).not.toContain('editable')
  })

  test('shows loader when loading', async () => {
    mockUseContentBloc.loading = true
    const wrapper = await mountSuspended(TextContent, {
      props: { blocId: 'id' },
    })
    expect(wrapper.find('.v-progress-circular').exists()).toBe(true)
    mockUseContentBloc.loading = false
  })
})
