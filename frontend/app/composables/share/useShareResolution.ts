import { computed, onBeforeUnmount, ref } from 'vue'
import { useIntervalFn } from '@vueuse/core'
import { $fetch } from 'ofetch'
import type {
  ShareResolutionRequestDto,
  ShareResolutionResponseDto,
  ShareResolutionStatus,
} from '~~/shared/api-client'
import type { DomainLanguage } from '~~/shared/utils/domain-language'

const DEFAULT_POLL_INTERVAL_MS = 700
const DEFAULT_MAX_DURATION_MS = 4000

type ShareResolutionPayload = ShareResolutionRequestDto & {
  domainLanguage?: DomainLanguage
}

type ShareResolutionFetcher = <T>(
  url: string,
  options?: Parameters<typeof $fetch<T>>[1]
) => Promise<T>

type UseShareResolutionOptions = {
  pollIntervalMs?: number
  maxDurationMs?: number
  fetcher?: ShareResolutionFetcher
}

export const useShareResolution = (options: UseShareResolutionOptions = {}) => {
  const resolution = ref<ShareResolutionResponseDto | null>(null)
  const errorMessage = ref<string | null>(null)
  const isLoading = ref(false)
  const elapsedMs = ref(0)
  const token = ref<string | null>(null)
  let lastPayload: ShareResolutionPayload | null = null
  let startedAt: number | null = null

  const fetcher = options.fetcher ?? $fetch
  const pollIntervalMs = options.pollIntervalMs ?? DEFAULT_POLL_INTERVAL_MS
  const maxDurationMs = options.maxDurationMs ?? DEFAULT_MAX_DURATION_MS

  const {
    pause: pausePolling,
    resume: resumePolling,
    isActive,
  } = useIntervalFn(
    async () => {
      if (!token.value || !lastPayload) {
        pausePolling()
        return
      }

      if (startedAt) {
        elapsedMs.value = Date.now() - startedAt
      }

      if (elapsedMs.value >= maxDurationMs) {
        resolution.value = resolution.value
          ? {
              ...resolution.value,
              status: 'TIMEOUT',
              resolvedAt: new Date().toISOString(),
            }
          : null
        pausePolling()
        return
      }

      const response = await fetcher<ShareResolutionResponseDto>(
        `/api/share/resolutions/${encodeURIComponent(token.value)}`,
        {
          method: 'GET',
          query: { domainLanguage: lastPayload.domainLanguage },
        }
      )

      resolution.value = response

      if (response.status !== 'PENDING') {
        pausePolling()
      }
    },
    pollIntervalMs,
    { immediate: false }
  )

  const reset = () => {
    pausePolling()
    resolution.value = null
    errorMessage.value = null
    isLoading.value = false
    elapsedMs.value = 0
    token.value = null
    lastPayload = null
    startedAt = null
  }

  const startResolution = async (payload: ShareResolutionPayload) => {
    reset()
    lastPayload = payload
    isLoading.value = true

    try {
      const response = await fetcher<ShareResolutionResponseDto>(
        '/api/share/resolutions',
        {
          method: 'POST',
          body: payload,
        }
      )

      resolution.value = response
      token.value = response.token
      startedAt = Date.now()
      elapsedMs.value = 0

      if (response.status === 'PENDING') {
        resumePolling()
      }
    } catch (error) {
      errorMessage.value =
        error instanceof Error ? error.message : String(error)
      pausePolling()
    } finally {
      isLoading.value = false
    }
  }

  const status = computed<ShareResolutionStatus | null>(
    () => resolution.value?.status ?? null
  )
  const isPending = computed(() => status.value === 'PENDING')
  const isResolved = computed(() => status.value === 'RESOLVED')
  const isTimeout = computed(() => status.value === 'TIMEOUT')
  const isErrored = computed(() => status.value === 'ERROR')

  onBeforeUnmount(() => pausePolling())

  return {
    resolution,
    status,
    isPending,
    isResolved,
    isTimeout,
    isErrored,
    isLoading,
    elapsedMs,
    errorMessage,
    startResolution,
    reset,
    isPolling: isActive,
  }
}
