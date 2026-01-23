import { mountSuspended, mockNuxtImport } from '@nuxt/test-utils/runtime'
import { beforeAll, beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent, h, ref } from 'vue'
import { flushPromises } from '@vue/test-utils'
import type { VerticalConfigFullDto } from '~~/shared/api-client'

const selectCategoryBySlugMock = vi.fn()
const useSeoMetaMock = vi.hoisted(() => vi.fn())
const mdAndDown = ref(false)
const localeRef = ref('en-US')

const messages: Record<string, string> = {
  'siteIdentity.siteName': 'Nudger',
  'category.ecoscorePage.breadcrumbLeaf': 'impactscore',
  'category.ecoscorePage.navigation.ariaLabel':
    'Impact Score section navigation',
  'category.ecoscorePage.navigation.availableCriteria': 'Available criteria',
  'category.ecoscorePage.navigation.overview': 'Overview',
  'category.ecoscorePage.navigation.purpose': 'Purpose & data',
  'category.ecoscorePage.navigation.criteria': 'Criteria',
  'category.ecoscorePage.navigation.transparency': 'Transparency',
  'category.ecoscorePage.navigation.aiAudit': 'AI audit',
  'category.ecoscorePage.sections.availableCriteria.title':
    'Available criteria for {category}',
  'category.ecoscorePage.sections.availableCriteria.subtitle':
    'Each Impact Score vertical uses criteria tailored to its products.',
  'category.ecoscorePage.sections.overview.title':
    'Impact Score for {category}',
  'category.ecoscorePage.sections.overview.card.title': 'Nudger Impact Score',
  'category.ecoscorePage.sections.overview.card.description':
    'This page details the criteria and weightings used to assess the environmental impact of this category.',
  'category.ecoscorePage.sections.overview.card.aria':
    'Learn more about the global Impact Score methodology',
  'category.ecoscorePage.sections.overview.card.cta':
    'Understand the global methodology',
  'category.ecoscorePage.sections.overview.card.scoreLabel':
    'Sample Impact Score',
  'category.ecoscorePage.sections.overview.visualization.eyebrow':
    'Impact breakdown',
  'category.ecoscorePage.sections.overview.visualization.title':
    'Score composition',
  'category.ecoscorePage.sections.overview.visualization.subtitle':
    'How the {category} Impact Score is distributed across the criteria.',
  'category.ecoscorePage.sections.overview.visualization.centerLabel':
    'Impact Score {category}',
  'category.ecoscorePage.sections.overview.visualization.weight':
    '{value}% weight',
  'category.ecoscorePage.sections.overview.visualization.weightFallback':
    'Weight pending',
  'category.ecoscorePage.sections.purpose.title':
    'Why and how we score {category}',
  'category.ecoscorePage.sections.purpose.objectiveTitle': 'Objective',
  'category.ecoscorePage.sections.purpose.objectiveFallback':
    'The objective description will be available soon.',
  'category.ecoscorePage.sections.purpose.dataTitle': 'Available data',
  'category.ecoscorePage.sections.purpose.dataFallback':
    'Detailed data for each criterion will be available soon.',
  'category.ecoscorePage.sections.criteria.title':
    'Selected criteria and weights',
  'category.ecoscorePage.sections.criteria.coefficientPrefix': 'Counts for',
  'category.ecoscorePage.sections.criteria.coefficientSuffix':
    'in the overall score',
  'category.ecoscorePage.sections.criteria.empty':
    'Criteria will be published soon.',
  'category.ecoscorePage.sections.transparency.title':
    'Transparency and traceability',
  'category.ecoscorePage.sections.transparency.criticalReviewTitle':
    'Critical review',
  'category.ecoscorePage.sections.transparency.criticalReviewFallback':
    'The critical review will be shared soon.',
  'category.ecoscorePage.sections.transparency.communityTitle':
    'Join the discussion',
  'category.ecoscorePage.sections.transparency.communityBody':
    'The {category} Impact Score definition is public on GitHub. Share your feedback or suggest adjustments.',
  'category.ecoscorePage.sections.transparency.communityCta':
    'View the configuration',
  'category.ecoscorePage.sections.transparency.communityIssues':
    'Open an issue',
  'category.ecoscorePage.sections.transparency.cardsTitle':
    'Open methodology resources',
  'category.ecoscorePage.sections.transparency.cards.openSource.title':
    'Open-source methodology',
  'category.ecoscorePage.sections.transparency.cards.openSource.description':
    'Follow our public repositories to track how we refine the Impact Score for {category}.',
  'category.ecoscorePage.sections.transparency.cards.openSource.cta':
    'Explore our open-source approach',
  'category.ecoscorePage.sections.transparency.cards.openSource.aria':
    'Explore open-source resources for the {category} Impact Score',
  'category.ecoscorePage.sections.transparency.cards.openData.title':
    'Open data exports',
  'category.ecoscorePage.sections.transparency.cards.openData.description':
    'Access the datasets powering the Impact Score for {category} and reuse them in your analyses.',
  'category.ecoscorePage.sections.transparency.cards.openData.cta':
    'Browse the data workspace',
  'category.ecoscorePage.sections.transparency.cards.openData.aria':
    'Browse open data resources for the {category} Impact Score',
  'category.ecoscorePage.sections.transparency.tableTitle':
    'Coefficient summary',
  'category.ecoscorePage.sections.transparency.tableHelper':
    'Comparison between the AI-generated proposal and the coefficients currently applied.',
  'category.ecoscorePage.sections.transparency.tableFallback':
    'Detailed coefficients will be published soon.',
  'category.ecoscorePage.sections.transparency.tableHeaders.name':
    'Criterion name',
  'category.ecoscorePage.sections.transparency.tableHeaders.proposed':
    'Proposed coef.',
  'category.ecoscorePage.sections.transparency.tableHeaders.applied':
    'Applied coef.',
  'category.ecoscorePage.sections.aiAudit.title':
    'Impact Score generation audit',
  'category.ecoscorePage.sections.aiAudit.intro':
    'Audit the prompts and responses used to configure the {category} Impact Score.',
  'category.ecoscorePage.sections.aiAudit.promptTitle':
    'YAML generation prompt',
  'category.ecoscorePage.sections.aiAudit.promptHelper':
    'Instructions sent to the model to produce the configuration.',
  'category.ecoscorePage.sections.aiAudit.responseTitle': 'Model response',
  'category.ecoscorePage.sections.aiAudit.responseHelper':
    'JSON output from the AI used as the basis of the configuration.',
  'category.ecoscorePage.sections.aiAudit.yamlUnavailable':
    'Prompt unavailable.',
  'category.ecoscorePage.sections.aiAudit.jsonUnavailable':
    'AI response unavailable.',
  'category.ecoscorePage.seo.title': 'Impact Score for {category}',
  'category.ecoscorePage.seo.description':
    'Impact Score insights for {category}.',
  'category.ecoscorePage.lifecycle.EXTRACTION': 'Extraction',
  'category.ecoscorePage.lifecycle.MANUFACTURING': 'Manufacturing',
  'category.ecoscorePage.lifecycle.TRANSPORTATION': 'Transportation',
  'category.ecoscorePage.lifecycle.USE': 'Use',
  'category.ecoscorePage.lifecycle.END_OF_LIFE': 'End of life',
}

