<template>
  <v-app class="error-app">
    <PageLoadingOverlay />
    <The-main-menu-container @toggle-drawer="toggleDrawer" />

    <ClientOnly>
      <template v-if="isMobileNavigation">
        <v-navigation-drawer
          v-model="drawer"
          location="start"
          :temporary="isMobileNavigation"
          :scrim="isMobileNavigation"
          :width="drawerWidth"
          floating
          class="mobile-menu-drawer"
          :style="drawerInlineStyles"
        >
          <the-mobile-menu @close="drawer = false" />
        </v-navigation-drawer>
      </template>
    </ClientOnly>

    <ClientOnly>
      <template #fallback>
        <div class="pre-hydration-app-bar-spacer" aria-hidden="true" />
      </template>
    </ClientOnly>

    <v-main>
      <section class="error-page" aria-labelledby="error-page-title">
        <v-container fluid class="error-page__container" tag="div">
          <div class="error-page__content">
            <p class="error-page__eyebrow">{{ eyebrow }}</p>
            <h1 id="error-page-title" class="error-page__title">
              {{ pageStrings.title }}
            </h1>
            <p class="error-page__description">
              {{ pageStrings.description }}
            </p>
            <p class="error-page__helper">
              {{ pageStrings.helper }}
            </p>

            <div
              class="error-page__actions"
              role="group"
              :aria-label="actionsAriaLabel"
            >
              <v-btn
                color="primary"
                class="error-page__action"
                size="large"
                prepend-icon="mdi-home"
                @click="handleGoHome"
              >
                {{ actionLabels.goHome }}
              </v-btn>
              <v-btn
                variant="text"
                color="primary"
                class="error-page__action"
                size="large"
                prepend-icon="mdi-lifebuoy"
                @click="handleContactSupport"
              >
                {{ actionLabels.contactSupport }}
              </v-btn>
            </div>
          </div>

          <aside
            class="error-page__aside"
            aria-labelledby="error-status-heading"
          >
            <div class="error-page__status-card">
              <p id="error-status-heading" class="error-page__status-eyebrow">
                {{ pageStrings.badge }}
              </p>
              <p class="error-page__status-code" aria-hidden="true">
                {{ paddedStatusCode }}
              </p>
              <p class="error-page__status-label">
                {{ statusLabel }}
              </p>
              <p class="error-page__status-message">
                {{ pageStrings.statusMessage }}
              </p>

              <section
                v-if="!is404 && (errorMessage || errorStack)"
                class="error-page__debug"
                aria-labelledby="error-debug-heading"
              >
                <h2 id="error-debug-heading" class="error-page__debug-title">
                  {{ debugStrings.title }}
                </h2>
                <p v-if="errorMessage" class="error-page__debug-message">
                  <span class="error-page__debug-label">{{
                    debugStrings.messageLabel
                  }}</span>
                  <span class="error-page__debug-value">{{
                    errorMessage
                  }}</span>
                </p>
                <div v-if="errorStack" class="error-page__debug-stack">
                  <p class="error-page__debug-label">
                    {{ debugStrings.stackLabel }}
                  </p>
                  <pre class="error-page__debug-stack-content">{{
                    errorStack
                  }}</pre>
                </div>
                <p v-else-if="errorMessage" class="error-page__debug-empty">
                  {{ debugStrings.empty }}
                </p>
              </section>
            </div>
          </aside>
        </v-container>
      </section>
    </v-main>

    <TheMainFooter>
      <template #footer>
        <TheMainFooterContent />
      </template>
    </TheMainFooter>
    <PwaOfflineNotice />
    <PwaInstallPrompt />
  </v-app>
</template>

<script setup lang="ts">
import type { NuxtError } from '#app'

interface ErrorPageStrings {
  badge: string
  title: string
  description: string
  helper: string
  statusMessage: string
}

interface ErrorDebugStrings {
  title: string
  messageLabel: string
  stackLabel: string
  empty: string
}

const props = defineProps<{
  error: NuxtError<Record<string, unknown>>
}>()

const { t } = useI18n()
const localePath = useLocalePath()

const drawer = useState('mobileDrawer', () => false)
const device = useDevice()
const display = useDisplay()
const routeLoading = useState('routeLoading', () => false)

routeLoading.value = false

const statusCode = computed(() => props.error?.statusCode ?? 500)
const paddedStatusCode = computed(() =>
  String(statusCode.value).padStart(3, '0')
)
const is404 = computed(() => statusCode.value === 404)

