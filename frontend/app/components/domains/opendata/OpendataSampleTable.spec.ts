import { mountSuspended } from '@nuxt/test-utils/runtime'
import { describe, expect, it, vi } from 'vitest'

import OpendataSampleTable from './OpendataSampleTable.vue'

vi.mock('vue-i18n', () => ({
  useI18n: () => ({
    t: (key: string) => key,
  }),
}))

describe('OpendataSampleTable', () => {
  it('renders the header and the rows of the sample', async () => {
    const wrapper = await mountSuspended(OpendataSampleTable, {
      props: {
        headers: ['gtin', 'brand'],
        rows: [
          ['123', 'Acme'],
          ['456', ''],
        ],
      },
    })

    expect(wrapper.findAll('thead th').map(th => th.text())).toEqual([
      'gtin',
      'brand',
    ])
    expect(wrapper.findAll('tbody tr')).toHaveLength(2)
    expect(wrapper.find('tbody tr td').text()).toBe('123')
  })

  it('renders nothing without a sample', async () => {
    const wrapper = await mountSuspended(OpendataSampleTable, {
      props: { headers: ['gtin'], rows: [] },
    })

    expect(wrapper.find('[data-testid="opendata-sample-table"]').exists()).toBe(
      false
    )
  })
})
