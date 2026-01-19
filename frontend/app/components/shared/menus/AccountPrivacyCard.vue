<template>
  <v-card
    class="account-privacy-card"
    color="surface-default"
    elevation="4"
    data-testid="account-privacy-card"
  >
    <div class="account-privacy-card__hero">
      <div class="account-privacy-card__hero-icon">
        <v-icon icon="mdi-shield-check" size="36" color="primary" />
      </div>
      <div class="account-privacy-card__hero-copy">
        <p class="account-privacy-card__hero-title">
          {{ t('siteIdentity.menu.account.privacy.hero.title') }}
        </p>
        <p class="account-privacy-card__hero-subtitle">
          {{ t('siteIdentity.menu.account.privacy.hero.subtitle') }}
        </p>
      </div>
    </div>

    <v-divider class="my-4" />

    <div class="account-privacy-card__section">
      <p class="account-privacy-card__section-label">
        {{ t('siteIdentity.menu.account.privacy.sections.remote') }}
      </p>

      <div class="account-privacy-card__subsection">
        <p class="account-privacy-card__section-title mb-2">
          {{ t('siteIdentity.menu.account.privacy.quotas.title') }}
        </p>
        <v-list density="compact" nav bg-color="transparent" class="pa-0">
          <v-list-item v-if="aiQuota !== null" class="px-0">
            <template #prepend>
              <v-icon icon="mdi-robot-outline" size="small" class="mr-2" />
            </template>
            <div class="d-flex align-center ga-2 w-100">
              <div class="d-flex align-center ga-2">
                <span class="text-body-2">{{
                  t('siteIdentity.menu.account.privacy.quotas.aiRemaining')
                }}</span>
                <v-tooltip location="top">
                  <template #activator="{ props }">
                    <v-icon
                      v-bind="props"
                      icon="mdi-information-outline"
                      size="small"
                      color="neutral-secondary"
                    />
                  </template>
                  <span>{{
                    t(
                      'siteIdentity.menu.account.privacy.quotas.aiRemainingTooltip'
                    )
                  }}</span>
                </v-tooltip>
              </div>
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
            <div class="d-flex align-center ga-2 w-100">
              <span class="text-body-2">{{
                t('siteIdentity.menu.account.privacy.quotas.votesRemaining')
              }}</span>
              <v-chip size="x-small" color="primary" variant="flat">{{
                voteQuota
              }}</v-chip>
            </div>
          </v-list-item>
        </v-list>
      </div>

      <div class="account-privacy-card__info-row d-flex align-center ga-2">
        <span class="text-body-2">{{
          t('siteIdentity.menu.account.privacy.technicalOnly.label')
        }}</span>
        <v-tooltip location="top">
          <template #activator="{ props }">
            <v-icon
              v-bind="props"
              icon="mdi-information-outline"
              size="small"
              color="neutral-secondary"
            />
          </template>
          <span>{{
            t('siteIdentity.menu.account.privacy.technicalOnly.tooltip')
          }}</span>
        </v-tooltip>
      </div>

      <div class="account-privacy-card__subsection">
        <div class="account-privacy-card__section-header">
          <div>
            <p class="account-privacy-card__section-title">
              {{ t('siteIdentity.menu.account.privacy.ip.title') }}
            </p>
            <p class="account-privacy-card__section-description">
              {{ t('siteIdentity.menu.account.privacy.ip.description') }}
            </p>
            <div class="account-privacy-card__meta">
              <div class="account-privacy-card__meta-row">
                <span class="account-privacy-card__meta-label">
                  {{ t('siteIdentity.menu.account.privacy.userAgent.label') }}
                </span>
                <v-tooltip v-if="userAgentFull" location="top">
                  <template #activator="{ props }">
                    <span
                      v-bind="props"
                      class="account-privacy-card__meta-value"
                    >
                      {{ userAgentDisplay }}
                    </span>
                  </template>
                  <span>{{ userAgentFull }}</span>
                </v-tooltip>
                <span v-else class="account-privacy-card__meta-value">
                  {{
                    t('siteIdentity.menu.account.privacy.userAgent.unavailable')
                  }}
                </span>
              </div>
            </div>
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
    </div>

    <div class="account-privacy-card__section">
      <p class="account-privacy-card__section-label">
        {{ t('siteIdentity.menu.account.privacy.sections.local') }}
      </p>

      <div class="account-privacy-card__subsection">
        <div
          class="d-flex justify-space-between align-center mb-2 cursor-pointer"
          @click="isCookiesListOpen = !isCookiesListOpen"
        >
          <div class="d-flex align-center ga-2">
            <p class="account-privacy-card__section-title">
              {{ t('siteIdentity.menu.account.privacy.cookies.title') }}
            </p>
            <v-icon
              :icon="isCookiesListOpen ? 'mdi-chevron-up' : 'mdi-chevron-down'"
              size="small"
              color="neutral-secondary"
            />
          </div>
          <v-btn
            color="primary"
            variant="text"
            size="x-small"
            data-testid="privacy-reset-cookies"
            :disabled="cookieKeys.length === 0"
            @click.stop="handleClearCookies"
          >
            {{ t('siteIdentity.menu.account.privacy.cookies.resetCta') }}
          </v-btn>
        </div>

        <v-expand-transition>
          <v-table
            v-if="isCookiesListOpen"
            density="compact"
            class="account-privacy-table"
          >
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
        </v-expand-transition>
      </div>

      <div class="account-privacy-card__subsection">
        <div
          class="d-flex justify-space-between align-center mb-2 cursor-pointer"
          @click="isStorageListOpen = !isStorageListOpen"
        >
          <div class="d-flex align-center ga-2">
            <p class="account-privacy-card__section-title">
              {{ t('siteIdentity.menu.account.privacy.storage.title') }}
            </p>
            <v-icon
              :icon="isStorageListOpen ? 'mdi-chevron-up' : 'mdi-chevron-down'"
              size="small"
              color="neutral-secondary"
            />
          </div>
          <v-btn
            color="primary"
            variant="text"
            size="x-small"
            data-testid="privacy-reset-local-storage"
            :disabled="localStorageKeys.length === 0"
            @click.stop="handleClearLocalStorage"
          >
            {{ t('siteIdentity.menu.account.privacy.storage.resetCta') }}
          </v-btn>
        </div>

        <v-expand-transition>
          <v-table
            v-if="isStorageListOpen"
            density="compact"
            class="account-privacy-table"
          >
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
        </v-expand-transition>
      </div>
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

    <div class="account-privacy-card__cta">
      <v-divider class="my-3" />
      <div class="d-flex flex-wrap justify-space-between ga-2">
        <v-btn :to="openSourcePath" color="primary" variant="flat" size="small">
          {{ t('siteIdentity.menu.account.privacy.ctas.openSource') }}
        </v-btn>
        <v-btn
          :to="dataPrivacyPath"
          color="primary"
          variant="text"
          size="small"
        >
          {{ t('siteIdentity.menu.account.privacy.ctas.dataPrivacy') }}
        </v-btn>
      </div>
    </div>
  </v-card>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { IpQuotaStatusDtoCategoryEnum } from '~~/shared/api-client'
