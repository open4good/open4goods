import { proxyRequest } from 'h3'

export default defineEventHandler(async event => {
  // BlogController in UI service runs on port 8082
  const target = 'http://localhost:8082'
  return proxyRequest(event, target + event.path)
})
