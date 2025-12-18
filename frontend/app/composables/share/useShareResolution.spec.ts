import { describe, expect, it, vi, beforeEach, afterEach } from 'vitest'
import { nextTick } from 'vue'
import type { ShareResolutionResponseDto } from '~~/shared/api-client'
import { useShareResolution } from './useShareResolution'

describe('useShareResolution', () => {
  const pendingSnapshot: ShareResolutionResponseDto = {
    token: 'token-123',
    status: 'PENDING',
    originUrl: 'https://shop.example.com/p/1',
    startedAt: new Date().toISOString(),
    resolvedAt: null,
    extracted: null,
    candidates: [],
    message: null,
  }

  const resolvedSnapshot: ShareResolutionResponseDto = {
    ...pendingSnapshot,
    status: 'RESOLVED',
    resolvedAt: new Date().toISOString(),
    candidates: [
      {
        productId: '123',
        name: 'Test product',
        image: null,
        ecoScore: 3.5,
        impactScore: null,
        bestPrice: null,
        confidence: 0.9,
      },
    ],
  }

  beforeEach(() => {
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.clearAllMocks()
    vi.useRealTimers()
  })

  it('polls until the resolution is resolved', async () => {
    const fetcher = vi
      .fn()
      .mockResolvedValueOnce(pendingSnapshot)
      .mockResolvedValueOnce(resolvedSnapshot)

    const { status, resolution, startResolution } = useShareResolution({
      fetcher,
      pollIntervalMs: 10,
      maxDurationMs: 200,
    })

    await startResolution({ url: pendingSnapshot.originUrl })
    await nextTick()

    expect(status.value).toBe('PENDING')

    await vi.advanceTimersByTimeAsync(15)
    await nextTick()

    expect(status.value).toBe('RESOLVED')
    expect(resolution.value?.candidates.length).toBe(1)
  })

  it('forces a timeout when the SLA budget is exceeded', async () => {
    const fetcher = vi.fn().mockResolvedValue(pendingSnapshot)

    const { status, startResolution } = useShareResolution({
      fetcher,
      pollIntervalMs: 5,
      maxDurationMs: 20,
    })

    await startResolution({ url: pendingSnapshot.originUrl })
    await vi.advanceTimersByTimeAsync(25)
    await nextTick()

    expect(status.value).toBe('TIMEOUT')
  })
})
