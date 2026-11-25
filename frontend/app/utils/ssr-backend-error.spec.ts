import { describe, expect, it, vi } from 'vitest'

import {
  BACKEND_UNAVAILABLE_RETRY_AFTER_SECONDS,
  createBackendUnavailableError,
} from './ssr-backend-error'

const setResponseHeaderMock = vi.hoisted(() => vi.fn())
vi.mock('h3', async importOriginal => ({
  ...(await importOriginal<typeof import('h3')>()),
  setResponseHeader: setResponseHeaderMock,
}))

describe('createBackendUnavailableError', () => {
  it('answers 503 with a Retry-After header and no noindex', () => {
    const event = {} as never
    const error = createBackendUnavailableError(new Error('boom'), event)

    expect(error.statusCode).toBe(503)
    expect(setResponseHeaderMock).toHaveBeenCalledWith(
      event,
      'Retry-After',
      String(BACKEND_UNAVAILABLE_RETRY_AFTER_SECONDS)
    )
  })
})
