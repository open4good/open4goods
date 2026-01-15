import { describe, it, expect, beforeEach, vi } from 'vitest'
import { ref, isRef } from 'vue'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import TextContent from './TextContent.vue'

const useContentBlocMock = vi.hoisted(() => vi.fn())
const generateLoremMock = vi.hoisted(() =>
  vi.fn<(length?: number) => string>(() => 'generated-lorem')
)
const hasRoleMock = vi.fn<(role: string) => boolean>(() => false)

const authState = {
  isLoggedIn: ref(false),
  roles: ref<string[]>([]),
  username: ref<string | null>(null),
  hasRole: hasRoleMock,
  logout: vi.fn(),
}

let runtimeConfig: { public: { editRoles: string[] } }

vi.mock('~/composables/content/useContentBloc', () => ({
  useContentBloc: useContentBlocMock,
}))

vi.mock('~/composables/useAuth', () => ({
  useAuth: () => authState,
}))

vi.mock('#app', () => ({
  useRuntimeConfig: () => runtimeConfig,
}))

vi.mock('~/utils/content/_loremIpsum', () => ({
  DEFAULT_LOREM_LENGTH: 480,
  _generateLoremIpsum: generateLoremMock,
}))

const defaultStubs = {
  'v-progress-circular': {
    template: '<div class="v-progress-circular-stub" />',
  },
  'v-alert': { template: '<div class="v-alert-stub"><slot /></div>' },
}

type BlocMockOptions = {
  htmlContent?: string
  editLink?: string | null
  pending?: boolean
  error?: string | null
}

const createBlocResponse = (options: BlocMockOptions = {}) => {
  return {
    htmlContent: ref(options.htmlContent ?? '<p>Content</p>'),
    editLink: ref(options.editLink ?? null),
    pending: ref(options.pending ?? false),
    error: ref(options.error ?? null),
    refresh: vi.fn(),
  }
}

const mountComponent = async (props: Record<string, unknown> = {}) => {
  return await mountSuspended(TextContent, {
    props: {
      blocId: 'Main.WebHome',
      ...props,
    },
    global: {
      stubs: defaultStubs,
    },
  })
}

beforeEach(() => {
  runtimeConfig = { public: { editRoles: [] } }
  authState.isLoggedIn.value = false
  authState.roles.value = []
  hasRoleMock.mockReset()
  hasRoleMock.mockReturnValue(false)
  generateLoremMock.mockReset()
  generateLoremMock.mockReturnValue('generated-lorem')
  useContentBlocMock.mockReset()
})

describe('TextContent', () => {
  it('fetches bloc content using the provided bloc identifier', async () => {
    useContentBlocMock.mockResolvedValue(
      createBlocResponse({ htmlContent: '<p>Loaded</p>' })
    )

    await mountComponent()

    expect(useContentBlocMock).toHaveBeenCalledTimes(1)
    const [firstArg] = useContentBlocMock.mock.calls[0] ?? []
    expect(isRef(firstArg)).toBe(true)
    if (isRef(firstArg)) {
      expect(firstArg.value).toBe('Main.WebHome')
    }
  })

  it('renders remote HTML content when available', async () => {
    const blocResponse = createBlocResponse({
      htmlContent: '<p>Server content</p>',
    })
    useContentBlocMock.mockResolvedValue(blocResponse)

    const wrapper = await mountComponent()

    expect(wrapper.get('.xwiki-sandbox').element.innerHTML).toBe(
      '<p>Server content</p>'
    )
    expect(wrapper.find('.v-progress-circular-stub').exists()).toBe(false)
    expect(wrapper.find('.v-alert-stub').exists()).toBe(false)
  })

  it('displays a loading indicator while the bloc content is pending', async () => {
    const blocResponse = createBlocResponse({ pending: true })
    useContentBlocMock.mockResolvedValue(blocResponse)

    const wrapper = await mountComponent()

    expect(wrapper.find('.v-progress-circular-stub').exists()).toBe(true)
    expect(wrapper.find('.xwiki-sandbox').exists()).toBe(false)
    expect(wrapper.find('.edit-link').exists()).toBe(false)
  })

  it('shows an error alert when the bloc request fails', async () => {
    const blocResponse = createBlocResponse({ error: 'Unable to load bloc' })
    useContentBlocMock.mockResolvedValue(blocResponse)

    const wrapper = await mountComponent()

    expect(wrapper.find('.v-alert-stub').text()).toContain(
      'Unable to load bloc'
    )
    expect(wrapper.find('.xwiki-sandbox').exists()).toBe(false)
  })

  it('falls back to generated lorem ipsum when no content is returned', async () => {
    const blocResponse = createBlocResponse({ htmlContent: '   ' })
    useContentBlocMock.mockResolvedValue(blocResponse)

    const wrapper = await mountComponent({ ipsumLength: 120 })

    expect(generateLoremMock).toHaveBeenCalledWith(120)
    expect(wrapper.get('.xwiki-sandbox').element.innerHTML).toBe(
      'generated-lorem'
    )
  })

  it('uses the defaultLength prop when ipsumLength is not provided', async () => {
    const blocResponse = createBlocResponse({ htmlContent: '' })
    useContentBlocMock.mockResolvedValue(blocResponse)

    await mountComponent({ defaultLength: 320 })

    expect(generateLoremMock).toHaveBeenCalledWith(320)
  })

  it('uses the global default length when no overrides are provided', async () => {
    const blocResponse = createBlocResponse({ htmlContent: '' })
    useContentBlocMock.mockResolvedValue(blocResponse)

    await mountComponent()

    expect(generateLoremMock).toHaveBeenCalledWith(480)
  })

  it('renders an edit link when the user has the required role', async () => {
    runtimeConfig.public.editRoles = ['content-editor', 'admin']
    authState.isLoggedIn.value = true
    hasRoleMock.mockImplementation((role: string) => role === 'content-editor')

    const blocResponse = createBlocResponse({
      htmlContent: '<p>Editable</p>',
      editLink: 'https://xwiki.example.com/edit/Main.WebHome',
    })
    useContentBlocMock.mockResolvedValue(blocResponse)

    const wrapper = await mountComponent()

    const editLink = wrapper.get('a.edit-link')
    expect(editLink.attributes('href')).toBe(
      'https://xwiki.example.com/edit/Main.WebHome'
    )
    expect(editLink.attributes('target')).toBe('_blank')
    expect(editLink.attributes('rel')).toBe('noopener')
    expect(hasRoleMock).toHaveBeenCalledWith('content-editor')
  })

  it('hides the edit link when the user lacks the required role', async () => {
    runtimeConfig.public.editRoles = ['content-editor']
    authState.isLoggedIn.value = true
    hasRoleMock.mockReturnValue(false)

    const blocResponse = createBlocResponse({
      htmlContent: '<p>Content</p>',
      editLink: 'https://xwiki.example.com/edit/Main.WebHome',
    })
    useContentBlocMock.mockResolvedValue(blocResponse)

    const wrapper = await mountComponent()

    expect(wrapper.find('.edit-link').exists()).toBe(false)
  })
})
