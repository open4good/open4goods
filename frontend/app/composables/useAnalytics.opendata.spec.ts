import { mockNuxtImport } from '@nuxt/test-utils/runtime'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { useAnalytics } from './useAnalytics'

const { trackEventSpy, dntState } = vi.hoisted(() => ({
  trackEventSpy: vi.fn(),
  dntState: { enabled: false },
}))

mockNuxtImport('useNuxtApp', () => () => ({
  $plausible: { trackEvent: trackEventSpy },
}))
mockNuxtImport('useRuntimeConfig', () => () => ({
  public: { plausible: { enabled: true } },
}))

vi.mock('~/utils/do-not-track', () => ({
  isDoNotTrackEnabled: () => dntState.enabled,
}))

describe('trackOpenDataOutboundClick', () => {
  beforeEach(() => {
    trackEventSpy.mockClear()
    dntState.enabled = false
  })

  it('emits open-data-link with low-cardinality props and no GTIN', () => {
    useAnalytics().trackOpenDataOutboundClick({
      block: 'explore-products',
      target: 'product',
      page: 'opendata',
      category: 'tv',
    })

    expect(trackEventSpy).toHaveBeenCalledWith('open-data-link', {
      props: {
        block: 'explore-products',
        target: 'product',
        page: 'opendata',
        category: 'tv',
      },
    })
  })

  it('omits the category when not applicable', () => {
    useAnalytics().trackOpenDataOutboundClick({
      block: 'mirrors',
      target: 'zenodo',
      page: 'gtin',
    })

    expect(trackEventSpy).toHaveBeenCalledWith('open-data-link', {
      props: { block: 'mirrors', target: 'zenodo', page: 'gtin' },
    })
  })

  it('respects Do Not Track', () => {
    dntState.enabled = true
    useAnalytics().trackOpenDataOutboundClick({
      block: 'mirrors',
      target: 'zenodo',
      page: 'gtin',
    })

    expect(trackEventSpy).not.toHaveBeenCalled()
  })
})