const pageStrings = computed<ErrorPageStrings>(() => {
  const fallbackStatusMessage = props.error?.statusMessage

  if (is404.value) {
    return {
      badge: String(t('error.page.notFound.badge')),
      title: String(t('error.page.notFound.title')),
      description: String(t('error.page.notFound.description')),
      helper: String(t('error.page.notFound.helper')),
      statusMessage: String(
        fallbackStatusMessage ?? t('error.page.notFound.statusMessage')
      ),
    }
  }

  return {
    badge: String(t('error.page.generic.badge')),
    title: String(t('error.page.generic.title')),
    description: String(t('error.page.generic.description')),
    helper: String(t('error.page.generic.helper')),
    statusMessage: String(
      fallbackStatusMessage ?? t('error.page.generic.statusMessage')
    ),
  }
})

const eyebrow = computed(() => String(t('error.page.eyebrow')))
const statusLabel = computed(() =>
  String(t('error.page.statusLabel', { code: paddedStatusCode.value }))
)
const actionsAriaLabel = computed(() =>
  String(t('error.page.actions.ariaLabel'))
)

const actionLabels = computed(() => ({
  goHome: String(t('error.page.actions.goHome')),
  contactSupport: String(t('error.page.actions.contactSupport')),
}))

const debugStrings = computed<ErrorDebugStrings>(() => ({
  title: String(t('error.page.debug.title')),
  messageLabel: String(t('error.page.debug.messageLabel')),
  stackLabel: String(t('error.page.debug.stackLabel')),
  empty: String(t('error.page.debug.empty')),
}))

const normaliseErrorDetail = (value: unknown): string => {
  if (typeof value === 'string') {
    return value.trim()
  }

  if (value instanceof Error) {
    return normaliseErrorDetail(value.stack ?? value.message)
  }

  if (value == null) {
    return ''
  }

  try {
    return JSON.stringify(value, null, 2)
  } catch {
    return String(value)
  }
}

const selectErrorDetail = (candidates: unknown[]): string => {
  for (const candidate of candidates) {
    const detail = normaliseErrorDetail(candidate)

    if (detail.length > 0) {
      return detail
    }
  }

  return ''
}

const errorMessage = computed(() =>
  selectErrorDetail([
    props.error?.message,
    props.error?.statusMessage,
    (props.error?.data as { message?: unknown } | undefined)?.message,
    (props.error?.cause as { message?: unknown } | undefined)?.message,
  ])
)

const errorStack = computed(() =>
  selectErrorDetail([
    props.error?.stack,
    (props.error?.cause as { stack?: unknown } | undefined)?.stack,
    (props.error?.data as { stack?: unknown } | undefined)?.stack,
  ])
)

useSeoMeta({
  title: () => pageStrings.value.title,
  ogTitle: () => pageStrings.value.title,
  description: () => pageStrings.value.description,
  ogDescription: () => pageStrings.value.description,
  robots: () => (is404.value ? 'noindex, follow' : 'noindex, nofollow'),
})

const toggleDrawer = () => {
  drawer.value = !drawer.value
}

const isMobileNavigation = computed(
  () => device.isMobileOrTablet || display.mdAndDown.value
)
const drawerWidth = computed(() => (isMobileNavigation.value ? 320 : 360))
const drawerInlineStyles = computed(() => ({
  paddingBottom: isMobileNavigation.value
    ? 'calc(env(safe-area-inset-bottom) + 24px)'
    : '24px',
}))

watch(
  () => isMobileNavigation.value,
  isMobileView => {
    if (!isMobileView) {
      drawer.value = false
    }
  },
  { immediate: true }
)

const handleGoHome = () => {
  clearError({ redirect: localePath('index') })
}
const handleContactSupport = () => {
  clearError({ redirect: localePath('contact') })
}
</script>

<style scoped lang="sass">
.error-app
  background: rgb(var(--v-theme-surface-default))

.pre-hydration-app-bar-spacer
  height: 64px

  @media (max-width: 959px)
    height: 56px

.error-page
  position: relative
  background: linear-gradient(135deg, rgba(var(--v-theme-hero-gradient-start), 0.1), rgba(var(--v-theme-hero-gradient-end), 0.16))

.error-page__container
  display: grid
  grid-template-columns: repeat(auto-fit, minmax(min(340px, 100%), 1fr))
  gap: clamp(2rem, 4vw, 3.5rem)
  padding-block: clamp(4rem, 12vh, 8rem)
  padding-inline: clamp(1.5rem, 6vw, 4rem)

