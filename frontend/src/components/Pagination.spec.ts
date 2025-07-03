import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import Pagination from './Pagination.vue'

vi.mock('#imports', () => ({
  useI18n: () => ({ t: (key: string) => key })
}))

const meta = { number: 0, size: 10, totalElements: 20, totalPages: 2 }

describe('Pagination', () => {
  it('emits change event when next page clicked', async () => {
    const wrapper = mount(Pagination, { props: { meta } })
    await wrapper.findAll('button')[1].trigger('click')
    expect(wrapper.emitted('change')?.[0]).toEqual([1])
  })

  it('disables previous button on first page', () => {
    const wrapper = mount(Pagination, { props: { meta } })
    expect(wrapper.findAll('button')[0].attributes()).toHaveProperty('disabled')
  })
})
