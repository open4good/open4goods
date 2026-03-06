import { proxyRequest } from 'h3'

export default defineEventHandler(event => {
  const config = useRuntimeConfig(event)
  return proxyRequest(
    event,
    `${config.apiUrl}${event.path.replace('/api', '')}`
  )
})
