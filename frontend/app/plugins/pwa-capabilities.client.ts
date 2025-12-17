export default defineNuxtPlugin(() => {
  if (!import.meta.client) {
    return
  }

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

  const hasNotificationSupport =
    typeof window !== 'undefined' &&
    'Notification' in window &&
    'serviceWorker' in navigator

  supportsNotifications.value = hasNotificationSupport
  notificationsPermission.value =
    hasNotificationSupport && typeof Notification !== 'undefined'
      ? Notification.permission
      : 'denied'

  if ('serviceWorker' in navigator) {
    navigator.serviceWorker.ready
      .then(registration => {
        serviceWorkerRegistration.value = registration
      })
      .catch(error => {
        if (import.meta.dev) {
          console.warn('Failed to resolve service worker registration', error)
        }
      })
  }
})
