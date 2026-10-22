import { mountSuspended } from '@nuxt/test-utils/runtime'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import OpendataMirrors from './OpendataMirrors.vue'

const trackOpenDataOutboundClick = vi.fn()

vi.mock('~/composables/useAnalytics', () => ({
  useAnalytics: () => ({ trackOpenDataOutboundClick }),
}))

vi.mock('vue-i18n', () => ({
  useI18n: () => ({
    t: (key: string) => key,
  }),
}))

describe('OpendataMirrors', () => {
  beforeEach(() => trackOpenDataOutboundClick.mockClear())

  it('renders the four mirrors in a new tab with their license', async () => {
    const wrapper = await mountSuspended(OpendataMirrors, {
      props: { page: 'gtin' },
    })
    const links = wrapper.findAll('a[data-mirror]')

    expect(links.map(link => link.attributes('data-mirror'))).toEqual([
      'datagouv',
      'zenodo',
      'kaggle',
      'figshare',
    ])
    for (const link of links) {
      expect(link.attributes('target')).toBe('_blank')
      expect(link.attributes('rel')).toContain('noopener')
    }
    expect(wrapper.text()).toContain('opendata.mirrors.items.zenodo.license')
  })

  it('tracks each mirror click with block, target and page', async () => {
    const wrapper = await mountSuspended(OpendataMirrors, {
      props: { page: 'isbn' },
    })

    await wrapper.find('a[data-mirror="kaggle"]').trigger('click')

    expect(trackOpenDataOutboundClick).toHaveBeenCalledWith({
      block: 'mirrors',
      target: 'kaggle',
      page: 'isbn',
    })
  })
})
