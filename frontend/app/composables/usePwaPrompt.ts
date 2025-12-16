import { useRegisterSW } from 'virtual:pwa-register/vue'

type InstallOutcome = 'accepted' | 'dismissed' | null

interface BeforeInstallPromptEvent extends Event {
  readonly platforms: string[]
  readonly userChoice: Promise<{
    outcome: Exclude<InstallOutcome, null>
    platform: string
  }>
  prompt(): Promise<void>
}

type RegisterSWState = {
  needRefresh: Ref<boolean>
  offlineReady: Ref<boolean>
  updateServiceWorker: (reloadPage?: boolean) => Promise<void>
}

const INSTALL_DISMISS_COOKIE = 'nudger-pwa-install-dismissed'

export function usePwaPrompt() {
  const deferredPrompt = useState<BeforeInstallPromptEvent | null>(
    'pwa-install-deferred',
    () => null
  )
  const installOutcome = useState<InstallOutcome>(
    'pwa-install-outcome',
    () => null
  )
  const installError = useState<string | null>('pwa-install-error', () => null)
  const installInProgress = useState('pwa-installing', () => false)
  const offlineReady = useState('pwa-offline-ready', () => false)
  const updateAvailable = useState('pwa-update-available', () => false)
  const installCookieDefaultsAllowed =
    import.meta.client && typeof window !== 'undefined'

  const installDismissed = useCookie<boolean | undefined>(
    INSTALL_DISMISS_COOKIE,
    {
      maxAge: 60 * 60 * 24 * 30, // 30 days
      sameSite: 'lax',
      ...(installCookieDefaultsAllowed ? { default: () => false } : {}),
    }
  )

  const swState: RegisterSWState = import.meta.client
    ? useRegisterSW({
        immediate: true,
        onOfflineReady() {
          offlineReady.value = true
        },
        onNeedRefresh() {
          updateAvailable.value = true
        },
      })
    : {
        needRefresh: ref(false),
        offlineReady: ref(false),
        updateServiceWorker: async () => {},
      }

  const installPromptVisible = computed(
    () => Boolean(deferredPrompt.value) && !installDismissed.value
  )
  const isInstallSupported = computed(() => Boolean(deferredPrompt.value))

  const dismissInstall = () => {
    installDismissed.value = true
    deferredPrompt.value = null
  }

  const requestInstall = async () => {
    const promptEvent = deferredPrompt.value
    if (!promptEvent) {
      return
    }

    installInProgress.value = true
    installError.value = null

    try {
      await promptEvent.prompt()
      const choice = await promptEvent.userChoice
      installOutcome.value = choice.outcome
      installDismissed.value = true
      deferredPrompt.value = null
    } catch (error) {
      installError.value =
        error instanceof Error ? error.message : 'unknown_error'
    } finally {
      installInProgress.value = false
    }
  }

  const applyUpdate = async () => {
    updateAvailable.value = false
    await swState.updateServiceWorker(true)
  }

  if (import.meta.client) {
    useEventListener(window, 'beforeinstallprompt', event => {
      event.preventDefault()
      deferredPrompt.value = event as BeforeInstallPromptEvent
      installOutcome.value = null
      installError.value = null
      installDismissed.value = false
    })

    useEventListener(window, 'appinstalled', () => {
      installOutcome.value = 'accepted'
      installDismissed.value = true
      deferredPrompt.value = null
    })
  }

  return {
    installPromptVisible,
    isInstallSupported,
    installOutcome,
    installError,
    installInProgress,
    offlineReady: computed(
      () => offlineReady.value || swState.offlineReady.value
    ),
    updateAvailable: computed(
      () => updateAvailable.value || swState.needRefresh.value
    ),
    dismissInstall,
    requestInstall,
    applyUpdate,
  }
}
