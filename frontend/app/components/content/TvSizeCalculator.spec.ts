import { mountSuspended } from '@nuxt/test-utils/runtime'
import { clearNuxtData } from '#imports'
import { flushPromises } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import TvSizeCalculator from './TvSizeCalculator.vue'

const trackEventMock = vi.hoisted(() => vi.fn())
const fetchCategoryByIdMock = vi.hoisted(() => vi.fn())
vi.mock('~/composables/categories/useCategories', () => ({
  useCategories: () => ({ fetchCategoryById: fetchCategoryByIdMock }),
}))
vi.mock('~/composables/useAnalytics', () => ({
  useAnalytics: () => ({ trackEvent: trackEventMock }),
}))

describe('TvSizeCalculator', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    fetchCategoryByIdMock.mockResolvedValue({
      verticalHomeUrl: 'televiseurs',
      subCategories: [
        { id: 'medium_screens', slug: 'ecrans-moyens' },
        { id: 'large_screens', slug: 'grands-ecrans' },
      ],
    })
  })

  afterEach(() => {
    vi.useRealTimers()
    trackEventMock.mockReset()
    fetchCategoryByIdMock.mockReset()
    clearNuxtData()
  })

  it('renders the default result (3 m, 4K) without any interaction', async () => {
    const wrapper = await mountSuspended(TvSizeCalculator)
    const text = wrapper.get('[data-test="tv-size-result"]').text()
    expect(text).toContain('74')
    expect(text).toContain('98')
    expect(text).toContain('85')
    expect(
      wrapper.get('[data-test="tv-size-result"]').attributes()
    ).toMatchObject({ 'aria-live': 'polite' })
    expect(trackEventMock).not.toHaveBeenCalled()
  })

  it('exposes labelled inputs and a radio fieldset', async () => {
    const wrapper = await mountSuspended(TvSizeCalculator)
    const input = wrapper.get('[data-test="tv-size-distance"]')
    expect(
      wrapper.find(`label[for="${input.attributes('id')}"]`).exists()
    ).toBe(true)
    expect(wrapper.find('fieldset legend').exists()).toBe(true)
    expect(wrapper.findAll('input[type="radio"]')).toHaveLength(2)
  })

  it('recomputes and tracks one debounced calcul event on change', async () => {
    const wrapper = await mountSuspended(TvSizeCalculator)
    await wrapper.get('[data-test="tv-size-distance"]').setValue('2')
    await wrapper.get('[data-test="tv-size-resolution-full-hd"]').setValue()
    const text = wrapper.get('[data-test="tv-size-result"]').text()
    expect(text).toContain('39')
    expect(text).toContain('43')

    expect(trackEventMock).not.toHaveBeenCalled()
    vi.advanceTimersByTime(800)
    expect(trackEventMock).toHaveBeenCalledTimes(1)
    expect(trackEventMock).toHaveBeenCalledWith('calculateur_tv', {
      props: { action: 'calcul', taille: 43, resolution: 'full-hd' },
    })
  })

  it('links to the sub-category of the size and tracks the click', async () => {
    const wrapper = await mountSuspended(TvSizeCalculator)
    await flushPromises()
    const link = wrapper.get('[data-test="tv-size-link"]')
    expect(link.attributes('href')).toBe('/televiseurs/grands-ecrans')
    await link.trigger('click')
    expect(trackEventMock).toHaveBeenCalledWith('calculateur_tv', {
      props: { action: 'clic_sous_categorie', taille: 85, resolution: '4k' },
    })
  })

  it('hides the link when the category is unavailable', async () => {
    fetchCategoryByIdMock.mockRejectedValue(new Error('down'))
    const wrapper = await mountSuspended(TvSizeCalculator)
    expect(wrapper.find('[data-test="tv-size-link"]').exists()).toBe(false)
  })
})
