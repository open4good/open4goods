<template>
  <v-container class="share-callback" fluid>
    <v-row justify="center">
      <v-col cols="12" md="8">
        <v-alert
          v-if="missingUrl"
          type="error"
          border="start"
          variant="tonal"
          class="mb-4"
        >
          {{ t('share.errors.missingUrl') }}
        </v-alert>

        <ShareResolutionLoader
          v-else-if="isPending || isLoading"
          :status="status"
          :message="resolution?.message ?? errorMessage"
          :origin-url="resolution?.originUrl || sharedUrl"
          :elapsed-ms="elapsedMs"
          :max-seconds="MAX_SLA_SECONDS"
          :is-standalone="isStandalone"
        />

        <v-alert
          v-else-if="isTimeout"
          type="warning"
          variant="tonal"
          border="start"
          class="mb-4"
        >
          <div class="share-callback__alert">
            <div>
              <p class="share-callback__alert-title">
                {{ t('share.timeout.title') }}
              </p>
              <p class="share-callback__alert-body">
                {{ resolution?.message ?? t('share.timeout.body') }}
              </p>
            </div>
            <v-btn color="primary" variant="flat" @click="restart">
              {{ t('share.actions.retry') }}
            </v-btn>
          </div>
        </v-alert>

        <v-alert
          v-else-if="isErrored"
          type="error"
          variant="tonal"
          border="start"
          class="mb-4"
        >
          <p class="share-callback__alert-title">
            {{ t('share.errors.title') }}
          </p>
          <p class="share-callback__alert-body">
            {{ resolution?.message ?? errorMessage ?? t('share.errors.generic') }}
          </p>
          <v-btn color="primary" variant="text" @click="restart">
            {{ t('share.actions.retry') }}
          </v-btn>
        </v-alert>

        <template v-else-if="isResolved && hasCandidates">
          <p class="share-callback__context">{{ contextLabel }}</p>

          <ShareCandidateList
            v-if="hasMultipleCandidates"
            :candidates="resolution?.candidates ?? []"
            :selected-product-id="selectedCandidate?.productId ?? null"
            class="mb-4"
            @select="handleCandidateSelect"
          />

          <ShareResolutionResultCard
            v-if="selectedCandidate"
            :candidate="selectedCandidate"
          />
        </template>

        <v-alert v-else type="info" variant="tonal" border="start">
          {{ t('share.empty') }}
        </v-alert>
      </v-col>
    </v-row>
  </v-container>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useDisplay } from 'vuetify'
import { useI18n } from 'vue-i18n'
import type { ShareCandidateDto } from '~~/shared/api-client'
import ShareCandidateList from '~/components/share/ShareCandidateList.vue'
import ShareResolutionLoader from '~/components/share/ShareResolutionLoader.vue'
import ShareResolutionResultCard from '~/components/share/ShareResolutionResultCard.vue'
import { useShareResolution } from '~/composables/share/useShareResolution'
import { useAnalytics } from '~/composables/useAnalytics'

const MAX_SLA_SECONDS = 4

definePageMeta({
  layout: 'default',
  ssr: true,
})

const { t } = useI18n()
const { trackEvent } = useAnalytics()
const route = useRoute()
const { mobile } = useDisplay()

const sharedUrl = computed(() => extractQueryParam('url'))
const sharedTitle = computed(() => extractQueryParam('title'))
const sharedText = computed(() => extractQueryParam('text'))
const sharedDomainLanguage = computed(() =>
  ['en', 'fr'].includes(extractQueryParam('domainLanguage') ?? '')
    ? (extractQueryParam('domainLanguage') as 'en' | 'fr')
    : undefined
)

const missingUrl = computed(() => !sharedUrl.value)

const isStandalone = ref(false)
let displayModeListener: ((event: MediaQueryListEvent) => void) | null = null

const {
  resolution,
  status,
  isPending,
  isResolved,
  isTimeout,
  isErrored,
  isLoading,
  elapsedMs,
  errorMessage,
  startResolution,
  reset,
} = useShareResolution({ maxDurationMs: MAX_SLA_SECONDS * 1000 })

