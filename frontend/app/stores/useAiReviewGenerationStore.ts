import { defineStore } from 'pinia'
import { useLocalStorage } from '@vueuse/core'
import { computed, reactive } from 'vue'

export type GenerationStatus = 'pending' | 'generating' | 'success' | 'failed'

interface QueuedMessage {
  message: string
  percent: number
  timestamp: number
}

export interface GenerationQueueItem {
  gtin: string
  productName: string
  productImage?: string
  productSlug?: string
  status: GenerationStatus
  statusMessage?: string
  percent: number
  startedAt: number
  completedAt?: number
  error?: string
  seen?: boolean // If true, the completion alert has been shown/dismissed
}

const STORAGE_KEY = 'open4goods:ai-generation-queue'
const COLLAPSE_STORAGE_KEY = `${STORAGE_KEY}:collapsed`

// Minimum delay between message transitions (3 seconds + random 0-2 seconds)
const MIN_MESSAGE_DELAY_MS = 3000
const RANDOM_DELAY_MAX_MS = 2000

export const useAiReviewGenerationStore = defineStore(
  'ai-review-generation',
  () => {
    const items = useLocalStorage<GenerationQueueItem[]>(STORAGE_KEY, [], {
      deep: true,
    })
    const isCollapsed = useLocalStorage<boolean>(COLLAPSE_STORAGE_KEY, false)

    // Message queues per GTIN - not persisted to localStorage, just in-memory
    const messageQueues = reactive<Map<string, QueuedMessage[]>>(new Map())
    const displayedMessages = reactive<
      Map<string, { message: string; percent: number }>
    >(new Map())
    const messageTimers = new Map<string, ReturnType<typeof setTimeout>>()
    const lastMessageTime = reactive<Map<string, number>>(new Map())

    const pendingCount = computed(
      () =>
        items.value.filter(
          item => item.status === 'pending' || item.status === 'generating'
        ).length
    )

    const hasItems = computed(() => items.value.length > 0)

    const getByGtin = (gtin: string) => {
      return items.value.find(item => item.gtin === gtin) ?? null
    }

    // Get displayed message for a GTIN (used by components)
    const getDisplayedMessage = (gtin: string) => {
      return displayedMessages.get(gtin) ?? null
    }

    const hasItem = (gtin: string) => {
      return Boolean(getByGtin(gtin))
    }

    const activePolls = new Map<string, number>()

    // Normalize/merge similar messages
    const normalizeMessage = (message: string): string => {
      // Trim and lowercase for comparison
      return message.trim().toLowerCase()
    }

    const shouldMergeMessages = (msg1: string, msg2: string): boolean => {
      const norm1 = normalizeMessage(msg1)
      const norm2 = normalizeMessage(msg2)

      // Exact match
      if (norm1 === norm2) return true

      // Check if one contains the other (common for status updates like "running" -> "running...")
      if (norm1.includes(norm2) || norm2.includes(norm1)) return true

      return false
    }

    const enqueueMessage = (gtin: string, message: string, percent: number) => {
      if (!messageQueues.has(gtin)) {
        messageQueues.set(gtin, [])
      }

      const queue = messageQueues.get(gtin)!

      // Check if we should merge with the last queued message
      if (queue.length > 0) {
        const lastQueued = queue[queue.length - 1]
        if (shouldMergeMessages(lastQueued.message, message)) {
          // Update the existing message with higher percent if applicable
          lastQueued.message = message
          lastQueued.percent = Math.max(lastQueued.percent, percent)
          return
        }
      }

      // Also check against currently displayed message
      const displayed = displayedMessages.get(gtin)
      if (displayed && shouldMergeMessages(displayed.message, message)) {
        // Just update percent if it's higher
        if (percent > displayed.percent) {
          displayed.percent = percent
        }
        return
      }

      // Add to queue
      queue.push({
        message,
        percent,
        timestamp: Date.now(),
      })

      // Start processing if not already running
      processMessageQueue(gtin)
    }

    const processMessageQueue = (gtin: string) => {
      // Don't start another timer if one is already running
      if (messageTimers.has(gtin)) return

      const queue = messageQueues.get(gtin)
      if (!queue || queue.length === 0) return

      const now = Date.now()
      const lastTime = lastMessageTime.get(gtin) ?? 0
      const elapsed = now - lastTime

      // Calculate delay - minimum 3 seconds + random 0-2 seconds
      const randomDelay = Math.random() * RANDOM_DELAY_MAX_MS
      const totalDelay = MIN_MESSAGE_DELAY_MS + randomDelay
      const remainingDelay = Math.max(0, totalDelay - elapsed)

      const timer = setTimeout(() => {
        messageTimers.delete(gtin)

        const currentQueue = messageQueues.get(gtin)
        if (!currentQueue || currentQueue.length === 0) return

        // Pop the first message
        const nextMessage = currentQueue.shift()!

        // Update displayed message
        displayedMessages.set(gtin, {
          message: nextMessage.message,
          percent: nextMessage.percent,
        })
        lastMessageTime.set(gtin, Date.now())

        // Also update the store item for persistence
        const item = getByGtin(gtin)
        if (item) {
          item.statusMessage = nextMessage.message
          item.percent = nextMessage.percent
        }

        // Process next message if any
        if (currentQueue.length > 0) {
          processMessageQueue(gtin)
        }
      }, remainingDelay)

      messageTimers.set(gtin, timer)
    }

    const clearMessageQueue = (gtin: string) => {
      const timer = messageTimers.get(gtin)
      if (timer) {
        clearTimeout(timer)
        messageTimers.delete(gtin)
      }
      messageQueues.delete(gtin)
      displayedMessages.delete(gtin)
      lastMessageTime.delete(gtin)
    }

    const setPoll = (gtin: string, handle: number) => {
      stopPoll(gtin)
      activePolls.set(gtin, handle)
    }

    const stopPoll = (gtin: string) => {
      const handle = activePolls.get(gtin)
      if (handle) {
        clearInterval(handle)
        activePolls.delete(gtin)
      }
    }

    const startGeneration = async (
      product: {
        gtin: string
        name: string
        image?: string
        slug?: string
      },
      captchaToken?: string,
      force: boolean = false
    ) => {
      const { gtin } = product
      const existing = getByGtin(gtin)

      // Reset or Create item
      // Clear any existing message queue
      clearMessageQueue(gtin)

      if (existing) {
        existing.status = 'pending'
        existing.percent = 0
        existing.startedAt = Date.now()
        existing.completedAt = undefined
        existing.error = undefined
        existing.statusMessage = undefined
        existing.seen = false
      } else {
        items.value.push({
          gtin,
          productName: product.name,
          productImage: product.image,
          productSlug: product.slug,
          status: 'pending',
          percent: 0,
          startedAt: Date.now(),
          seen: false,
        })
      }

      isCollapsed.value = false

      // Trigger API
      try {
        await $fetch(`/api/products/${gtin}/review`, {
          method: 'POST',
          body: {
            hcaptchaResponse: captchaToken,
          },
          query: {
            force: String(force),
          },
        })

        updateStatus(gtin, {
          status: 'generating',
          percent: 5,
          message: 'Démarrage...',
        })
        startPolling(gtin)
      } catch (error) {
        console.error('Failed to trigger generation', error)
        completeGeneration(gtin, {
          success: false,
          error: error instanceof Error ? error.message : 'Error',
        })
      }
    }

    const startPolling = (gtin: string) => {
      stopPoll(gtin)

      // Immediate check
      void checkStatus(gtin)

      if (import.meta.client) {
        const handle = window.setInterval(() => {
          void checkStatus(gtin)
        }, 3000)
        setPoll(gtin, handle)
      }
    }

    interface ReviewGenerationResponse {
      status: 'CREATED' | 'RUNNING' | 'SUCCESS' | 'FAILED' | string
      errorMessage?: string
      percent?: number
    }

    const checkStatus = async (gtin: string) => {
      try {
        const response = await $fetch<ReviewGenerationResponse>(
          `/api/products/${gtin}/review`
        )

        if (!response || !response.status) {
          // Keep waiting or fail? retry?
          return
        }

        if (response.status === 'FAILED') {
          stopPoll(gtin)
          completeGeneration(gtin, {
            success: false,
            error: response.errorMessage ?? 'Failed',
          })
          return
        }

        if (response.status === 'SUCCESS') {
          stopPoll(gtin)
          completeGeneration(gtin, { success: true })
          return
        }

        // Generating
        let percent = response.percent ?? 0
        // Artificial progress if needed, or mapping statuses to percent
        if (percent === 0 && response.status === 'CREATED') percent = 10
        if (percent === 0 && response.status === 'RUNNING') percent = 20

        updateStatus(gtin, {
          status: 'generating',
          percent,
          message: response.status?.toLowerCase(), // TODO: Translate or map this?
        })
      } catch (error) {
        // network blink? don't fail immediately, maybe count errors
        console.error('Poll error', error)
      }
    }

    const updateStatus = (
      gtin: string,
      statusData: {
        status?: GenerationStatus
        percent?: number
        message?: string
      }
    ) => {
      const item = getByGtin(gtin)
      if (!item) return

      if (statusData.status) item.status = statusData.status

      // Use message queue for smooth transitions with delay
      if (statusData.message && typeof statusData.percent === 'number') {
        enqueueMessage(gtin, statusData.message, statusData.percent)
      } else {
        // Fallback for cases with only percent or only message
        if (typeof statusData.percent === 'number') {
          item.percent = statusData.percent
        }
        if (statusData.message) {
          item.statusMessage = statusData.message
        }
      }
    }

    const completeGeneration = (
      gtin: string,
      result: { success: boolean; error?: string }
    ) => {
      const item = getByGtin(gtin)
      if (!item) return

      // Clear message queue on completion
      clearMessageQueue(gtin)

      item.status = result.success ? 'success' : 'failed'
      item.percent = 100
      item.completedAt = Date.now()
      if (result.error) {
        item.error = result.error
      }
      stopPoll(gtin)
    }

    // Resume polling for any pending items on init
    const resumePending = () => {
      if (!import.meta.client) return

      items.value.forEach(item => {
        if (item.status === 'pending' || item.status === 'generating') {
          // We don't have the token to re-trigger, but if it's already running on server, we just poll
          startPolling(item.gtin)
        }
      })
    }

    const removeItem = (gtin: string) => {
      stopPoll(gtin)
      clearMessageQueue(gtin)
      items.value = items.value.filter(item => item.gtin !== gtin)
    }

    const acknowledgeCompletion = (gtin: string) => {
      const item = getByGtin(gtin)
      if (item) {
        item.seen = true
      }
    }

    const clear = () => {
      items.value.forEach(item => {
        stopPoll(item.gtin)
        clearMessageQueue(item.gtin)
      })
      items.value = []
    }

    return {
      items,
      isCollapsed,
      pendingCount,
      hasItems,
      getByGtin,
      getDisplayedMessage,
      hasItem,
      startGeneration,
      resumePending,
      removeItem,
      acknowledgeCompletion,
      clear,
      // exposed for testing if needed, though usually internal
      activePolls,
      completeGeneration,
      updateStatus,
    }
  }
)
