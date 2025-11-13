import { useI18n, useOnline, useState } from '#imports'

export const OFFLINE_DISMISSED_STATE_KEY = 'pwa-offline-dismissed'

export function usePwaOfflineNoticeBridge() {
  const { t } = useI18n()
  const isOnline = useOnline()
  const offlineDismissed = useState(OFFLINE_DISMISSED_STATE_KEY, () => false)

  return {
    t,
    isOnline,
    offlineDismissed,
  }
}