const selectedCandidate = ref<ShareCandidateDto | null>(null)

const hasCandidates = computed(() => (resolution.value?.candidates?.length ?? 0) > 0)
const hasMultipleCandidates = computed(
  () => (resolution.value?.candidates?.length ?? 0) > 1
)

const contextLabel = computed(() =>
  hasMultipleCandidates.value
    ? t('share.selection.context', { count: resolution.value?.candidates.length ?? 0 })
    : t('share.result.context')
)

const extractDisplayMode = () => {
  if (!import.meta.client) {
    return
  }

  const media = window.matchMedia('(display-mode: standalone)')
  const navigatorWithStandalone =
    window.navigator as Navigator & { standalone?: boolean }
  const updateStandalone = () => {
    isStandalone.value =
      media.matches || navigatorWithStandalone.standalone === true
  }

  updateStandalone()
  displayModeListener = () => updateStandalone()
  media.addEventListener('change', displayModeListener)
}

const startFlow = async () => {
  if (missingUrl.value) {
    return
  }

  trackEvent('share-resolution-start', {
    props: {
      domainLanguage: sharedDomainLanguage.value,
      originUrl: sharedUrl.value,
      mode: isStandalone.value ? 'standalone' : 'browser',
    },
  })

  await startResolution({
    url: sharedUrl.value!,
    title: sharedTitle.value ?? undefined,
    text: sharedText.value ?? undefined,
    domainLanguage: sharedDomainLanguage.value,
  })
}

const restart = async () => {
  reset()
  await startFlow()
}

const handleCandidateSelect = (candidate: ShareCandidateDto) => {
  selectedCandidate.value = candidate
  trackEvent('share-resolution-select', {
    props: {
      productId: candidate.productId,
      source: 'list',
    },
  })
}

watch(
  () => status.value,
  newStatus => {
    if (newStatus === 'RESOLVED') {
      trackEvent('share-resolution-resolved', {
        props: {
          candidates: resolution.value?.candidates?.length ?? 0,
          originUrl: resolution.value?.originUrl,
        },
      })
    }

    if (newStatus === 'TIMEOUT') {
      trackEvent('share-resolution-timeout', {
        props: { originUrl: resolution.value?.originUrl },
      })
    }

    if (newStatus === 'ERROR') {
      trackEvent('share-resolution-error', {
        props: { originUrl: resolution.value?.originUrl },
      })
    }
  }
)

watch(
  () => resolution.value?.candidates,
  candidates => {
    if (!candidates?.length) {
      selectedCandidate.value = null
      return
    }

    if (candidates.length === 1) {
      selectedCandidate.value = candidates[0]
      return
    }

    if (!mobile.value && !selectedCandidate.value) {
      selectedCandidate.value = candidates[0]
    }
  }
)

onMounted(() => {
  extractDisplayMode()
  startFlow()
})

onBeforeUnmount(() => {
  const media = window.matchMedia('(display-mode: standalone)')
  if (displayModeListener) {
    media.removeEventListener('change', displayModeListener)
  }
})

useSeoMeta({
  title: () => t('share.seo.title'),
  ogTitle: () => t('share.seo.title'),
  description: () => t('share.seo.description'),
  ogDescription: () => t('share.seo.description'),
})

function extractQueryParam(param: string): string | undefined {
  const rawValue = route.query[param]
  if (Array.isArray(rawValue)) {
    return rawValue[0]
  }
  return typeof rawValue === 'string' ? rawValue : undefined
}
</script>

<style scoped>
.share-callback {
  min-height: 100vh;
  display: flex;
  align-items: flex-start;
  padding-top: 2rem;
  padding-bottom: 2rem;
}

.share-callback__alert {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: center;
  flex-wrap: wrap;
}

.share-callback__alert-title {
  font-weight: 700;
  margin: 0;
}

.share-callback__alert-body {
  margin: 0.25rem 0 0;
  color: rgb(var(--v-theme-text-neutral-secondary));
}

.share-callback__context {
  margin: 0 0 0.75rem;
  color: rgb(var(--v-theme-text-neutral-secondary));
}
</style>
