import { useI18n } from '#imports'
import { usePwaPrompt } from '../usePwaPrompt'

export function usePwaInstallPromptBridge() {
  const { t } = useI18n()
  const promptState = usePwaPrompt()

  return {
    t,
    ...promptState,
  }
}
