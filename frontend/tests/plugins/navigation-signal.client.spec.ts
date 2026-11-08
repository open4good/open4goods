import { beforeEach, describe, expect, it, vi } from 'vitest'
import plugin from '~/plugins/navigation-signal.client'

type Hook = (name: string, callback: () => void) => void

const runSetup = (hook: Hook) =>
  (plugin as unknown as { setup: (app: { hook: Hook }) => void }).setup({
    hook,
  })

const clearCookie = () => {
  document.cookie = 'nudger_nav=; Path=/; Max-Age=0'
}

describe('navigation-signal plugin', () => {
  beforeEach(clearCookie)

  it('writes the first-party navigation cookie when the app starts', () => {
    runSetup(vi.fn())

    expect(document.cookie).toContain('nudger_nav=1')
  })

  it('refreshes the cookie after each client-side page navigation', () => {
    const hook = vi.fn<Hook>()
    runSetup(hook)
    clearCookie()

    expect(hook).toHaveBeenCalledWith('page:finish', expect.any(Function))
    hook.mock.calls[0]?.[1]()

    expect(document.cookie).toContain('nudger_nav=1')
  })
})