.error-page__content
  align-self: center
  background: rgba(var(--v-theme-surface-glass), 0.88)
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.6)
  border-radius: 28px
  box-shadow: 0 32px 80px rgba(var(--v-theme-shadow-primary-600), 0.18)
  padding: clamp(2rem, 5vw, 3.25rem)
  backdrop-filter: blur(20px)

.error-page__eyebrow
  font-size: 0.875rem
  font-weight: 600
  letter-spacing: 0.08em
  text-transform: uppercase
  color: rgba(var(--v-theme-text-neutral-secondary), 0.9)
  margin-bottom: 0.75rem

.error-page__title
  font-size: clamp(2.25rem, 4vw, 3.25rem)
  line-height: 1.15
  font-weight: 600
  color: rgb(var(--v-theme-text-neutral-strong))
  margin-bottom: 1rem

.error-page__description
  font-size: 1.125rem
  color: rgba(var(--v-theme-text-neutral-secondary), 0.95)
  margin-bottom: 1.5rem

.error-page__helper
  font-size: 1rem
  color: rgba(var(--v-theme-text-neutral-soft), 0.95)
  margin-bottom: 2.5rem

.error-page__actions
  display: flex
  flex-wrap: wrap
  gap: 0.75rem

.error-page__action
  flex: 1 1 220px
  justify-content: flex-start

.error-page__aside
  align-self: stretch
  display: flex
  justify-content: center

.error-page__status-card
  background: rgba(var(--v-theme-surface-glass-strong), 0.92)
  border-radius: 28px
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.55)
  padding: clamp(2rem, 4.5vw, 3rem)
  width: min(420px, 100%)
  display: flex
  flex-direction: column
  gap: 1rem
  box-shadow: 0 26px 60px rgba(var(--v-theme-shadow-primary-600), 0.16)
  backdrop-filter: blur(16px)

.error-page__status-eyebrow
  font-size: 0.875rem
  font-weight: 600
  letter-spacing: 0.1em
  text-transform: uppercase
  color: rgba(var(--v-theme-text-neutral-secondary), 0.85)

.error-page__status-code
  font-size: clamp(4.5rem, 8vw, 6rem)
  font-weight: 700
  letter-spacing: -0.04em
  color: rgb(var(--v-theme-accent-primary-highlight))
  margin: -0.5rem 0 0.25rem

.error-page__status-label
  font-size: 1rem
  color: rgba(var(--v-theme-text-neutral-secondary), 0.9)

.error-page__status-message
  font-size: 1rem
  color: rgba(var(--v-theme-text-neutral-soft), 0.95)

.error-page__debug
  display: flex
  flex-direction: column
  gap: 0.75rem
  margin-top: 1.5rem
  padding-top: 1.5rem
  border-top: 1px solid rgba(var(--v-theme-border-primary-strong), 0.35)

.error-page__debug-title
  font-size: 1rem
  font-weight: 600
  color: rgb(var(--v-theme-text-neutral-strong))

.error-page__debug-label
  display: block
  font-size: 0.875rem
  font-weight: 600
  color: rgba(var(--v-theme-text-neutral-secondary), 0.95)
  margin-bottom: 0.35rem

.error-page__debug-message
  margin: 0

.error-page__debug-value
  display: block
  font-size: 0.95rem
  line-height: 1.5
  color: rgba(var(--v-theme-text-neutral-secondary), 0.95)

.error-page__debug-stack-content
  font-family: var(--font-mono, 'Roboto Mono', ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', 'Courier', monospace)
  font-size: 0.8rem
  line-height: 1.45
  max-height: 220px
  overflow: auto
  padding: 0.75rem
  border-radius: 12px
  background: rgba(var(--v-theme-surface-primary-080), 0.9)
  color: rgba(var(--v-theme-text-neutral-soft), 1)

.error-page__debug-empty
  font-size: 0.9rem
  color: rgba(var(--v-theme-text-neutral-soft), 0.95)

@media (max-width: 959px)
  .error-page__content, .error-page__status-card
    border-radius: 24px
    padding: clamp(1.75rem, 6vw, 2.25rem)

  .error-page__actions
    flex-direction: column

  .error-page__action
    justify-content: center

.mobile-menu-drawer
  padding-top: calc(env(safe-area-inset-top) + 8px)
</style>
