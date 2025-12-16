import { computed } from 'vue'

export function usePwaCapabilities() {
  const supportsNotifications = useState(
    'pwa-notifications-supported',
    () => false
  )
  const notificationsPermission = useState<NotificationPermission>(
    'pwa-notifications-permission',
    () => 'default'
  )
  const serviceWorkerRegistration = useState<ServiceWorkerRegistration | null>(
    'pwa-service-worker-registration',
    () => null
  )

  const requestNotificationPermission = async () => {
    if (
      !import.meta.client ||
      !supportsNotifications.value ||
      typeof Notification === 'undefined'
    ) {
      notificationsPermission.value = 'denied'
      return notificationsPermission.value
    }

    const permission = await Notification.requestPermission()
    notificationsPermission.value = permission
    return permission
  }

  return {
    supportsNotifications: computed(() => supportsNotifications.value),
    notificationPermission: computed(() => notificationsPermission.value),
    serviceWorkerRegistration: computed(() => serviceWorkerRegistration.value),
    requestNotificationPermission,
  }
}
