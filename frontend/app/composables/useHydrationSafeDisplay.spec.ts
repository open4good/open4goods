import { describe, expect, it, vi } from 'vitest'
import { defineComponent, h, nextTick } from 'vue'
import { mount } from '@vue/test-utils'

import { useHydrationSafeDisplay } from './useHydrationSafeDisplay'

vi.mock('vuetify', () => ({
  useDisplay: () => ({ mdAndDown: { value: false } }),
}))

describe('useHydrationSafeDisplay', () => {
  it('keeps the server value until mounted, then follows the viewport', async () => {
    const seen: boolean[] = []
    const Probe = defineComponent({
      setup() {
        const { mdAndDown } = useHydrationSafeDisplay()
        // Value used by the first (hydration) render
        seen.push(mdAndDown.value)
        return () => h('div', String(mdAndDown.value))
      },
    })

    const wrapper = mount(Probe)
    expect(seen[0]).toBe(true)
    await nextTick()
    expect(wrapper.text()).toBe('false')
  })
})
