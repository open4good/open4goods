import { mountSuspended } from '@nuxt/test-utils/runtime'
import { ref } from 'vue'
import { beforeAll, beforeEach, describe, expect, test, vi } from 'vitest'
import { DEFAULT_LOREM_LENGTH } from '~/utils/content/_loremIpsum'

const mockBloc = {
  htmlContent: '<p>Hello bloc</p>',
  editLink: '/edit',
}

const htmlContent = ref(mockBloc.htmlContent)
const editLink = ref(mockBloc.editLink)
const pending = ref(false)
const error = ref<string | null>(null)

const mockUseContentBloc = vi.fn(async () => ({
  htmlContent,
  editLink,
  pending,
  error,
}))

vi.mock('~/composables/content/useContentBloc', () => ({
  useContentBloc: mockUseContentBloc,
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
    htmlContent.value = mockBloc.htmlContent
    editLink.value = mockBloc.editLink
    pending.value = false
    error.value = null
    mockUseAuth.hasRole.mockReturnValue(true)
  })

  test('renders bloc content and requests SSR data for the bloc id', async () => {
    const wrapper = await mountSuspended(TextContent, {
      props: { blocId: 'Main.WebHome' },
    })
    expect(mockUseContentBloc).toHaveBeenCalledTimes(1)
    const blocArg = mockUseContentBloc.mock.calls[0]?.[0]
    expect(blocArg && 'value' in blocArg ? blocArg.value : undefined).toBe('Main.WebHome')
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

  test('shows loader when pending', async () => {
    pending.value = true
    const wrapper = await mountSuspended(TextContent, {
      props: { blocId: 'id' },
    })
    expect(wrapper.find('.v-progress-circular').exists()).toBe(true)
    pending.value = false
  })

  test('renders lorem ipsum fallback when bloc content is empty', async () => {
    htmlContent.value = '    '
    const wrapper = await mountSuspended(TextContent, {
      props: { blocId: 'Main.WebHome' },
    })
    const sandbox = wrapper.find('.xwiki-sandbox').element
    const plainTextLength = sandbox.textContent?.trim().length ?? 0
    expect(sandbox.innerHTML).toContain('Lorem ipsum dolor sit amet')
    expect(plainTextLength).toBeGreaterThanOrEqual(DEFAULT_LOREM_LENGTH)
  })

  test('extends fallback length when custom defaultLength is provided', async () => {
    htmlContent.value = ''
    const customLength = 900
    const wrapper = await mountSuspended(TextContent, {
      props: { blocId: 'Main.WebHome', defaultLength: customLength },
    })
    const sandbox = wrapper.find('.xwiki-sandbox').element
    const plainTextLength = sandbox.textContent?.trim().length ?? 0
    expect(plainTextLength).toBeGreaterThanOrEqual(customLength)
  })
})
