import { mountSuspended } from '@nuxt/test-utils/runtime'
import { flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import BlogOfferBlocks from './BlogOfferBlocks.vue'

const trackEvent = vi.fn()
vi.mock('~/composables/useAnalytics', () => ({
  useAnalytics: () => ({ trackEvent }),
}))

const filled = new Set(['tv-55'])

vi.mock('~/components/product/GuideProductGrid.vue', () => ({
  default: {
    name: 'GuideProductGridStub',
    props: ['vertical', 'filters', 'requireOffers', 'hideWhenEmpty'],
    setup(
      props: { filters: unknown[] },
      { attrs }: { attrs: Record<string, string> }
    ) {
      return { show: filled.has(attrs['data-block'] ?? ''), props }
    },
    template:
      '<div v-if="show"><slot name="header" /><a href="/televiseurs/x">p</a><slot name="footer" /></div>',
  },
}))

describe('BlogOfferBlocks', () => {
  beforeEach(() => trackEvent.mockReset())

  it('renders nothing for an article without config', async () => {
    const wrapper = await mountSuspended(BlogOfferBlocks, {
      props: { slug: 'autre' },
    })
    expect(wrapper.find('[data-test="blog-offer-blocks"]').exists()).toBe(false)
  })

  it('hides empty blocks and keeps filled ones', async () => {
    const wrapper = await mountSuspended(BlogOfferBlocks, {
      props: { slug: 'quelle-taille-de-tv-choisir-pour-son-salon' },
    })
    await flushPromises()
    expect(wrapper.findAll('h2')).toHaveLength(1)
    expect(wrapper.find('a[href="/televiseurs"]').exists()).toBe(true)
  })

  it('emits an analytics event on card click', async () => {
    const wrapper = await mountSuspended(BlogOfferBlocks, {
      props: { slug: 'quelle-taille-de-tv-choisir-pour-son-salon' },
    })
    await wrapper.find('a[href="/televiseurs/x"]').trigger('click')
    expect(trackEvent).toHaveBeenCalledWith('blog-offer-block-click', {
      props: {
        article: 'quelle-taille-de-tv-choisir-pour-son-salon',
        vertical: 'tv',
        block: 'tv-55',
      },
    })
  })
})
