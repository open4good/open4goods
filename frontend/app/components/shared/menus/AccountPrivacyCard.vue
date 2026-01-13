<template>
  <v-card
    class="account-privacy-card"
    color="surface-default"
    elevation="4"
    data-testid="account-privacy-card"
  >
    <div class="account-privacy-card__header">
      <v-avatar
        size="44"
        color="surface-primary-080"
        class="account-privacy-card__avatar"
      >
        <v-icon icon="mdi-navigation-private" color="primary" />
      </v-avatar>
      <div>
        <p class="account-privacy-card__title">
          {{ t('siteIdentity.menu.account.privacy.title') }}
        </p>
        <p class="account-privacy-card__subtitle">
          {{ t('siteIdentity.menu.account.privacy.subtitle') }}
        </p>
      </div>
    </div>

    <v-divider class="my-4" />

    <div class="account-privacy-card__section">
      <div class="account-privacy-card__section-header">
        <div>
          <p class="account-privacy-card__section-title">
            {{ t('siteIdentity.menu.account.privacy.ip.title') }}
          </p>
          <p class="account-privacy-card__section-description">
            {{ t('siteIdentity.menu.account.privacy.ip.description') }}
          </p>
        </div>
        <v-chip
          size="small"
          color="surface-primary-100"
          variant="flat"
          class="account-privacy-card__chip"
        >
          {{ ipLabel }}
        </v-chip>
      </div>
    </div>

    <div class="account-privacy-card__section">
      <div class="account-privacy-card__section-header">
        <div>
          <p class="account-privacy-card__section-title">
            {{ t('siteIdentity.menu.account.privacy.cookies.title') }}
          </p>
          <p class="account-privacy-card__section-description">
            {{
              t('siteIdentity.menu.account.privacy.cookies.description', {
                count: cookieKeys.length,
              })
            }}
          </p>
        </div>
        <v-btn
          color="primary"
          variant="tonal"
          size="small"
          class="account-privacy-card__action"
          data-testid="privacy-reset-cookies"
          :disabled="cookieKeys.length === 0"
          @click="handleClearCookies"
        >
          {{ t('siteIdentity.menu.account.privacy.cookies.resetCta') }}
        </v-btn>
      </div>

      <div class="account-privacy-card__chips">
        <template v-if="cookieKeys.length">
          <v-chip
            v-for="cookieKey in cookieKeys"
            :key="cookieKey"
            size="x-small"
            variant="outlined"
            class="account-privacy-card__chip"
          >
            {{ cookieKey }}
          </v-chip>
        </template>
        <p v-else class="account-privacy-card__empty">
          {{ t('siteIdentity.menu.account.privacy.cookies.empty') }}
        </p>
      </div>
    </div>

    <div class="account-privacy-card__section">
      <div class="account-privacy-card__section-header">
        <div>
          <p class="account-privacy-card__section-title">
            {{ t('siteIdentity.menu.account.privacy.storage.title') }}
          </p>
          <p class="account-privacy-card__section-description">
            {{
              t('siteIdentity.menu.account.privacy.storage.description', {
                count: localStorageKeys.length,
              })
            }}
          </p>
        </div>
        <v-btn
          color="primary"
          variant="tonal"
          size="small"
          class="account-privacy-card__action"
          data-testid="privacy-reset-local-storage"
          :disabled="localStorageKeys.length === 0"
          @click="handleClearLocalStorage"
        >
          {{ t('siteIdentity.menu.account.privacy.storage.resetCta') }}
        </v-btn>
      </div>

      <div class="account-privacy-card__chips">
        <template v-if="localStorageKeys.length">
          <v-chip
            v-for="storageKey in localStorageKeys"
            :key="storageKey"
            size="x-small"
            variant="outlined"
            class="account-privacy-card__chip"
          >
            {{ storageKey }}
          </v-chip>
        </template>
        <p v-else class="account-privacy-card__empty">
          {{ t('siteIdentity.menu.account.privacy.storage.empty') }}
        </p>
      </div>
    </div>
  </v-card>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
const requestHeaders = useRequestHeaders(['x-forwarded-for', 'x-real-ip'])

const cookieKeys = ref<string[]>([])
const localStorageKeys = ref<string[]>([])

const resolveIpAddress = () => {
  const forwardedHeader = requestHeaders['x-forwarded-for']
  const forwardedIp = forwardedHeader?.split(',')[0]?.trim()
  return forwardedIp || requestHeaders['x-real-ip']?.trim() || null
}

const ipLabel = computed(() => {
  const resolvedIp = resolveIpAddress()
  return resolvedIp ?? t('siteIdentity.menu.account.privacy.ip.unavailable')
})

const refreshStorageSnapshot = () => {
  if (typeof window === 'undefined') {
    cookieKeys.value = []
    localStorageKeys.value = []
    return
  }

  const cookies = document.cookie
    .split(';')
    .map(cookie => cookie.trim())
    .filter(cookie => cookie.length > 0)
    .map(cookie => cookie.split('=')[0]?.trim() ?? '')
    .filter(cookie => cookie.length > 0)

  cookieKeys.value = Array.from(new Set(cookies))
  localStorageKeys.value = Object.keys(window.localStorage)
}

const clearCookieKey = (key: string) => {
  document.cookie = `${key}=; Max-Age=0; path=/`
  document.cookie = `${key}=; Max-Age=0; path=/; domain=${location.hostname}`
  document.cookie = `${key}=; Max-Age=0; path=/; domain=.${location.hostname}`
}

const handleClearCookies = () => {
  if (typeof window === 'undefined') {
    return
  }

  cookieKeys.value.forEach(clearCookieKey)
  refreshStorageSnapshot()
}

const handleClearLocalStorage = () => {
  if (typeof window === 'undefined') {
    return
  }

  window.localStorage.clear()
  refreshStorageSnapshot()
}

onMounted(() => {
  refreshStorageSnapshot()
})
</script>

<style scoped lang="sass">
.account-privacy-card
  border-radius: 20px
  padding: 20px

  &__header
    display: flex
    align-items: center
    gap: 16px

  &__avatar
    flex-shrink: 0

  &__title
    font-size: 1rem
    font-weight: 700
    margin: 0
    color: rgb(var(--v-theme-text-neutral-strong))

  &__subtitle
    font-size: 0.875rem
    margin: 4px 0 0
    color: rgb(var(--v-theme-text-neutral-secondary))

  &__section
    display: flex
    flex-direction: column
    gap: 12px
    padding: 12px 0

  &__section-header
    display: flex
    align-items: flex-start
    justify-content: space-between
    gap: 16px
    flex-wrap: wrap

  &__section-title
    font-size: 0.95rem
    font-weight: 600
    margin: 0
    color: rgb(var(--v-theme-text-neutral-strong))

  &__section-description
    margin: 4px 0 0
    font-size: 0.875rem
    color: rgb(var(--v-theme-text-neutral-secondary))

  &__chips
    display: flex
    flex-wrap: wrap
    gap: 8px

  &__chip
    font-weight: 500

  &__action
    text-transform: none

  &__empty
    margin: 0
    font-size: 0.85rem
    color: rgb(var(--v-theme-text-neutral-soft))

  @media (max-width: 599px)
    padding: 16px
    border-radius: 16px

    &__header
      align-items: flex-start
</style>