const translate = (key: string, params: Record<string, unknown> = {}) => {
  const template = messages[key] ?? key
  return template.replace(/\{(\w+)\}/g, (_, match) =>
    String(params[match] ?? '')
  )
}

vi.mock('vue-i18n', () => ({
  useI18n: () => ({
    t: (key: string, params: Record<string, unknown> = {}) =>
      translate(key, params),
    locale: localeRef,
  }),
}))

const route = {
  params: { categorySlug: 'televisions' },
  fullPath: '/televisions/ecoscore',
}

mockNuxtImport('useRoute', () => () => route)
mockNuxtImport(
  'useRequestURL',
  () => () => new URL('https://example.com/televisions/ecoscore')
)
mockNuxtImport('useSeoMeta', () => useSeoMetaMock)
mockNuxtImport(
  'createError',
  () => (input: { statusMessage?: string } & Record<string, unknown>) => {
    const error = new Error(input?.statusMessage ?? 'Error')
    Object.assign(error, input)
    return error
  }
)

vi.mock('vuetify', () => ({
  useDisplay: () => ({ mdAndDown }),
}))

vi.mock('~/components/category/CategoryHero.vue', () => ({
  default: defineComponent({
    name: 'CategoryHeroStub',
    props: {
      breadcrumbs: { type: Array, default: () => [] },
      title: { type: String, default: '' },
      description: { type: String, default: '' },
      image: { type: String, default: '' },
      eyebrow: { type: String, default: '' },
    },
    setup(props) {
      return () =>
        h('header', { class: 'category-hero-stub' }, [
          h(
            'div',
            { 'data-test': 'hero-breadcrumbs' },
            (props.breadcrumbs as Array<{ title?: string }>)
              .map(item => item.title ?? '')
              .join(' / ')
          ),
          h('h1', { class: 'category-hero-stub__title' }, props.title),
        ])
    },
  }),
}))

