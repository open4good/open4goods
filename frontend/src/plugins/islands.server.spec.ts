import { describe, it, expect, vi } from 'vitest'
import plugin from './islands.server'

vi.mock('#app', () => ({
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  defineNuxtPlugin: (fn: any) => fn
}))

describe('islands server plugin', () => {
  it('registers LazyHydrate stub', () => {
    const component = vi.fn()
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const nuxtApp = { vueApp: { component } } as any
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    ;(plugin as any)(nuxtApp)
    expect(component).toHaveBeenCalled()
    const args = component.mock.calls[0]
    expect(args[0]).toBe('LazyHydrate')
    const comp = args[1]
    const slot = vi.fn()
    const render = comp.setup({}, { slots: { default: slot } })
    render()
    expect(slot).toHaveBeenCalledWith({ hydrated: true })
  })
})
