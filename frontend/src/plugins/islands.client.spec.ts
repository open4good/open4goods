import { describe, it, expect, vi } from 'vitest'
import plugin from './islands.client'
import LazyHydrate from 'vue-lazy-hydration'

vi.mock('#app', () => ({
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  defineNuxtPlugin: (fn: any) => fn
}))

describe('islands plugin', () => {
  it('registers LazyHydrate component', () => {
    const component = vi.fn()
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const nuxtApp = { vueApp: { component } } as any
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    ;(plugin as any)(nuxtApp)
    expect(component).toHaveBeenCalledWith('LazyHydrate', LazyHydrate)
  })
})
