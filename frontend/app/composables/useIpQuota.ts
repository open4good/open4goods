import { useStorage } from '@vueuse/core'
import type { IpQuotaStatusDto, IpQuotaCategory } from '~~/shared/api-client'

const STORAGE_KEY = 'nudger-ip-quota-v1'

/**
 * Local storage payload tracking the latest IP quota status per category.
 */
export interface IpQuotaStorageEntry {
  used: number
  remaining: number
  limit: number
  windowSeconds?: number
  lastSync: number
}

/**
 * Composable handling IP quota status per category.
 *
 * Storage:
 * - localStorage key: {@link STORAGE_KEY}
 * - value: JSON object keyed by category (e.g. FEEDBACK_VOTE) containing usage,
 *   limits, and last sync timestamp.
 * - no cookies are written by this composable.
 */
export const useIpQuota = () => {
  const quotaStorage = useStorage<Record<string, IpQuotaStorageEntry>>(
    STORAGE_KEY,
    {}
  )

  const getEntry = (category: IpQuotaCategory) =>
    quotaStorage.value[category] ?? null

  const setEntry = (category: IpQuotaCategory, entry: IpQuotaStorageEntry) => {
    quotaStorage.value = {
      ...quotaStorage.value,
      [category]: entry,
    }
  }

  const applyQuotaStatus = (
    category: IpQuotaCategory,
    status: IpQuotaStatusDto
  ) => {
    setEntry(category, {
      used: status.used,
      remaining: status.remaining,
      limit: status.limit,
      windowSeconds: status.windowSeconds,
      lastSync: Date.now(),
    })
  }

  const refreshQuota = async (category: IpQuotaCategory) => {
    if (!import.meta.client) {
      return null
    }

    const response = await $fetch<IpQuotaStatusDto>(`/api/quotas/${category}`, {
      headers: {
        'cache-control': 'no-cache',
      },
      query: {
        cacheBuster: String(Date.now()),
      },
    })

    applyQuotaStatus(category, response)
    return response
  }

  const recordUsage = (category: IpQuotaCategory, count = 1) => {
    const current = getEntry(category)
    if (!current) {
      setEntry(category, {
        used: count,
        remaining: Math.max(0, 0 - count),
        limit: 0,
        lastSync: Date.now(),
      })
      return
    }

    const nextUsed = current.used + count
    const nextRemaining =
      current.limit > 0
        ? Math.max(current.limit - nextUsed, 0)
        : Math.max(current.remaining - count, 0)

    setEntry(category, {
      ...current,
      used: nextUsed,
      remaining: nextRemaining,
      lastSync: Date.now(),
    })
  }

  const getUsed = (category: IpQuotaCategory) =>
    getEntry(category)?.used ?? null
  const getRemaining = (category: IpQuotaCategory) =>
    getEntry(category)?.remaining ?? null
  const getLimit = (category: IpQuotaCategory) =>
    getEntry(category)?.limit ?? null
  const getWindowSeconds = (category: IpQuotaCategory) =>
    getEntry(category)?.windowSeconds ?? null
  const getLastSync = (category: IpQuotaCategory) =>
    getEntry(category)?.lastSync ?? null

  return {
    refreshQuota,
    recordUsage,
    getUsed,
    getRemaining,
    getLimit,
    getWindowSeconds,
    getLastSync,
  }
}