vi.mock('~/components/shared/ui/StickySectionNavigation.vue', () => ({
  default: defineComponent({
    name: 'StickySectionNavigationStub',
    props: {
      sections: {
        type: Array as () => Array<{ id: string; label: string }>,
        default: () => [],
      },
    },
    emits: ['navigate'],
    setup(props, { emit }) {
      return () =>
        h(
          'nav',
          { class: 'sticky-nav-stub', 'data-test': 'sticky-nav' },
          (props.sections as Array<{ id: string; label: string }>).map(
            section =>
              h(
                'button',
                {
                  type: 'button',
                  class: 'sticky-nav-stub__item',
                  'data-section-id': section.id,
                  onClick: () => emit('navigate', section.id),
                },
                section.label
              )
          )
        )
    },
  }),
}))

vi.mock('~/components/domains/content/TextContent.vue', () => ({
  default: defineComponent({
    name: 'TextContentStub',
    props: {
      blocId: { type: String, default: '' },
    },
    setup(props) {
      return () =>
        h('div', { class: 'text-content-stub' }, `content:${props.blocId}`)
    },
  }),
}))

vi.mock('~/components/shared/ui/ImpactScore.vue', () => ({
  default: defineComponent({
    name: 'ImpactScoreStub',
    props: {
      score: { type: Number, default: 0 },
    },
    setup(props) {
      return () =>
        h('div', { class: 'impact-score-stub' }, `score:${props.score}`)
    },
  }),
}))

vi.mock('~/composables/categories/useCategories', () => ({
  useCategories: () => ({ selectCategoryBySlug: selectCategoryBySlugMock }),
}))

const categoryFixture = {
  id: 'tv',
  verticalHomeTitle: 'Televisions',
  verticalMetaTitle: 'TV',
  verticalHomeDescription: 'Find eco responsible televisions.',
  verticalMetaDescription: 'Eco score methodology for televisions.',
  imageMedium: 'https://example.com/images/tv-medium.jpg',
  breadCrumb: [{ title: 'Electronics', link: '/electronics' }],
  impactScoreConfig: {
    criteriasPonderation: {
      POWER: 0.3,
      REPAIRABILITY: 0.2,
    },
    texts: {
      purpose: 'Purpose text for televisions.',
      availlableDatas: 'Available data text.',
      criticalReview: 'Critical review text.',
      criteriasAnalysis: {
        POWER: 'Power analysis.',
        REPAIRABILITY: 'Repairability analysis.',
      },
    },
    yamlPrompt: 'key: value',
    aiJsonResponse: JSON.stringify({
      criteriasPonderation: {
        POWER: 0.35,
        REPAIRABILITY: 0.25,
      },
    }),
  },
  availableImpactScoreCriterias: ['POWER', 'REPAIRABILITY'],
  attributesConfig: {
    configs: [
      {
        key: 'POWER',
        name: 'Energy efficiency',
        icon: 'mdi-flash',
        participateInACV: new Set(['USE', 'TRANSPORTATION']),
      },
      {
        key: 'REPAIRABILITY',
        name: 'Repairability index',
        icon: 'mdi-tools',
        participateInACV: new Set(['MANUFACTURING', 'END_OF_LIFE']),
      },
    ],
  },
} as unknown as VerticalConfigFullDto

const vuetifyStubs = {
  NuxtLink: defineComponent({
    name: 'NuxtLinkStub',
    props: { to: { type: [String, Object], default: undefined } },
    setup(_props, { slots }) {
      return () => h('a', { class: 'nuxt-link-stub' }, slots.default?.())
    },
  }),
  'v-container': defineComponent({
    name: 'VContainerStub',
    setup(_props, { slots, attrs }) {
      return () =>
        h('div', { class: 'v-container-stub', ...attrs }, slots.default?.())
    },
  }),
  'v-row': { template: '<div class="v-row-stub"><slot /></div>' },
  'v-col': { template: '<div class="v-col-stub"><slot /></div>' },
  'v-sheet': { template: '<div class="v-sheet-stub"><slot /></div>' },
  'v-card': { template: '<div class="v-card-stub"><slot /></div>' },
  'v-card-text': { template: '<div class="v-card-text-stub"><slot /></div>' },
  'v-card-title': { template: '<div class="v-card-title-stub"><slot /></div>' },
  'v-btn': defineComponent({
    name: 'VBtnStub',
    setup(_props, { slots, attrs }) {
      return () =>
        h(
          'button',
          { class: 'v-btn-stub', type: 'button', ...attrs },
          slots.default?.()
        )
    },
  }),
  'v-icon': { template: '<i class="v-icon-stub"><slot /></i>' },
  'v-avatar': { template: '<div class="v-avatar-stub"><slot /></div>' },
  'v-img': defineComponent({
    name: 'VImgStub',
    props: {
      src: { type: String, default: '' },
      alt: { type: String, default: '' },
    },
    setup(props) {
      return () =>
        h('img', { class: 'v-img-stub', src: props.src, alt: props.alt })
    },
  }),
  'v-table': defineComponent({
    name: 'VTableStub',
    setup(_props, { slots, attrs }) {
      return () =>
        h('table', { class: 'v-table-stub', ...attrs }, slots.default?.())
    },
  }),
  'v-divider': { template: '<hr class="v-divider-stub" />' },
  'v-skeleton-loader': { template: '<div class="v-skeleton-loader-stub" />' },
  ImpactScoreCriteriaPanel: {
    template: '<div class="impact-score-criteria-panel-stub" />',
  },
}

