import { defineStore } from 'pinia'
import { useLocalStorage } from '@vueuse/core'
import { computed } from 'vue'

export type GenerationStatus = 'pending' | 'generating' | 'success' | 'failed'

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

export const useAiReviewGenerationStore = defineStore(
  'ai-review-generation',
  () => {
    const items = useLocalStorage<GenerationQueueItem[]>(STORAGE_KEY, [], {
      deep: true,
    })
    const isCollapsed = useLocalStorage<boolean>(COLLAPSE_STORAGE_KEY, false)

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

    const hasItem = (gtin: string) => {
      return Boolean(getByGtin(gtin))
    }

    const activePolls = new Map<string, number>()

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
      captchaToken?: string
    ) => {
      const { gtin } = product
      const existing = getByGtin(gtin)

      // Reset or Create item
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
      if (typeof statusData.percent === 'number')
        item.percent = statusData.percent
      if (statusData.message) item.statusMessage = statusData.message
    }

    const completeGeneration = (
      gtin: string,
      result: { success: boolean; error?: string }
    ) => {
      const item = getByGtin(gtin)
      if (!item) return

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
      items.value = items.value.filter(item => item.gtin !== gtin)
    }

    const acknowledgeCompletion = (gtin: string) => {
      const item = getByGtin(gtin)
      if (item) {
        item.seen = true
      }
    }

    const clear = () => {
      items.value.forEach(item => stopPoll(item.gtin))
      items.value = []
    }

    return {
      items,
      isCollapsed,
      pendingCount,
      hasItems,
      getByGtin,
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
