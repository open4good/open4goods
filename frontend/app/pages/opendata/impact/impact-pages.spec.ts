import { mountSuspended, mockNuxtImport } from '@nuxt/test-utils/runtime'
import { describe, expect, it, vi } from 'vitest'
import { defineComponent, h, ref } from 'vue'

import type { OpenDataDatasetDto } from '~~/shared/api-client'
import frFR from '../../../../i18n/locales/fr-FR.json'

type MessageTree = { [key: string]: string | MessageTree }

const lookup = (tree: MessageTree, key: string): string => {
  const value = key
    .split('.')
    .reduce<string | MessageTree | undefined>(
      (node, part) =>
        node && typeof node !== 'string' ? node[part] : undefined,
      tree
    )
  return typeof value === 'string' ? value : key
}

const translate = (key: string, params?: Record<string, unknown>) =>
  Object.entries(params ?? {}).reduce(
    (text, [name, value]) => text.replace(`{${name}}`, String(value)),
    lookup(frFR as MessageTree, key)
  )

const state = vi.hoisted(() => ({
  datasets: [] as OpenDataDatasetDto[],
  head: null as null | (() => { script?: { innerHTML?: string }[] }),
}))

vi.mock('vue-i18n', () => ({
  useI18n: () => ({ t: translate, locale: { value: 'fr-FR' } }),
}))
vi.mock('~/composables/categories/useCategories', () => ({
  useCategories: () => ({
    categories: ref([{ id: 'dishwasher', verticalHomeUrl: 'lave-vaisselles' }]),
    fetchCategories: vi.fn(async () => []),
  }),
}))
vi.mock('~/composables/useAnalytics', () => ({
  useAnalytics: () => ({ trackOpenDataOutboundClick: vi.fn() }),
}))

mockNuxtImport('useLocalePath', () => () => (path: string) => `/${path}`)
mockNuxtImport('useRequestURL', () => () => new URL('https://nudger.test/'))
mockNuxtImport('useRequestHeaders', () => () => ({}))
mockNuxtImport('useSeoMeta', () => vi.fn())
mockNuxtImport('useRoute', () => () => ({ params: { vertical: 'dishwasher' } }))
mockNuxtImport('useHead', () =>
  vi.fn((factory: typeof state.head) => {
    state.head = factory
  })
)
mockNuxtImport('useAsyncData', () => (key: unknown) => {
  const isHub = key === 'opendata-impact-hub'
  const result = {
    data: ref(isHub ? state.datasets : state.datasets[0]),
    pending: ref(false),
    error: ref(null),
    refresh: vi.fn(),
  }
  return Object.assign(Promise.resolve(result), result)
})

const stub = (name: string) =>
  defineComponent({ name, setup: () => () => h('div', { class: name }) })

const stubs = {
  PageHeader: stub('PageHeader'),
  OpendataMirrors: stub('OpendataMirrors'),
  OpendataLicenseSection: stub('OpendataLicenseSection'),
  OpendataCitation: stub('OpendataCitation'),
}

const dishwasher: OpenDataDatasetDto = {
  type: 'IMPACT',
  id: 'impact-dishwasher',
  recordCount: '52 013',
  recordCountValue: 52013,
  lastUpdated: '22 nov. 2026, 03:55:00',
  generatedAt: '2026-11-22T02:55:00Z',
  fileSize: '4,1 MB',
  sizeBytes: 4_100_000,
  sha256: 'def456',
  downloadUrl: 'https://data.nudger.fr/impact-dishwasher.zip',
  headers: ['gtin', 'warranty_year'],
  columns: [
    { name: 'gtin', fillRate: 1, description: 'Code-barres' },
    {
      name: 'warranty_year',
      fillRate: 0.61,
      unit: 'Year(s)',
      description: 'Durée de garantie',
      source: 'Attribut WARRANTY (EPREL, Icecat, marchands)',
    },
  ],
}

describe('/opendata/impact hub', () => {
  it('lists each dataset with the manifest rows and explains that the score is not published', async () => {
    state.datasets = [dishwasher]
    const component = (await import('./index.vue')).default
    const wrapper = await mountSuspended(component, { global: { stubs } })

    expect(wrapper.find('[data-testid="impact-rows"]').text()).toBe('52 013')
    expect(wrapper.text()).toContain('Lave-vaisselle')
    expect(wrapper.find('[data-testid="impact-no-score"]').text()).toContain(
      'score agrégé'
    )

    const jsonLd = JSON.parse(state.head?.().script?.[0]?.innerHTML ?? '{}')
    expect(jsonLd['@type']).toBe('Dataset')
    expect(jsonLd.spatialCoverage).toBe('France')
    expect(jsonLd.distribution[0].contentUrl).toBe(
      'https://data.nudger.fr/impact-dishwasher.zip'
    )
  })

  it('says so when no dataset is listed', async () => {
    state.datasets = []
    const component = (await import('./index.vue')).default
    const wrapper = await mountSuspended(component, { global: { stubs } })

    expect(wrapper.find('[data-testid="impact-hub-empty"]').exists()).toBe(true)
  })
})

describe('/opendata/impact/[vertical] page', () => {
  it('shows the published criteria with their fill rate and source, and links to the category', async () => {
    state.datasets = [dishwasher]
    const component = (await import('./[vertical].vue')).default
    const wrapper = await mountSuspended(component, { global: { stubs } })
    const text = wrapper.text()

    expect(text).toContain('52 013')
    expect(text).toContain('warranty_year')
    expect(text).toContain('Attribut WARRANTY (EPREL, Icecat, marchands)')
    expect(
      wrapper
        .findAll('[data-testid="opendata-fill-rate"]')
        .map(cell => cell.text())
    ).toEqual(['100 %', '61 %'])
    expect(
      wrapper.findComponent('[data-testid="impact-category-link"]').props('to')
    ).toBe('/lave-vaisselles')
    expect(text).toContain(
      'curl -C - -L -O https://data.nudger.fr/impact-dishwasher.zip'
    )

    const jsonLd = JSON.parse(state.head?.().script?.[0]?.innerHTML ?? '{}')
    expect(jsonLd.dateModified).toBe('2026-11-22T02:55:00Z')
    expect(
      jsonLd.variableMeasured.map((v: { name: string }) => v.name)
    ).toEqual(['gtin', 'warranty_year'])
  })
})
