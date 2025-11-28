import http from 'node:http'
import https from 'node:https'

// Singleton agents for connection pooling and keep-alive
let httpAgent: http.Agent | undefined
let httpsAgent: https.Agent | undefined

/**
 * Create or get HTTP agent with optimized connection pooling
 */
export function createHttpAgent(): http.Agent {
  if (!httpAgent) {
    httpAgent = new http.Agent({
      keepAlive: true,
      keepAliveMsecs: 30000, // Send keep-alive probe every 30s
      maxSockets: 50, // Max concurrent sockets
      maxFreeSockets: 10, // Keep up to 10 free sockets
      timeout: 30000, // Socket timeout
      freeSocketTimeout: 30000, // Free socket timeout
    })
  }
  return httpAgent
}

/**
 * Create or get HTTPS agent with optimized connection pooling
 */
export function createHttpsAgent(): https.Agent {
  if (!httpsAgent) {
    httpsAgent = new https.Agent({
      keepAlive: true,
      keepAliveMsecs: 30000, // Send keep-alive probe every 30s
      maxSockets: 50, // Max concurrent sockets
      maxFreeSockets: 10, // Keep up to 10 free sockets
      timeout: 30000, // Socket timeout
      freeSocketTimeout: 30000, // Free socket timeout
    })
  }
  return httpsAgent
}
