const localDevelopmentHosts = new Set(['localhost', '127.0.0.1', '::1'])
const reloadMarker = 'infera-dev-cache-reset-reloaded'

export default defineNuxtPlugin(() => {
  if (!import.meta.dev || !localDevelopmentHosts.has(window.location.hostname)) {
    return
  }

  window.addEventListener('load', () => {
    void resetLocalDevelopmentCaches()
  }, { once: true })
})

async function resetLocalDevelopmentCaches() {
  let changed = false

  if ('serviceWorker' in navigator) {
    const registrations = await navigator.serviceWorker.getRegistrations()
    for (const registration of registrations) {
      if (registration.scope.startsWith(window.location.origin)) {
        changed = await registration.unregister() || changed
      }
    }
  }

  if ('caches' in window) {
    const keys = await caches.keys()
    await Promise.all(keys.map(async (key) => {
      changed = await caches.delete(key) || changed
    }))
  }

  if (!changed) {
    sessionStorage.removeItem(reloadMarker)
    return
  }

  if (!sessionStorage.getItem(reloadMarker)) {
    sessionStorage.setItem(reloadMarker, 'true')
    window.location.reload()
  }
}
