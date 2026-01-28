import { setActivePinia, createPinia } from 'pinia'
import { describe, it, expect, beforeEach, vi } from 'vitest'
import { useAiReviewGenerationStore } from './useAiReviewGenerationStore'
import { ref } from 'vue'

// Mock useLocalStorage to behave like a normal ref for testing
vi.mock('@vueuse/core', () => ({
  useLocalStorage: (_key: string, initialValue: unknown) => ref(initialValue),
}))

vi.stubGlobal('$fetch', vi.fn())

describe('useAiReviewGenerationStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('starts with empty list', () => {
    const store = useAiReviewGenerationStore()
    expect(store.items).toEqual([])
    expect(store.hasItems).toBe(false)
  })

  it('adds an item when starting generation', () => {
    const store = useAiReviewGenerationStore()
    const product = { gtin: '123', name: 'Test Product', slug: 'test-product' }

    store.startGeneration(product)

    expect(store.items).toHaveLength(1)
    expect(store.items[0].gtin).toBe('123')
    expect(store.items[0].status).toBe('pending')
    expect(store.hasItem('123')).toBe(true)
  })

  it('updates existing item if restarted', () => {
    const store = useAiReviewGenerationStore()
    const product = { gtin: '123', name: 'Test Product' }

    store.startGeneration(product)
    store.completeGeneration('123', { success: true }) // Set to success
    expect(store.items[0].status).toBe('success')

    store.startGeneration(product) // Restart
    expect(store.items[0].status).toBe('pending')
    expect(store.items[0].percent).toBe(0)
    expect(store.items[0].seen).toBe(false)
  })

  it('updates generation status', async () => {
    vi.useFakeTimers()
    const store = useAiReviewGenerationStore()
    await store.startGeneration({ gtin: '123', name: 'Test' })

    store.updateStatus('123', {
      status: 'generating',
      percent: 50,
      message: 'Processing...',
    })

    // Advance timers twice to trigger BOTH the initial "Démarrage..." message
    // and the "Processing..." message. Each has a ~3-5s delay.
    await vi.advanceTimersByTimeAsync(10000)

    const item = store.getByGtin('123')
    expect(item?.status).toBe('generating')
    expect(item?.percent).toBe(50)
    expect(item?.statusMessage).toBe('Processing...')

    vi.useRealTimers()
  })

  it('completes generation successfully', () => {
    const store = useAiReviewGenerationStore()
    store.startGeneration({ gtin: '123', name: 'Test' })

    store.completeGeneration('123', { success: true })

    const item = store.getByGtin('123')
    expect(item?.status).toBe('success')
    expect(item?.percent).toBe(100)
    expect(item?.completedAt).toBeDefined()
    expect(item?.seen).toBe(false)
  })

  it('completes generation with failure', () => {
    const store = useAiReviewGenerationStore()
    store.startGeneration({ gtin: '123', name: 'Test' })

    store.completeGeneration('123', { success: false, error: 'Timeout' })

    const item = store.getByGtin('123')
    expect(item?.status).toBe('failed')
    expect(item?.error).toBe('Timeout')
  })

  it('removes an item', () => {
    const store = useAiReviewGenerationStore()
    store.startGeneration({ gtin: '123', name: 'Test' })
    store.startGeneration({ gtin: '456', name: 'Other' })

    store.removeItem('123')

    expect(store.items).toHaveLength(1)
    expect(store.items[0].gtin).toBe('456')
  })

  it('acknowledges completion', () => {
    const store = useAiReviewGenerationStore()
    store.startGeneration({ gtin: '123', name: 'Test' })
    store.completeGeneration('123', { success: true })

    expect(store.items[0].seen).toBe(false)

    store.acknowledgeCompletion('123')
    expect(store.items[0].seen).toBe(true)
  })
})
