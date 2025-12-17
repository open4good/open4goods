<template>
  <div class="contrib-page">
    <v-container class="contrib-page__container" max-width="sm">
      <v-card class="contrib-page__card" elevation="10">
        <div
          v-if="isPending || showSuccess"
          class="contrib-page__state"
          role="status"
          aria-live="polite"
        >
          <v-progress-circular
            indeterminate
            color="primary"
            size="56"
            class="contrib-page__spinner"
          />
          <h1 class="contrib-page__title">
            {{
              isPending
                ? t('contrib.loading.title')
                : t('contrib.success.title')
            }}
          </h1>
          <p class="contrib-page__message">
            {{
              isPending
                ? t('contrib.loading.message')
                : t('contrib.success.message')
            }}
          </p>
          <v-btn
            v-if="showSuccess && redirectUrl"
            :href="redirectUrl"
            color="primary"
            variant="flat"
            size="large"
            class="contrib-page__cta"
            target="_blank"
            rel="noopener"
          >
            {{ t('contrib.success.cta') }}
          </v-btn>
        </div>
        <div
          v-else
          class="contrib-page__state contrib-page__state--error"
          role="alert"
          aria-live="assertive"
        >
          <v-icon
            icon="mdi-alert-circle-outline"
            size="56"
            color="error"
            class="contrib-page__icon"
            aria-hidden="true"
          />
          <h1 class="contrib-page__title">
            {{ t('contrib.error.title') }}
          </h1>
          <p class="contrib-page__message">
            {{ t('contrib.error.description') }}
          </p>
          <p v-if="errorDetails" class="contrib-page__details">
            {{ t('contrib.error.detailsPrefix') }} {{ errorDetails }}
          </p>
          <v-btn
            :to="homeLink"
            color="primary"
            variant="flat"
            size="large"
            class="contrib-page__cta"
          >
            {{ t('contrib.error.cta') }}
          </v-btn>
        </div>
      </v-card>
    </v-container>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import type { AffiliationRedirectResponse } from '~~/shared/api-client/services/affiliation.services'

definePageMeta({
  ssr: true,
})

const route = useRoute()
const requestURL = useRequestURL()
const localePath = useLocalePath()
const { t } = useI18n()
const requestHeaders = useRequestHeaders(['host', 'x-forwarded-host'])

const token = computed(() => {
  const param = route.params.token
  if (Array.isArray(param)) {
    return param[0] ?? ''
  }

  return typeof param === 'string' ? param : ''
})

const fetchRedirect = async () => {
  if (!token.value) {
    throw createError({
      statusCode: 400,
      statusMessage: 'Missing affiliation token',
    })
  }

  const encodedToken = encodeURIComponent(token.value)
  return $fetch<AffiliationRedirectResponse>(`/api/contrib/${encodedToken}`, {
    headers: requestHeaders,
  })
}

const { data, pending, error } =
  await useAsyncData<AffiliationRedirectResponse>(
    `contrib-redirect-${token.value}`,
    fetchRedirect
  )

const redirectUrl = computed(() => data.value?.location ?? null)
const isPending = computed(() => pending.value)
const hasError = computed(() => Boolean(error.value))
const showSuccess = computed(
  () => !isPending.value && !hasError.value && Boolean(redirectUrl.value)
)

const getErrorMessage = (err: unknown): string | null => {
  if (!err) {
    return null
  }

  const statusMessage = (err as { statusMessage?: string }).statusMessage
  if (typeof statusMessage === 'string' && statusMessage.length > 0) {
    return statusMessage
  }

  const maybeData = (
    err as { data?: { statusMessage?: string; message?: string } }
  ).data
  if (maybeData?.statusMessage) {
    return maybeData.statusMessage
  }

  if (maybeData?.message) {
    return maybeData.message
  }

  if (err instanceof Error && err.message) {
    return err.message
  }

  if (typeof err === 'string' && err.length > 0) {
    return err
  }

  return null
}

const errorDetails = computed(() => getErrorMessage(error.value))
const hasLoggedError = ref(false)
const hasTriggeredRedirect = ref(false)

if (import.meta.client) {
  watch(
    () => redirectUrl.value,
    async value => {
      if (!value || hasTriggeredRedirect.value) {
        return
      }

      hasTriggeredRedirect.value = true

      try {
        await navigateTo(value, {
          external: true,
          replace: true,
        })
      } catch (navigationError) {
        console.warn('Client-side redirect failed', navigationError)
        hasTriggeredRedirect.value = false
      }
    },
    { immediate: true }
  )

  watch(
    () => error.value,
    currentError => {
      if (!currentError || hasLoggedError.value) {
        return
      }

      hasLoggedError.value = true
      const message = getErrorMessage(currentError)
      console.warn(
        'Affiliation redirect returned a non-redirect response',
        message,
        currentError
      )
    },
    { immediate: true }
  )
}

const canonicalUrl = computed(() => requestURL.href)
const ogImageUrl = computed(() =>
  new URL('/nudger-icon-512x512.png', requestURL.origin).toString()
)
const homeLink = computed(() => localePath({ name: 'index' }) ?? '/')

useSeoMeta({
  title: () => String(t('contrib.seo.title')),
  description: () => String(t('contrib.seo.description')),
  ogTitle: () => String(t('contrib.seo.title')),
  ogDescription: () => String(t('contrib.seo.description')),
  ogUrl: () => canonicalUrl.value,
  ogType: () => 'website',
  ogImage: () => ogImageUrl.value,
})

useHead(() => ({
  link: [{ rel: 'canonical', href: canonicalUrl.value }],
}))
</script>

<style scoped lang="sass">
.contrib-page
  min-height: 100vh
  display: flex
  align-items: center
  justify-content: center
  padding: 3rem 1.5rem
  background: rgb(var(--v-theme-surface-muted))

  &__container
    padding: 0

  &__card
    padding: 2.5rem 2rem
    border-radius: 1.5rem
    background: rgb(var(--v-theme-surface-default))
    text-align: center

  &__state
    display: flex
    flex-direction: column
    align-items: center
    gap: 1.25rem

    &--error
      color: rgb(var(--v-theme-error))

  &__spinner
    margin-bottom: 0.5rem

  &__title
    font-size: clamp(1.5rem, 2vw + 1rem, 2rem)
    font-weight: 600
    color: rgb(var(--v-theme-text-neutral-strong))

  &__message
    font-size: 1rem
    color: rgb(var(--v-theme-text-neutral-secondary))

  &__details
    font-size: 0.875rem
    color: rgb(var(--v-theme-text-neutral-soft))

  &__icon
    color: rgb(var(--v-theme-error))

  &__cta
    margin-top: 0.5rem
</style>
