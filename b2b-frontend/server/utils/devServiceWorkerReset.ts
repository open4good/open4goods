import { setResponseHeader } from 'h3'
import type { H3Event } from 'h3'

const resetWorkerScript = `
self.addEventListener('install', (event) => {
  self.skipWaiting()
})

self.addEventListener('activate', (event) => {
  event.waitUntil((async () => {
    const keys = await caches.keys()
    await Promise.all(keys.map((key) => caches.delete(key)))
    await self.registration.unregister()
    const clientsList = await self.clients.matchAll({ type: 'window', includeUncontrolled: true })
    for (const client of clientsList) {
      client.navigate(client.url)
    }
  })())
})
`

export function sendDevServiceWorkerReset(event: H3Event) {
  if (!import.meta.dev) {
    setResponseHeader(event, 'Cache-Control', 'no-store')
    return ''
  }

  setResponseHeader(event, 'Content-Type', 'application/javascript; charset=utf-8')
  setResponseHeader(event, 'Cache-Control', 'no-store, no-cache, must-revalidate, proxy-revalidate')
  setResponseHeader(event, 'Service-Worker-Allowed', '/')
  return resetWorkerScript
}
