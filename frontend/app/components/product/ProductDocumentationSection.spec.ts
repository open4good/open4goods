import { flushPromises, mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import { createI18n } from 'vue-i18n'
import { defineComponent, h, type PropType } from 'vue'
import ProductDocumentationSection from './ProductDocumentationSection.vue'
import type { ProductPdfDto } from '~~/shared/api-client'

vi.mock('vue-pdf-embed', () => ({
  default: defineComponent({
    name: 'VuePdfEmbedStub',
    props: {
      source: {
        type: [String, Object],
        default: '',
      },
      width: {
        type: Number,
        default: undefined,
      },
      scale: {
        type: Number,
        default: undefined,
      },
      rotation: {
        type: Number,
        default: 0,
      },
      page: {
        type: [Number, Array] as PropType<number | number[]>,
        default: undefined,
      },
      textLayer: {
        type: Boolean,
        default: true,
      },
      annotationLayer: {
        type: Boolean,
        default: true,
      },
    },
    setup(props) {
      return () =>
        h('div', {
          class: 'vue-pdf-embed-stub',
          'data-source': props.source ?? '',
          'data-rotation': String(props.rotation ?? 0),
          'data-scale': props.scale ? String(props.scale) : '',
          'data-width': props.width ? String(props.width) : '',
          'data-page': Array.isArray(props.page) ? props.page.join(',') : props.page ?? '',
          'data-text-layer': String(props.textLayer),
          'data-annotation-layer': String(props.annotationLayer),
        })
    },
  }),
}))

const ClientOnlyStub = defineComponent({
  name: 'ClientOnlyStub',
  setup(_, { slots }) {
    return () => slots.default?.() ?? null
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
            pageCount: '{count} page(s)',
            sidebarTitle: 'Available documents',
            navigationAria: 'Product documentation list',
            empty: 'No documents are available for this product yet.',
            previewUnavailable: 'Preview unavailable for this document. Use the download link instead.',
            loading: 'Loading document preview…',
            loadError: 'We could not load this document. Try downloading it instead.',
            printError: 'Printing failed. Please use the download link instead.',
            toolbar: {
              ariaLabel: 'PDF viewer controls',
              zoomOut: 'Zoom out',
              fitToWidth: 'Fit to width',
              zoomIn: 'Zoom in',
              zoomAuto: 'Auto',
              zoomIndicator: '{value}%',
              pageLabel: 'Page',
              allPages: 'All pages',
              pageOption: 'Page {page}',
              rotationLabel: 'Rotation',
              rotationOption: '{angle}°',
              textLayer: 'Text layer',
              annotationLayer: 'Annotations',
              print: 'Print',
              scrollPrevious: 'Scroll documents backwards',
              scrollNext: 'Scroll documents forwards',
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
    ...overrides,
  })

  const mountComponent = (props: { pdfs?: ProductPdfDto[] }) =>
    mount(ProductDocumentationSection, {
      props,
      global: {
        plugins: [createI18nPlugin()],
        stubs: { ClientOnly: ClientOnlyStub },
      },
    })

  it('renders the first PDF in the viewer by default', async () => {
    const pdfs = [basePdf(), basePdf({ cacheKey: 'doc-2', url: 'https://example.com/doc-2.pdf', language: 'en' })]
    const wrapper = mountComponent({ pdfs })

    await flushPromises()

    const viewer = wrapper.get('.vue-pdf-embed-stub')
    expect(viewer.attributes('data-source')).toBe(pdfs[0].url)

    const activeTab = wrapper.get('[data-testid="product-docs-tab"]')
    expect(activeTab.classes()).toContain('product-docs__tab--active')

    const meta = wrapper.get('.product-docs__viewer-meta').text()
    expect(meta).toContain('1.2 KB')
    expect(meta).toMatch(/3 page\(s\)/)
    expect(meta).toMatch(/2024/)

    const language = wrapper.get('.product-docs__viewer-language').text()
    expect(language).toContain('French')
  })

  it('switches the active PDF when selecting a different tab', async () => {
    const pdfs = [
      basePdf(),
      basePdf({ cacheKey: 'doc-2', url: 'https://example.com/doc-2.pdf', language: 'en', numberOfPages: 8 }),
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

  it('falls back to a message when a PDF has no preview URL', async () => {
    const wrapper = mountComponent({ pdfs: [basePdf({ url: undefined })] })

    await flushPromises()

    expect(wrapper.get('[data-testid="product-docs-preview-empty"]').text()).toContain('Preview unavailable')
  })

  it('renders an empty state when no documents are available', async () => {
    const wrapper = mountComponent({ pdfs: [] })

    expect(wrapper.get('[data-testid="product-docs-empty"]').text()).toContain('No documents are available')
  })

  it('displays the raw language label when an invalid locale code is provided', async () => {
    const wrapper = mountComponent({ pdfs: [basePdf({ language: 'Multilingue' })] })

    await flushPromises()

    expect(wrapper.get('.product-docs__viewer-language').text()).toContain('Multilingue')
  })

  it('updates the zoom indicator when toggling zoom controls', async () => {
    const wrapper = mountComponent({ pdfs: [basePdf()] })

    await flushPromises()

    const indicator = wrapper.get('[data-testid="product-docs-zoom-indicator"]')
    expect(indicator.text()).toContain('Auto')

    await wrapper.get('[data-testid="product-docs-zoom-in"]').trigger('click')
    await flushPromises()

    expect(indicator.text()).toContain('125')

    await wrapper.get('[data-testid="product-docs-zoom-auto"]').trigger('click')
    await flushPromises()

    expect(indicator.text()).toContain('Auto')
  })
})
