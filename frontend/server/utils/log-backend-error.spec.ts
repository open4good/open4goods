import { describe, expect, it } from 'vitest'
import { FetchError, ResponseError } from '~~/shared/api-client'

import { extractBackendErrorDetails } from './log-backend-error'

describe('extractBackendErrorDetails', () => {
  it('returns backend response details when the error is a ResponseError', async () => {
    const response = new Response('Backend failure', {
      status: 502,
      statusText: 'Bad Gateway',
    })
    const error = new ResponseError(response)

    const details = await extractBackendErrorDetails(error)

    expect(details.statusCode).toBe(502)
    expect(details.statusText).toBe('Bad Gateway')
    expect(details.bodyText).toBe('Backend failure')
    expect(details.statusMessage).toBe('Backend failure')
    expect(details.logMessage).toBe('HTTP 502 - Bad Gateway - Backend failure')
    expect(details.isResponseError).toBe(true)
    // Logging contract: the sanitized payload only exposes serialisable primitives
    expect(Object.keys(details).sort()).toEqual([
      'bodyText',
      'isResponseError',
      'logMessage',
      'statusCode',
      'statusMessage',
      'statusText',
    ])
    expect(JSON.parse(JSON.stringify(details))).toMatchObject({
      statusCode: 502,
      statusText: 'Bad Gateway',
      statusMessage: 'Backend failure',
      bodyText: 'Backend failure',
      isResponseError: true,
      logMessage: 'HTTP 502 - Bad Gateway - Backend failure',
    })
  })

  it('provides a safe fallback for non ResponseError values', async () => {
    const error = new Error('boom')

    const details = await extractBackendErrorDetails(error)

    expect(details.statusCode).toBe(500)
    expect(details.statusText).toBe('Internal Server Error')
    expect(details.bodyText).toBeUndefined()
    expect(details.statusMessage).toBe('boom')
    expect(details.logMessage).toBe('HTTP 500 - Internal Server Error - boom')
    expect(details.isResponseError).toBe(false)
    // Logging contract: object can still be stringified for structured logging
    expect(Object.keys(details).sort()).toEqual([
      'bodyText',
      'isResponseError',
      'logMessage',
      'statusCode',
      'statusMessage',
      'statusText',
    ])
    expect(JSON.parse(JSON.stringify(details))).toMatchObject({
      statusCode: 500,
      statusText: 'Internal Server Error',
      statusMessage: 'boom',
      isResponseError: false,
      logMessage: 'HTTP 500 - Internal Server Error - boom',
    })
  })

  it('maps an unreachable backend (ECONNREFUSED) to 503 Service Unavailable', async () => {
    // Shape thrown by the generated client while front-api restarts.
    const connectError = Object.assign(
      new Error('connect ECONNREFUSED 127.0.0.1:8086'),
      { code: 'ECONNREFUSED' }
    )
    const error = new FetchError(
      new TypeError('fetch failed', { cause: connectError }),
      'The request failed and the interceptors did not return an alternative response'
    )

    const details = await extractBackendErrorDetails(error)

    expect(details.statusCode).toBe(503)
    expect(details.statusText).toBe('Service Unavailable')
    expect(details.statusMessage).toBe(
      'Backend unavailable (ECONNREFUSED): connect ECONNREFUSED 127.0.0.1:8086'
    )
    expect(details.isResponseError).toBe(false)
    expect(details.logMessage).toBe(
      'HTTP 503 - Service Unavailable - Backend unavailable (ECONNREFUSED): connect ECONNREFUSED 127.0.0.1:8086'
    )
  })

  it.each(['ECONNRESET', 'ENOTFOUND', 'ETIMEDOUT', 'UND_ERR_CONNECT_TIMEOUT'])(
    'maps the %s network error to 503',
    async code => {
      const error = new TypeError('fetch failed', {
        cause: Object.assign(new Error(code), { code }),
      })

      const details = await extractBackendErrorDetails(error)

      expect(details.statusCode).toBe(503)
    }
  )

  it('keeps 500 for an unexpected error that is not a network failure', async () => {
    const error = new FetchError(new TypeError('Cannot read properties'))

    const details = await extractBackendErrorDetails(error)

    expect(details.statusCode).toBe(500)
  })
})