const mountPage = async () => {
  const component = (await import('./ecoscore.vue')).default
  return mountSuspended(component, {
    global: {
      stubs: vuetifyStubs,
    },
  })
}

beforeAll(() => {
  class MockIntersectionObserver {
    callback: IntersectionObserverCallback
    constructor(callback: IntersectionObserverCallback) {
      this.callback = callback
    }
    observe() {}
    unobserve() {}
    disconnect() {}
  }
  // @ts-expect-error override for test environment
  globalThis.IntersectionObserver = MockIntersectionObserver
})

beforeEach(() => {
  mdAndDown.value = false
  localeRef.value = 'en-US'
  selectCategoryBySlugMock.mockClear()
  selectCategoryBySlugMock.mockImplementation(async () => categoryFixture)
  useSeoMetaMock.mockClear()
  // @ts-expect-error jsdom window
  window.scrollTo = vi.fn()
})

describe('Category ecosystem Impact Score page', () => {
  it('renders navigation and category-specific sections', async () => {
    const wrapper = await mountPage()
    await flushPromises()

    expect(selectCategoryBySlugMock).toHaveBeenCalledWith('televisions')

    const breadcrumbText = wrapper.get('[data-test="hero-breadcrumbs"]').text()
    expect(breadcrumbText).toContain('impactscore')

    const navItems = wrapper.findAll('.sticky-nav-stub__item')
    expect(navItems).toHaveLength(6)
    expect(navItems.map(item => item.text())).toEqual([
      'Available criteria',
      'Overview',
      'Purpose & data',
      'Criteria',
      'Transparency',
      'AI audit',
    ])

    const criteriaCards = wrapper.findAll('[data-test="impact-criteria-card"]')
    expect(criteriaCards).toHaveLength(2)
    expect(criteriaCards[0].text()).toContain('Energy efficiency')
    expect(criteriaCards[0].text()).toContain('30%')

    const sampleScore = wrapper.get('.impact-score-stub').text()
    expect(sampleScore).toContain('score:4.3')

    const orbit = wrapper.get('[data-test="impact-score-orbit"]')
    expect(orbit.text()).toContain('Impact Score Televisions')
    expect(orbit.text()).toContain('Transportation')
    expect(orbit.text()).toContain('End of life')

    const yamlBlock = wrapper.get('[data-test="ai-yaml"]').text()
    expect(yamlBlock).toContain('key: value')

    const jsonBlock = wrapper.get('[data-test="ai-json"]').text()
    expect(jsonBlock).toContain('"POWER"')
    expect(jsonBlock).toContain('0.35')

    const table = wrapper.get('[data-test="comparison-table"]').text()
    expect(table).toContain('Energy efficiency')
    expect(table).toContain('0.30')
    expect(table).toContain('0.35')

    const transparencyCards = wrapper.findAll(
      '.category-ecoscore__transparency-card'
    )
    expect(transparencyCards).toHaveLength(2)
    expect(transparencyCards[0].text()).toContain('Open-source methodology')
  })

  it('scrolls smoothly to the requested section via sticky navigation', async () => {
    const wrapper = await mountPage()
    await flushPromises()

    const scrollSpy = vi.spyOn(window, 'scrollTo')
    const originalGetElementById = document.getElementById.bind(document)
    vi.spyOn(document, 'getElementById').mockImplementation((id: string) => {
      return (
        originalGetElementById(id) ??
        (wrapper.element.querySelector(`#${id}`) as HTMLElement | null)
      )
    })

    const navItems = wrapper.findAll('.sticky-nav-stub__item')
    await navItems[1].trigger('click')

    expect(scrollSpy).toHaveBeenCalled()
    const [firstCall] = scrollSpy.mock.calls
    expect(firstCall?.[0]).toMatchObject({ behavior: 'smooth' })
  })
})
