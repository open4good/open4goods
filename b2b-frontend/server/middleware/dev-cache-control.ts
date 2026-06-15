import { getRequestURL, setResponseHeader } from 'h3'

const localDevelopmentHosts = new Set(['localhost', '127.0.0.1', '::1'])

export default defineEventHandler((event) => {
  if (!import.meta.dev) {
    return
  }

  const requestUrl = getRequestURL(event)
  if (!localDevelopmentHosts.has(requestUrl.hostname)) {
    return
  }

  setResponseHeader(event, 'Cache-Control', 'no-store, no-cache, must-revalidate, proxy-revalidate')
  setResponseHeader(event, 'Pragma', 'no-cache')
  setResponseHeader(event, 'Expires', '0')
  setResponseHeader(event, 'Surrogate-Control', 'no-store')
})
