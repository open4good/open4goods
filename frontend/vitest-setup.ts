import { vi } from 'vitest'

// Silence Nuxt manifest fetching background tasks that cause timeouts in tests
// especially when fake timers are used.
// We only stub $fetch globally here because it's safer than mocking aliases.
vi.stubGlobal('$fetch', (request: string) => {
  if (typeof request === 'string' && request.includes('/builds/meta/')) {
    return Promise.resolve({})
  }
  // Return a promise that resolves to null for other requests.
  // Individual tests that need to mock $fetch will call vi.stubGlobal('$fetch', ...) themselves.
  return Promise.resolve(null)
})

vi.stubGlobal('defineRouteRules', vi.fn())

// Ensure window and performance exist to prevent ReferenceError in i18n libraries
if (typeof globalThis.window === 'undefined') {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  globalThis.window = globalThis as any
}
if (!globalThis.performance) {
  // @ts-expect-error global.performance does not exist by default
  globalThis.performance = { now: vi.fn(() => Date.now()) }
}
if (!globalThis.window.performance) {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  globalThis.window.performance = globalThis.performance as any
}
