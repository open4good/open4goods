import { describe, expect, it, vi } from 'vitest'
import { ref } from 'vue'
import plugin from '~/plugins/affiliation-content.client'

const state = ref<string | null>(null)
const route = ref({ path: '/' })
const hooks: Array<() => void> = []

vi.mock('#imports', () => ({
  defineNuxtPlugin: (p: unknown) => p,
}))
vi.mock('~/composables/useAffiliationContentPath', () => ({
  useAffiliationContentPath: () => state,
}))

const navigate = (path: string) => {
  route.value = { path }
  hooks.forEach(h => h())
}

describe('affiliation-content plugin (GOU-1829)', () => {
  it('keeps the last article through product pages, not the home page', () => {
    ;(plugin as unknown as { setup: (a: unknown) => void }).setup({
      $router: { currentRoute: route },
      hook: (_n: string, cb: () => void) => hooks.push(cb),
    })
    expect(state.value).toBeNull()

    navigate('/')
    expect(state.value).toBeNull()

    navigate('/fours/a')
    expect(state.value).toBeNull()

    navigate('/blog/mon-article')
    navigate('/fours/a')
    navigate('/fours/b')
    expect(state.value).toBe('/blog/mon-article')

    navigate('/guides')
    expect(state.value).toBe('/blog/mon-article')
    navigate('/guides/four')
    expect(state.value).toBe('/guides/four')
  })
})
