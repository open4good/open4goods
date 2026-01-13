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
        <v-icon icon="mdi-shield-account" color="primary" />
      </v-avatar>
      <div>
        <p class="account-privacy-card__title">
          {{ t('siteIdentity.menu.account.privacy.cardTitle') }}
        </p>
        <p class="account-privacy-card__subtitle">
          {{ t('siteIdentity.menu.account.privacy.subtitle') }}
        </p>
      </div>
    </div>

    <v-divider class="my-4" />

    <!-- IP Address -->
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
          class="account-privacy-card__chip font-weight-bold"
        >
          {{ ipLabel }}
        </v-chip>
      </div>
    </div>

    <!-- Quotas -->
    <div class="account-privacy-card__section">
      <p class="account-privacy-card__section-title mb-2">
        {{ t('siteIdentity.menu.account.privacy.quotas.title') }}
      </p>
      <v-list density="compact" nav bg-color="transparent" class="pa-0">
        <v-list-item v-if="aiQuota !== null" class="px-0">
          <template #prepend>
            <v-icon icon="mdi-robot-outline" size="small" class="mr-2" />
          </template>
          <div class="d-flex justify-space-between w-100 align-center">
            <span class="text-body-2">{{
              t('siteIdentity.menu.account.privacy.quotas.aiRemaining')
            }}</span>
            <v-chip size="x-small" color="primary" variant="flat">{{
              aiQuota
            }}</v-chip>
          </div>
        </v-list-item>
        <v-list-item
          v-if="voteQuota !== null"
          to="/feedback"
          class="px-0"
          color="primary"
        >
          <template #prepend>
            <v-icon icon="mdi-vote-outline" size="small" class="mr-2" />
          </template>
          <div class="d-flex justify-space-between w-100 align-center">
            <span class="text-body-2">{{
              t('siteIdentity.menu.account.privacy.quotas.votesRemaining')
            }}</span>
            <v-chip size="x-small" color="primary" variant="flat">{{
              voteQuota
            }}</v-chip>
          </div>
          <template #append>
            <v-icon icon="mdi-chevron-right" size="small" />
          </template>
        </v-list-item>
      </v-list>
    </div>

    <!-- Cookies -->
    <div class="account-privacy-card__section">
      <div class="d-flex justify-space-between align-center mb-2">
        <p class="account-privacy-card__section-title">
          {{ t('siteIdentity.menu.account.privacy.cookies.title') }}
        </p>
        <v-btn
          color="primary"
          variant="text"
          size="x-small"
          data-testid="privacy-reset-cookies"
          :disabled="cookieKeys.length === 0"
          @click="handleClearCookies"
        >
          {{ t('siteIdentity.menu.account.privacy.cookies.resetCta') }}
        </v-btn>
      </div>

      <v-table density="compact" class="account-privacy-table">
        <tbody>
          <tr v-for="key in cookieKeys" :key="key">
            <td class="text-caption text-neutral-secondary">{{ key }}</td>
          </tr>
          <tr v-if="cookieKeys.length === 0">
            <td class="text-caption text-neutral-soft text-center py-2">
              {{ t('siteIdentity.menu.account.privacy.cookies.empty') }}
            </td>
          </tr>
        </tbody>
      </v-table>
    </div>

    <!-- Storage -->
    <div class="account-privacy-card__section">
      <div class="d-flex justify-space-between align-center mb-2">
        <p class="account-privacy-card__section-title">
          {{ t('siteIdentity.menu.account.privacy.storage.title') }}
        </p>
        <v-btn
          color="primary"
          variant="text"
          size="x-small"
          data-testid="privacy-reset-local-storage"
          :disabled="localStorageKeys.length === 0"
          @click="handleClearLocalStorage"
        >
          {{ t('siteIdentity.menu.account.privacy.storage.resetCta') }}
        </v-btn>
      </div>

      <v-table density="compact" class="account-privacy-table">
        <tbody>
          <tr v-for="key in localStorageKeys" :key="key">
            <td class="text-caption text-neutral-secondary">{{ key }}</td>
          </tr>
          <tr v-if="localStorageKeys.length === 0">
            <td class="text-caption text-neutral-soft text-center py-2">
              {{ t('siteIdentity.menu.account.privacy.storage.empty') }}
            </td>
          </tr>
        </tbody>
      </v-table>
    </div>

    <!-- Compare -->
    <div v-if="compareCount > 0" class="mt-2">
      <v-divider class="mb-3" />
      <div class="d-flex justify-space-between align-center">
        <span class="text-caption font-weight-medium">{{
          t('siteIdentity.menu.account.privacy.compare.count', {
            count: compareCount,
          })
        }}</span>
        <v-btn
          to="/compare"
          color="primary"
          variant="flat"
          size="small"
          prepend-icon="mdi-scale-balance"
        >
          {{ t('siteIdentity.menu.account.privacy.compare.cta') }}
        </v-btn>
      </div>
    </div>
  </v-card>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { IpQuotaCategory } from '~~/shared/api-client'
import { storeToRefs } from 'pinia'
import { computed, ref, onMounted } from 'vue'
import { useIpQuota } from '~/composables/useIpQuota'
import { useProductCompareStore } from '~/stores/useProductCompareStore'

const { t } = useI18n()
const requestHeaders = useRequestHeaders(['x-forwarded-for', 'x-real-ip'])

// --- IP Logic ---
const resolveIpAddress = () => {
  const forwardedHeader = requestHeaders['x-forwarded-for']
  const forwardedIp = forwardedHeader?.split(',')[0]?.trim()
  return forwardedIp || requestHeaders['x-real-ip']?.trim() || null
}

const ipLabel = computed(() => {
  const resolvedIp = resolveIpAddress()
  return resolvedIp ?? t('siteIdentity.menu.account.privacy.ip.unavailable')
})

// --- Quota Logic ---
const { getRemaining, refreshQuota } = useIpQuota()

const aiQuotaCategory = IpQuotaCategory.ReviewGeneration
const voteQuotaCategory = IpQuotaCategory.FeedbackVote

const aiQuota = computed(() => getRemaining(aiQuotaCategory))
const voteQuota = computed(() => getRemaining(voteQuotaCategory))

const loadQuotas = async () => {
  if (import.meta.client) {
    await Promise.allSettled([
      refreshQuota(aiQuotaCategory),
      refreshQuota(voteQuotaCategory),
    ])
  }
}

// --- Cookies & Storage Logic ---
const cookieKeys = ref<string[]>([])
const localStorageKeys = ref<string[]>([])

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

// --- Compare Logic ---
const compareStore = useProductCompareStore()
const { items: compareItems } = storeToRefs(compareStore)
const compareCount = computed(() => compareItems.value.length)

onMounted(() => {
  refreshStorageSnapshot()
  loadQuotas()
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
