import { flushPromises, mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import { createI18n } from 'vue-i18n'
import { defineComponent, h, ref } from 'vue'
import ProductDocumentationSection from './ProductDocumentationSection.vue'
import type { ProductPdfDto } from '~~/shared/api-client'

vi.mock('@vueuse/core', () => ({
  useEventListener: vi.fn(() => () => {}),
  useResizeObserver: vi.fn(() => ({ stop: vi.fn() })),
  useStorage: (_key: string, defaultValue: boolean) => ref(defaultValue),
}))

vi.mock('vue-pdf-embed', () => ({
  default: defineComponent({
    name: 'VuePdfEmbedStub',
    props: {
      source: {
        type: [String, Object],
        default: '',
      },
    },
    setup(props, { attrs }) {
      return () =>
        h(
          'div',
          {
            ...attrs,
            class: ['vue-pdf-embed-stub', attrs.class],
            'data-source': props.source ?? '',
          },
          []
        )
    },
  }),
}))

const ClientOnlyStub = defineComponent({
  name: 'ClientOnlyStub',
  setup(_, { slots }) {
    return () => slots.default?.() ?? null
  },
})

const VIconStub = defineComponent({
  name: 'VIconStub',
  props: {
    icon: { type: [String, Array], default: '' },
  },
  setup(props) {
    return () =>
      h('span', { class: 'v-icon-stub', 'data-icon': props.icon ?? '' })
  },
})

const createI18nPlugin = () =>
  createI18n({
    legacy: false,
    locale: 'en-US',
    fallbackLocale: 'en-US',
    messages: {
      'en-US': {
        product: {
          docs: {
            title: 'Documentation',
            subtitle: 'Preview and download official documents.',
            download: 'Download PDF',
            untitled: 'Untitled document',
            documentPdf: 'Document PDF',
            pageCount: '{count} page | {count} pages',
            sidebarTitle: 'Available documents',
            navigationAria: 'Product documentation list',
            empty: 'No documents are available for this product yet.',
            previewUnavailable:
              'Preview unavailable for this document. Use the download link instead.',
            loading: 'Loading document preview…',
            controls: {
              rotate: 'Rotate document',
              rotateLabel: 'Rotate',
              scrollLeft: 'Scroll left',
              scrollRight: 'Scroll right',
            },
          },
        },
      },
    },
  })

describe('ProductDocumentationSection', () => {
  const basePdf = (overrides: Partial<ProductPdfDto> = {}): ProductPdfDto => ({
    cacheKey: 'doc-1',
    url: 'https://example.com/doc-1.pdf',
    fileSize: 1_200,
    numberOfPages: 3,
    modificationDate: new Date('2024-03-25').getTime(),
    language: 'fr',
    author: 'Open4Goods',
    metadataTitle: 'Quick start manual',
    ...overrides,
  })

  const mountComponent = (props: { pdfs?: ProductPdfDto[] }) =>
    mount(ProductDocumentationSection, {
      props,
      global: {
        plugins: [createI18nPlugin()],
        stubs: {
          ClientOnly: ClientOnlyStub,
          VIcon: VIconStub,
          'v-icon': VIconStub,
        },
      },
    })

  it('renders the first PDF in the viewer by default', async () => {
    const pdfs = [
      basePdf(),
      basePdf({
        cacheKey: 'doc-2',
        url: 'https://example.com/doc-2.pdf',
        language: 'en',
      }),
    ]
    const wrapper = mountComponent({ pdfs })

    await flushPromises()

    const viewer = wrapper.get('.vue-pdf-embed-stub')
    expect(viewer.attributes('data-source')).toBe(pdfs[0].url)
    expect(viewer.classes()).toContain('product-docs__viewer-frame')
    expect(viewer.classes()).toContain('product-docs__viewer-frame--fit')

    const activeTab = wrapper.get('[data-testid="product-docs-tab"]')
    expect(activeTab.classes()).toContain('product-docs__tab--active')
    expect(activeTab.find('.product-docs__tab-title').text()).toBe(
      'Quick start manual'
    )

    const viewerTitle = wrapper.get('.product-docs__viewer-title')
    expect(viewerTitle.text()).toBe('Quick start manual')
    expect(viewerTitle.attributes('title')).toBe('Quick start manual')

    const metas = wrapper
      .findAll('.product-docs__viewer-meta')
      .map(node => node.text())
    expect(metas[0]).toContain('3 pages')
    expect(metas[0]).toMatch(/2024/)
    expect(metas[0]).toContain('French')
    expect(metas[1]).toContain('1.2 KB')
    expect(metas[1]).toContain('Open4Goods')
  })

  it('switches the active PDF when selecting a different tab', async () => {
    const pdfs = [
      basePdf(),
      basePdf({
        cacheKey: 'doc-2',
        url: 'https://example.com/doc-2.pdf',
        language: 'en',
        numberOfPages: 8,
      }),
    ]
    const wrapper = mountComponent({ pdfs })

    await flushPromises()

    const tabs = wrapper.findAll('[data-testid="product-docs-tab"]')
    expect(tabs).toHaveLength(2)

    await tabs[1].trigger('click')
    await flushPromises()

    const viewer = wrapper.get('.vue-pdf-embed-stub')
    expect(viewer.attributes('data-source')).toBe(pdfs[1].url)
    expect(tabs[1].classes()).toContain('product-docs__tab--active')
  })

  it('hides navigation arrows when documents fit within the viewport', async () => {
    const wrapper = mountComponent({ pdfs: [basePdf()] })

    await flushPromises()

    expect(wrapper.findAll('.product-docs__nav-arrow')).toHaveLength(0)
  })

  it('falls back to raw language label when Intl formatting fails', async () => {
    const wrapper = mountComponent({
      pdfs: [basePdf({ language: 'Multilingue' })],
    })

    await flushPromises()

    const metas = wrapper
      .findAll('.product-docs__viewer-meta')
      .map(node => node.text())
    expect(metas[0]).toContain('Multilingue')
  })

  it('falls back to a message when a PDF has no preview URL', async () => {
    const wrapper = mountComponent({ pdfs: [basePdf({ url: undefined })] })

    await flushPromises()

    expect(
      wrapper.get('[data-testid="product-docs-preview-empty"]').text()
    ).toContain('Preview unavailable')
  })

  it('renders an empty state when no documents are available', async () => {
    const wrapper = mountComponent({ pdfs: [] })

    expect(wrapper.get('[data-testid="product-docs-empty"]').text()).toContain(
      'No documents are available'
    )
  })

  it('falls back to a translated generic title when a PDF has no title metadata', async () => {
    const wrapper = mountComponent({
      pdfs: [
        basePdf({
          cacheKey: 'doc-3',
          url: 'https://example.com/doc-3.pdf',
          extractedTitle: undefined,
          metadataTitle: undefined,
          fileName: undefined,
        }),
      ],
    })

    await flushPromises()

    const tabTitle = wrapper.get('.product-docs__tab-title').text()
    const viewerTitle = wrapper.get('.product-docs__viewer-title').text()

    expect(tabTitle).toBe('Document PDF')
    expect(viewerTitle).toBe('Document PDF')
  })

  it('uses the same truncated title in the viewer and the navigation tab', async () => {
    const longTitle =
      'Very long product documentation title exceeding thirty five characters'
    const wrapper = mountComponent({
      pdfs: [basePdf({ cacheKey: 'doc-long', extractedTitle: longTitle })],
    })

    await flushPromises()

    const tab = wrapper.get('[data-testid="product-docs-tab"]')
    expect(tab.attributes('title')).toBe(longTitle)

    const tabTitle = tab.get('.product-docs__tab-title').text()
    const viewerTitle = wrapper.get('.product-docs__viewer-title').text()

    expect(tabTitle.endsWith('…')).toBe(true)
    expect(tabTitle).toHaveLength(35)
    expect(viewerTitle).toBe(tabTitle)
    expect(wrapper.get('.product-docs__viewer-title').attributes('title')).toBe(
      longTitle
    )
  })
})
