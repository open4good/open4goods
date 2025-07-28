import { mountSuspended } from '@nuxt/test-utils/runtime'
import { beforeEach, describe, expect, test, vi } from 'vitest'
import TextContent from './TextContent.vue'

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