import { storeToRefs } from 'pinia'
import { computed, ref, onMounted } from 'vue'
import { useIpQuota } from '~/composables/useIpQuota'
import { useProductCompareStore } from '~/stores/useProductCompareStore'
import {
  normalizeLocale,
  resolveLocalizedRoutePath,
} from '~~/shared/utils/localized-routes'

const { t, locale } = useI18n()
const requestHeaders = useRequestHeaders(['x-forwarded-for', 'x-real-ip'])
const currentLocale = computed(() => normalizeLocale(locale.value))
const dataPrivacyPath = computed(() =>
  resolveLocalizedRoutePath('data-privacy', currentLocale.value)
)
const openSourcePath = computed(() => '/opensource')

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

const aiQuotaCategory = IpQuotaStatusDtoCategoryEnum.ReviewGeneration
const voteQuotaCategory = IpQuotaStatusDtoCategoryEnum.FeedbackVote

const aiQuota = computed(() => getRemaining(aiQuotaCategory))
const voteQuota = computed(() => getRemaining(voteQuotaCategory))
const userAgentFull = ref<string | null>(null)

const userAgentDisplay = computed(() => {
  if (!userAgentFull.value) {
    return t('siteIdentity.menu.account.privacy.userAgent.unavailable')
  }

  const maxLength = 38
  return userAgentFull.value.length > maxLength
    ? `${userAgentFull.value.slice(0, maxLength)}…`
    : userAgentFull.value
})

const loadQuotas = async () => {
  if (import.meta.client) {
    await Promise.allSettled([
      refreshQuota(aiQuotaCategory),
      refreshQuota(voteQuotaCategory),
    ])
  }
}

// --- Cookies & Storage Logic ---
const isCookiesListOpen = ref(false)
const isStorageListOpen = ref(false)
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

  if (import.meta.client) {
    userAgentFull.value = window.navigator.userAgent
  }
})
</script>

<style scoped lang="sass">
.account-privacy-card
  border-radius: 20px
  padding: 20px

  &__hero
    display: flex
    align-items: flex-start
    gap: 16px
    padding: 16px
    border-radius: 16px
    background: rgba(var(--v-theme-surface-primary-080), 0.7)
    margin-bottom: 16px

  &__hero-icon
    width: 44px
    height: 44px
    border-radius: 12px
    display: flex
    align-items: center
    justify-content: center
    background: rgba(var(--v-theme-surface-primary-100), 0.9)
    flex-shrink: 0

  &__hero-copy
    display: flex
    flex-direction: column
    gap: 4px

  &__hero-title
    margin: 0
    font-size: 1rem
    font-weight: 700
    color: rgb(var(--v-theme-text-neutral-strong))

  &__hero-subtitle
    margin: 0
    font-size: 0.875rem
    color: rgb(var(--v-theme-text-neutral-secondary))

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

  &__section-label
    margin: 0
    font-size: 0.75rem
    font-weight: 700
    letter-spacing: 0.08em
    text-transform: uppercase
    color: rgb(var(--v-theme-text-neutral-soft))

  &__subsection
    display: flex
    flex-direction: column
    gap: 12px

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

  &__info-row
    font-size: 0.875rem
    color: rgb(var(--v-theme-text-neutral-secondary))

  &__meta
    margin-top: 8px
    display: flex
    flex-direction: column
    gap: 4px

  &__meta-row
    display: flex
    flex-wrap: wrap
    gap: 6px
    align-items: center

  &__meta-label
    font-size: 0.75rem
    font-weight: 600
    color: rgb(var(--v-theme-text-neutral-soft))

  &__meta-value
    font-size: 0.75rem
    color: rgb(var(--v-theme-text-neutral-secondary))
    word-break: break-word

  &__cta
    margin-top: 12px

  &__action
    text-transform: none

  &__empty
    margin: 0
    font-size: 0.85rem
    color: rgb(var(--v-theme-text-neutral-soft))

  @media (max-width: 599px)
    padding: 16px
    border-radius: 16px

    &__hero
      padding: 12px
      gap: 12px

    &__hero-icon
      width: 40px
      height: 40px

    &__header
      align-items: flex-start
</style>
