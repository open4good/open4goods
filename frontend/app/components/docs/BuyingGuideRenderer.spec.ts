import { mountSuspended, mockNuxtImport } from '@nuxt/test-utils/runtime'
import { describe, expect, it, vi } from 'vitest'
import { defineComponent, h, nextTick, ref } from 'vue'

import type { DocsDoc } from '~/composables/useDocsContent'

const localeRef = ref('en-US')

const messages: Record<string, string> = {
  'buyingGuide.article.eyebrow': 'Buying guide',
  'buyingGuide.article.progressAria': 'Article reading progress',
  'buyingGuide.article.readingTime': 'Approx. {minutes} min read',
  'buyingGuide.article.tocAria': 'Buying guide section navigation',
  'buyingGuide.article.updated': 'Updated {date}',
  'docs.labels.article': 'Article',
  'docs.labels.toc': 'Contents',
  'docs.errors.load': 'Unable to load this content.',
}

const translate = (key: string, params: Record<string, unknown> = {}) =>
  (messages[key] ?? key).replace(/\{(\w+)\}/g, (_, match) =>
    String(params[match] ?? '')
  )

mockNuxtImport('useI18n', () => () => ({
  t: (key: string, params: Record<string, unknown> = {}) =>
    translate(key, params),
  locale: localeRef,
}))

vi.mock('~/components/shared/ui/SectionReveal.vue', () => ({
  default: {
    name: 'SectionRevealStub',
    template: '<div class="section-reveal-stub"><slot /></div>',
    props: ['transition'],
  },
}))

vi.mock('~/components/docs/DocsContentLink.vue', () => ({
  default: {
    name: 'DocsContentLinkStub',
    template: '<a><slot /></a>',
  },
}))

vi.mock('~/components/product/ProductCardEmbed.vue', () => ({
  default: {
    name: 'ProductCardEmbedStub',
    template: '<div data-test="product-card-embed" />',
  },
}))

vi.mock('~/components/product/ProductEmbed.vue', () => ({
  default: {
    name: 'ProductEmbedStub',
    template: '<div data-test="product-embed" />',
  },
}))

vi.mock('~/components/dataviz/BrandShareChart.vue', () => ({
  default: {
    name: 'BrandShareChartStub',
    template: '<div data-test="brand-share-chart" />',
  },
}))

vi.mock('~/components/product/GuideProductGrid.vue', () => ({
  default: {
    name: 'GuideProductGridStub',
    template: '<div data-test="guide-product-grid" />',
  },
}))

const ContentRendererStub = defineComponent({
  name: 'ContentRenderer',
  props: {
    value: {
      type: Object,
      required: true,
    },
    components: {
      type: Object,
      required: true,
    },
  },
  setup(props) {
    return () => {
      const Heading = props.components.h1
      const Grid = props.components.GuideProductGrid

      return h('div', [
        h(Heading, { id: 'markdown-title' }, () => props.value.title),
        h('h2', { id: 'intro' }, 'Intro'),
        h('p', 'Readable content'),
        h(Grid),
      ])
    }
  },
})

const StickySectionNavigationStub = defineComponent({
  name: 'StickySectionNavigation',
  props: {
    sections: {
      type: Array,
      default: () => [],
    },
    activeSection: {
      type: String,
      default: '',
    },
  },
  emits: ['navigate'],
  setup(props, { emit }) {
    return () =>
      h(
        'nav',
        { class: 'sticky-section-navigation-stub' },
        (props.sections as Array<{ id: string; label: string }>).map(section =>
          h(
            'button',
            {
              class:
                section.id === props.activeSection ? 'is-active' : undefined,
              type: 'button',
              onClick: () => emit('navigate', section.id),
            },
            section.label
          )
        )
      )
  },
})

vi.mock('~/components/shared/ui/StickySectionNavigation.vue', () => ({
  default: StickySectionNavigationStub,
}))

const baseDoc: DocsDoc = {
  path: '/guides/test/buying-guide',
  title: 'Best quiet dishwasher',
  description: 'A calm guide for repeat reading.',
  tags: ['language:en', 'kitchen'],
  updatedAt: '2026-05-01',
  body: {
    toc: {
      links: [{ id: 'intro', text: 'Intro', depth: 2 }],
    },
    children: [
      { type: 'text', value: 'word '.repeat(440) },
      { type: 'element', tag: 'GuideProductGrid' },
    ],
  },
}

const mountRenderer = async () => {
  const componentModule = await import('./BuyingGuideRenderer.vue')

  return mountSuspended(componentModule.default, {
    attachTo: document.body,
    props: {
      doc: baseDoc,
      guideContext: {
        verticalId: 'appliances',
        categorySlug: 'dishwashers',
        categoryPath: '/dishwashers',
        categoryTitle: 'Dishwashers',
        heroImage: '/guide.jpg',
      },
      breadcrumbs: [{ title: 'Home', to: '/' }],
    },
    global: {
      stubs: {
        ContentRenderer: ContentRendererStub,
        VAlert: { template: '<div><slot /></div>' },
        VBreadcrumbs: { template: '<nav class="v-breadcrumbs" />' },
        VChip: {
          template: '<span class="v-chip"><slot /></span>',
          props: ['prependIcon', 'variant', 'color', 'size'],
        },
        VCol: { template: '<div><slot /></div>' },
        VContainer: { template: '<div><slot /></div>' },
        VExpansionPanel: { template: '<div><slot /></div>' },
        VExpansionPanels: { template: '<div><slot /></div>' },
        VExpansionPanelText: { template: '<div><slot /></div>' },
        VExpansionPanelTitle: { template: '<button><slot /></button>' },
        VFadeTransition: { template: '<div><slot /></div>' },
        VImg: { template: '<img class="v-img" />', props: ['src', 'alt'] },
        VProgressLinear: {
          template: '<div class="v-progress-linear" :aria-label="ariaLabel" />',
          props: ['ariaLabel', 'modelValue', 'height', 'color'],
        },
        VRow: { template: '<div><slot /></div>' },
        VSlideYTransition: { template: '<div><slot /></div>' },
      },
    },
  })
}

describe('BuyingGuideRenderer', () => {
  it('renders reading progress metadata and reading time', async () => {
    const wrapper = await mountRenderer()

    expect(wrapper.get('.v-progress-linear').attributes('aria-label')).toBe(
      'Article reading progress'
    )
    expect(wrapper.text()).toContain('Approx. 2 min read')
  })

  it('marks the first markdown H1 as the duplicate title and keeps embeds', async () => {
    const wrapper = await mountRenderer()
    const markdownTitle = wrapper.get('h1#markdown-title')

    expect(markdownTitle.classes()).toContain(
      'buying-guide__markdown-title--duplicate'
    )
    expect(markdownTitle.attributes('aria-hidden')).toBe('true')
    expect(wrapper.find('[data-test="guide-product-grid"]').exists()).toBe(true)
  })

  it('keeps ToC navigation wired to article anchors', async () => {
    const scrollTo = vi.fn()
    Object.defineProperty(window, 'scrollTo', {
      value: scrollTo,
      writable: true,
    })

    const wrapper = await mountRenderer()
    scrollTo.mockClear()

    await wrapper.get('.sticky-section-navigation-stub button').trigger('click')
    await nextTick()

    expect(scrollTo).toHaveBeenCalledWith(
      expect.objectContaining({
        behavior: 'smooth',
      })
    )
    expect(wrapper.find('.is-active').text()).toBe('Intro')
  })
})
