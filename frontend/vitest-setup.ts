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
