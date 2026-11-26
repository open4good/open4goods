import { mountSuspended, mockNuxtImport } from '@nuxt/test-utils/runtime'
import { describe, expect, it, vi } from 'vitest'
import { defineComponent, h, ref } from 'vue'

import type { OpenDataDatasetDto } from '~~/shared/api-client'
import enUS from '../../../i18n/locales/en-US.json'
import frFR from '../../../i18n/locales/fr-FR.json'

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
  dataset: null as OpenDataDatasetDto | null,
  head: null as null | (() => { script?: { innerHTML?: string }[] }),
}))

vi.mock('vue-i18n', () => ({
  useI18n: () => ({ t: translate, locale: { value: 'fr-FR' } }),
}))
vi.mock('~/composables/useAnalytics', () => ({
  useAnalytics: () => ({ trackOpenDataDownload: vi.fn() }),
}))

mockNuxtImport('useLocalePath', () => () => (path: string) => `/${path}`)
mockNuxtImport('useRequestURL', () => () => new URL('https://nudger.test/'))
mockNuxtImport('useRequestHeaders', () => () => ({}))
mockNuxtImport('useSeoMeta', () => vi.fn())
mockNuxtImport('useHead', () =>
  vi.fn((factory: typeof state.head) => {
    state.head = factory
  })
)
mockNuxtImport('useAsyncData', () => () => ({
  data: ref(state.dataset ? { dataset: state.dataset } : null),
  pending: ref(false),
  error: ref(null),
  refresh: vi.fn(),
}))

const stub = (name: string) =>
  defineComponent({ name, setup: () => () => h('div', { class: name }) })

const mountPage = async (page: 'gtin' | 'isbn') => {
  const component =
    page === 'gtin'
      ? (await import('./gtin.vue')).default
      : (await import('./isbn.vue')).default
  return mountSuspended(component, {
    global: {
      stubs: {
        PageHeader: stub('PageHeader'),
        OpendataExploreProducts: stub('OpendataExploreProducts'),
        OpendataMirrors: stub('OpendataMirrors'),
        OpendataLicenseSection: stub('OpendataLicenseSection'),
        OpendataCitation: stub('OpendataCitation'),
        OpendataFaqSection: stub('OpendataFaqSection'),
      },
    },
  })
}

const nullDataset = (type: 'GTIN' | 'ISBN'): OpenDataDatasetDto => ({
  type,
  recordCount: null,
  recordCountValue: null,
  lastUpdated: null,
  fileSize: null,
  sha256: null,
  downloadUrl: null,
  headers: [],
  columns: [],
})

describe.each(['gtin', 'isbn'] as const)('/opendata/%s page', page => {
  it('tolerates a manifest that is not readable yet: placeholders, no NaN, no 1970', async () => {
    state.dataset = nullDataset(page === 'gtin' ? 'GTIN' : 'ISBN')

    const wrapper = await mountPage(page)
    const summary = wrapper.find('.dataset-summary').text()

    expect(summary).toContain(
      `${translate('opendata.datasets.common.summary.records')}-`
    )
    expect(wrapper.text()).not.toMatch(/NaN|1970|undefined|null/)
    expect(wrapper.text()).not.toContain('SHA-256')
    expect(
      wrapper.find('.dataset-download__cta[href="#"]').classes()
    ).toContain('v-btn--disabled')
  })

  it('shows the manifest values and never a download rate limit', async () => {
    state.dataset = {
      ...nullDataset(page === 'gtin' ? 'GTIN' : 'ISBN'),
      recordCount: '115 786 654',
      recordCountValue: 115786654,
      lastUpdated: '22 nov. 2026, 03:55:00',
      fileSize: '6,4 GB',
      sha256: 'abc123',
      downloadUrl: `https://data.nudger.fr/${page}-open-data.zip`,
    }

    const wrapper = await mountPage(page)
    const text = wrapper.text()

    expect(text).toContain('115 786 654')
    expect(text).toContain('22 nov. 2026, 03:55:00')
    expect(text).toContain('6,4 GB')
    expect(text).toContain('SHA-256 : abc123')
    expect(text).toContain(
      translate('opendata.datasets.common.download.direct.resume')
    )
    expect(text).not.toMatch(/débit|simultan|bande passante/i)
    expect(
      wrapper
        .find(`a[href="https://data.nudger.fr/${page}-open-data.zip"]`)
        .exists()
    ).toBe(true)
  })
})

describe.each(['gtin', 'isbn'] as const)(
  '/opendata/%s manifest figures',
  page => {
    const manifestDataset = (): OpenDataDatasetDto => ({
      ...nullDataset(page === 'gtin' ? 'GTIN' : 'ISBN'),
      id: page,
      recordCount: '8 703 397',
      recordCountValue: 8703397,
      lastUpdated: '22 nov. 2026, 03:55:00',
      generatedAt: '2026-11-22T02:55:00Z',
      fileSize: '210 MB',
      sizeBytes: 210_000_000,
      sha256: 'abc123',
      downloadUrl: `https://data.nudger.fr/${page}-open-data.zip`,
      headers: ['gtin'],
      columns: [{ name: 'gtin', fillRate: 0.4234 }],
    })

    it('shows the fill rate of each column from the manifest', async () => {
      state.dataset = manifestDataset()

      const wrapper = await mountPage(page)

      expect(wrapper.find('[data-testid="opendata-fill-rate"]').text()).toBe(
        '42.3 %'
      )
    })

    it('publishes in the JSON-LD the same figures as the manifest', async () => {
      state.dataset = manifestDataset()

      await mountPage(page)
      const jsonLd = JSON.parse(
        state.head?.().script?.[0]?.innerHTML ?? '{}'
      ) as {
        '@type': string
        dateModified: string
        contentSize: string
        license: string
        creator: { '@type': string }
        distribution: { contentUrl: string; sha256: string }[]
      }

      expect(jsonLd['@type']).toBe('Dataset')
      expect(jsonLd.dateModified).toBe('2026-11-22T02:55:00Z')
      expect(jsonLd.contentSize).toBe('210 MB')
      expect(jsonLd.distribution[0]?.contentUrl).toBe(
        `https://data.nudger.fr/${page}-open-data.zip`
      )
      expect(jsonLd.distribution[0]?.sha256).toBe('abc123')
      expect(jsonLd.creator['@type']).toBe('Organization')
      expect(jsonLd.license).toContain('odbl')
    })

    it('renders the resumable download and reading examples', async () => {
      state.dataset = manifestDataset()

      const text = (await mountPage(page)).text()

      expect(text).toContain(
        `curl -C - -L -O https://data.nudger.fr/${page}-open-data.zip`
      )
      expect(text).toContain(
        `wget -c https://data.nudger.fr/${page}-open-data.zip`
      )
      expect(text).toContain('abc123')
      expect(text).toContain('pd.read_csv')
    })
  }
)

describe('open data wording', () => {
  it.each([
    ['fr-FR', frFR],
    ['en-US', enUS],
  ])('%s no longer mentions download rate limits', (_, messages) => {
    const opendata = JSON.stringify(
      (messages as unknown as { opendata: MessageTree }).opendata
    )

    expect(opendata).not.toMatch(
      /débit|simultan|bande passante|bandwidth|per client|concurrent|traffic/i
    )
  })
})
