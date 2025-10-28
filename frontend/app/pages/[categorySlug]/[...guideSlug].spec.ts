import { mountSuspended, mockNuxtImport } from '@nuxt/test-utils/runtime'
import { afterAll, beforeAll, beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent, h } from 'vue'

vi.mock('~/components/cms/XwikiFullPageRenderer.vue', () => ({
  default: defineComponent({
    name: 'XwikiFullPageRendererStub',
    props: {
      pageId: { type: String, default: '' },
      fallbackTitle: { type: String, default: '' },
      fallbackDescription: { type: String, default: '' },
    },
    setup(props) {
      return () =>
        h('div', { class: 'xwiki-renderer-stub' }, [
          h('span', { class: 'xwiki-renderer-stub__id' }, props.pageId),
          h('span', { class: 'xwiki-renderer-stub__title' }, props.fallbackTitle),
          h('span', { class: 'xwiki-renderer-stub__description' }, props.fallbackDescription),
        ])
    },
  }),
}))

const selectCategoryBySlugMock = vi.fn()
const navigateToMock = vi.hoisted(() => vi.fn())
const route = { params: { categorySlug: 'televiseurs', guideSlug: '' } }

vi.mock('~/composables/categories/useCategories', () => ({
  useCategories: () => ({
    selectCategoryBySlug: selectCategoryBySlugMock,
  }),
}))

mockNuxtImport('createError', () => (input: { statusCode?: number; statusMessage?: string }) => {
  const error = new Error(input?.statusMessage ?? 'Error')
  Object.assign(error, input)
  return error
})
mockNuxtImport('navigateTo', () => navigateToMock)
mockNuxtImport('useRoute', () => () => route)

describe('[categorySlug]/[...guideSlug] page', () => {
  beforeAll(() => {
    navigateToMock.mockResolvedValue(undefined)
  })

  afterAll(() => {
    vi.unstubAllGlobals()
  })

  beforeEach(() => {
    navigateToMock.mockReset()
    navigateToMock.mockResolvedValue(undefined)
    selectCategoryBySlugMock.mockReset()
    route.params.categorySlug = 'televiseurs'
    route.params.guideSlug = ''
  })

  it('redirects product guide slugs to the canonical product page', async () => {
    route.params.categorySlug = 'televiseurs'
    route.params.guideSlug = '8806094355536-television-samsung-qe32ls03b-2022'

    const Page = await import('./[...guideSlug].vue').then(module => module.default)

    await mountSuspended(Page)

    expect(navigateToMock).toHaveBeenCalledTimes(1)
    expect(navigateToMock).toHaveBeenCalledWith(
      {
        name: 'slug',
        params: {
          slug: ['televiseurs', '8806094355536-television-samsung-qe32ls03b-2022'],
        },
      },
      { replace: true, redirectCode: 301 },
    )
    expect(selectCategoryBySlugMock).not.toHaveBeenCalled()
  })
})
