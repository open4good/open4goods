<template>
  <section :id="sectionId" class="product-ai-review">
    <header class="product-ai-review__header">
      <h2 class="product-ai-review__title">
        {{ $t('product.aiReview.title') }}
      </h2>
      <p class="product-ai-review__subtitle">
        {{ $t('product.aiReview.subtitle') }}
      </p>
    </header>

    <div v-if="reviewContent" class="product-ai-review__content" itemscope itemtype="https://schema.org/Review">
      <div v-if="createdDate" class="product-ai-review__metadata">
        <v-icon icon="mdi-calendar-clock" size="18" class="product-ai-review__metadata-icon" />
        <span>{{ $t('product.aiReview.generatedAt', { date: createdDate }) }}</span>
      </div>

      <article class="product-ai-review__article">
        <div
          v-if="reviewContent.mediumTitle || reviewContent.shortDescription"
          class="product-ai-review__intro-card"
        >
          <div class="product-ai-review__intro-icon">
            <v-icon icon="mdi-robot-outline" size="28" />
          </div>
          <div class="product-ai-review__intro-content">
            <h3 v-if="reviewContent.mediumTitle" class="product-ai-review__article-title">
              {{ reviewContent.mediumTitle }}
            </h3>
            <p
              v-if="reviewContent.shortDescription"
              class="product-ai-review__summary"
              itemprop="description"
            >
              {{ reviewContent.shortDescription }}
            </p>
          </div>
        </div>

        <div v-if="hasAnalysisSections" class="product-ai-review__analysis-grid">
          <section v-if="reviewContent.summary" class="product-ai-review__card" itemprop="reviewBody">
            <header class="product-ai-review__card-header">
              <div class="product-ai-review__card-icon">
                <v-icon icon="mdi-lightbulb-on-outline" size="22" />
              </div>
              <h4 class="product-ai-review__card-title">
                {{ $t('product.aiReview.sections.overall') }}
              </h4>
            </header>
            <p class="product-ai-review__card-text">{{ reviewContent.summary }}</p>
          </section>

          <section
            v-if="reviewContent.description"
            class="product-ai-review__card product-ai-review__card--wide"
          >
            <header class="product-ai-review__card-header">
              <div class="product-ai-review__card-icon product-ai-review__card-icon--accent">
                <v-icon icon="mdi-text-box-outline" size="22" />
              </div>
              <h4 class="product-ai-review__card-title">
                {{ $t('product.aiReview.sections.details') }}
              </h4>
            </header>
            <div class="product-ai-review__card-body">
              <!-- eslint-disable-next-line vue/no-v-html -->
              <div ref="descriptionRef" class="product-ai-review__richtext" v-html="reviewContent.description" />
            </div>
          </section>

          <section v-if="reviewContent.technicalReview" class="product-ai-review__card">
            <header class="product-ai-review__card-header">
              <div class="product-ai-review__card-icon">
                <v-icon icon="mdi-cog-outline" size="22" />
              </div>
              <h4 class="product-ai-review__card-title">
                {{ $t('product.aiReview.sections.technical') }}
              </h4>
            </header>
            <!-- eslint-disable-next-line vue/no-v-html -->
            <p class="product-ai-review__card-text" v-html="reviewContent.technicalReview" />
          </section>

          <section v-if="reviewContent.ecologicalReview" class="product-ai-review__card">
            <header class="product-ai-review__card-header">
              <div class="product-ai-review__card-icon product-ai-review__card-icon--eco">
                <v-icon icon="mdi-leaf-outline" size="22" />
              </div>
              <h4 class="product-ai-review__card-title">
                {{ $t('product.aiReview.sections.ecological') }}
              </h4>
            </header>
            <!-- eslint-disable-next-line vue/no-v-html -->
            <p class="product-ai-review__card-text" v-html="reviewContent.ecologicalReview" />
          </section>
        </div>
      </article>

      <div
        v-if="reviewContent.pros?.length || reviewContent.cons?.length"
        class="product-ai-review__insights"
      >
        <section v-if="reviewContent.pros?.length" class="product-ai-review__panel">
          <header class="product-ai-review__panel-header">
            <div class="product-ai-review__panel-icon product-ai-review__panel-icon--pros">
              <v-icon icon="mdi-thumb-up-outline" size="20" />
            </div>
            <h4 class="product-ai-review__panel-title">
              {{ $t('product.aiReview.sections.pros') }}
            </h4>
          </header>
          <ul class="product-ai-review__list">
            <li v-for="pro in reviewContent.pros" :key="pro" class="product-ai-review__list-item">
              <v-icon
                icon="mdi-check-circle-outline"
                size="18"
                class="product-ai-review__list-icon product-ai-review__list-icon--pros"
              />
              <!-- eslint-disable-next-line vue/no-v-html -->
              <span v-html="pro" />
            </li>
          </ul>
        </section>

        <section v-if="reviewContent.cons?.length" class="product-ai-review__panel">
          <header class="product-ai-review__panel-header">
            <div class="product-ai-review__panel-icon product-ai-review__panel-icon--cons">
              <v-icon icon="mdi-alert-circle-outline" size="20" />
            </div>
            <h4 class="product-ai-review__panel-title">
              {{ $t('product.aiReview.sections.cons') }}
            </h4>
          </header>
          <ul class="product-ai-review__list">
            <li v-for="con in reviewContent.cons" :key="con" class="product-ai-review__list-item">
              <v-icon
                icon="mdi-close-circle-outline"
                size="18"
                class="product-ai-review__list-icon product-ai-review__list-icon--cons"
              />
              <!-- eslint-disable-next-line vue/no-v-html -->
              <span v-html="con" />
            </li>
          </ul>
        </section>
      </div>

      <div v-if="reviewContent.attributes?.length" class="product-ai-review__attributes">
        <header class="product-ai-review__section-header">
          <div class="product-ai-review__section-icon">
            <v-icon icon="mdi-card-account-details-outline" size="20" />
          </div>
          <h4>{{ $t('product.aiReview.sections.identity') }}</h4>
        </header>
        <v-table density="comfortable" class="product-ai-review__table">
          <tbody>
            <tr v-for="attribute in reviewContent.attributes" :key="attribute.name">
              <th scope="row">{{ attribute.name }}</th>
              <td>{{ attribute.value }}</td>
            </tr>
          </tbody>
        </v-table>
      </div>

      <div v-if="reviewContent.sources?.length" class="product-ai-review__sources">
        <header class="product-ai-review__section-header">
          <div class="product-ai-review__section-icon">
            <v-icon icon="mdi-book-open-variant" size="20" />
          </div>
          <h4>{{ $t('product.aiReview.sections.sources') }}</h4>
        </header>
        <v-table density="compact" class="product-ai-review__table product-ai-review__table--compact">
          <thead>
            <tr>
              <th scope="col">{{ $t('product.aiReview.sources.index') }}</th>
              <th scope="col">{{ $t('product.aiReview.sources.source') }}</th>
              <th scope="col">{{ $t('product.aiReview.sources.description') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="source in reviewContent.sources" :id="`review-ref-${source.number}`" :key="source.number">
              <td>
                <a :href="source.url" target="_blank" rel="noopener">
                  [{{ source.number }}]
                </a>
              </td>
              <td>
                <div class="product-ai-review__source-info">
                  <img
                    v-if="source.favicon"
                    :src="source.favicon"
                    :alt="source.name"
                    class="product-ai-review__source-favicon"
                    width="16"
                    height="16"
                  >
                  <span>{{ source.name }}</span>
                </div>
              </td>
              <td>
                <a :href="source.url" target="_blank" rel="noopener">
                  {{ source.description }}
                </a>
              </td>
            </tr>
          </tbody>
        </v-table>
      </div>
    </div>

    <div v-else class="product-ai-review__empty">
      <p class="product-ai-review__empty-message">
        {{ $t('product.aiReview.empty') }}
      </p>
      <div class="product-ai-review__actions">
        <v-btn color="primary" :loading="requesting" :disabled="requesting" @click="startRequest">
          {{ $t('product.aiReview.requestButton') }}
        </v-btn>
        <v-alert v-if="errorMessage" type="error" class="mt-4" border="start">
          {{ errorMessage }}
        </v-alert>
      </div>
      <ClientOnly>
        <VueHcaptcha
          v-if="showCaptcha && hasSiteKey"
          ref="captchaRef"
          :sitekey="siteKey"
          :theme="captchaTheme"
          :language="captchaLocale"
          @verify="handleCaptchaVerify"
          @expired="handleCaptchaExpired"
          @error="handleCaptchaError"
        />
      </ClientOnly>
      <div v-if="statusMessage" class="product-ai-review__status">
        <v-progress-linear
          v-if="isGenerating"
          color="primary"
          :model-value="statusPercent"
          rounded
          height="6"
        />
        <p>{{ statusMessage }}</p>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import type { PropType } from 'vue'
import VueHcaptcha from '@hcaptcha/vue3-hcaptcha'
import { useI18n } from 'vue-i18n'
import { useTheme } from 'vuetify'
import { format } from 'date-fns'
import { fr, enUS } from 'date-fns/locale'
import DOMPurify from 'isomorphic-dompurify'
import type { AiReviewDto, AiReviewSourceDto, AiReviewAttributeDto, ReviewGenerationStatus } from '~~/shared/api-client'

interface ReviewContent {
  description?: string | null
  shortDescription?: string | null
  mediumTitle?: string | null
  shortTitle?: string | null
  technicalReview?: string | null
  ecologicalReview?: string | null
  summary?: string | null
  pros?: string[]
  cons?: string[]
  sources?: AiReviewSourceDto[]
  attributes?: AiReviewAttributeDto[]
  dataQuality?: string | null
}

const props = defineProps({
  sectionId: {
    type: String,
    default: 'synthese',
  },
  gtin: {
    type: [String, Number],
    required: true,
  },
  initialReview: {
    type: Object as PropType<AiReviewDto | null>,
    default: null,
  },
  reviewCreatedAt: {
    type: Number,
    default: null,
  },
  siteKey: {
    type: String,
    default: '',
  },
})

const { locale, t } = useI18n()
const theme = useTheme()

const review = ref<ReviewContent | null>(normalizeReview(props.initialReview))
const createdMs = ref<number | null>(props.reviewCreatedAt ?? null)
const requesting = ref(false)
const errorMessage = ref<string | null>(null)
const status = ref<ReviewGenerationStatus | null>(null)
const pollHandle = ref<number | null>(null)
const showCaptcha = ref(false)
const captchaToken = ref<string | null>(null)
const captchaRef = ref<InstanceType<typeof VueHcaptcha> | null>(null)
const descriptionRef = ref<HTMLElement | null>(null)

watch(
  () => props.initialReview,
  (value) => {
    review.value = normalizeReview(value)
  },
)

watch(
  () => props.reviewCreatedAt,
  (value) => {
    createdMs.value = value ?? null
  },
)

const hasSiteKey = computed(() => props.siteKey.length > 0)
const captchaTheme = computed(() => (theme.global.current.value.dark ? 'dark' : 'light'))
const captchaLocale = computed(() => (locale.value.startsWith('fr') ? 'fr' : 'en'))

const reviewContent = computed(() => review.value)

const hasAnalysisSections = computed(() => {
  const content = reviewContent.value
  if (!content) {
    return false
  }

  return Boolean(content.summary || content.description || content.technicalReview || content.ecologicalReview)
})

const createdDate = computed(() => {
  if (!createdMs.value) {
    return null
  }

  return format(createdMs.value, 'PPP', {
    locale: locale.value.startsWith('fr') ? fr : enUS,
  })
})

const isGenerating = computed(() => status.value?.status && status.value.status !== 'SUCCESS' && status.value.status !== 'FAILED')
const statusPercent = computed(() => Math.round(status.value?.percent ?? 0))

const statusMessage = computed(() => {
  if (!status.value) {
    return null
  }

  if (status.value.status === 'FAILED') {
    return t('product.aiReview.status.failed')
  }

  if (status.value.status === 'SUCCESS') {
    return t('product.aiReview.status.success')
  }

  return t('product.aiReview.status.running', {
    step: status.value.status?.toLowerCase() ?? 'pending',
  })
})

function sanitizeHtml(content: string | null): string | null {
  if (!content) {
    return null
  }

  return DOMPurify.sanitize(content, {
    ADD_ATTR: ['target', 'rel', 'class'],
  })
}

function normalizeReview(reviewData: AiReviewDto | null): ReviewContent | null {
  if (!reviewData) {
    return null
  }

  const pros = Array.isArray(reviewData.pros)
    ? reviewData.pros
        .map((entry) => sanitizeHtml(String(entry)))
        .filter((entry): entry is string => typeof entry === 'string' && entry.length > 0)
    : []
  const cons = Array.isArray(reviewData.cons)
    ? reviewData.cons
        .map((entry) => sanitizeHtml(String(entry)))
        .filter((entry): entry is string => typeof entry === 'string' && entry.length > 0)
    : []

  const sources: AiReviewSourceDto[] = (reviewData.sources ?? [])
    .map((source) => ({
      number: source.number ?? undefined,
      name: source.name ?? undefined,
      description: source.description ?? undefined,
      url: source.url ?? undefined,
      favicon: source.favicon ?? undefined,
    }))
    .filter((source) => Boolean(source.url))

  const attributes: AiReviewAttributeDto[] = (reviewData.attributes ?? [])
    .map((attribute) => ({
      name: attribute.name ?? '',
      value: attribute.value ?? '',
      number: attribute.number ?? undefined,
    }))
    .filter((attribute) => attribute.name.length > 0)

  return {
    description: sanitizeHtml(reviewData.description ?? null),
    shortDescription: reviewData.shortDescription ?? null,
    mediumTitle: reviewData.mediumTitle ?? null,
    shortTitle: reviewData.shortTitle ?? null,
    technicalReview: sanitizeHtml(reviewData.technicalReview ?? null),
    ecologicalReview: sanitizeHtml(reviewData.ecologicalReview ?? null),
    summary: reviewData.summary ?? null,
    pros,
    cons,
    sources,
    attributes,
    dataQuality: reviewData.dataQuality ?? null,
  }
}

const startRequest = () => {
  errorMessage.value = null
  showCaptcha.value = true
  captchaRef.value?.reset?.()
}

const handleCaptchaVerify = async (token: string) => {
  captchaToken.value = token
  await triggerGeneration()
}

const handleCaptchaExpired = () => {
  captchaToken.value = null
}

const handleCaptchaError = () => {
  captchaToken.value = null
  errorMessage.value = t('product.aiReview.errors.captcha')
}

const triggerGeneration = async () => {
  if (!captchaToken.value) {
    errorMessage.value = t('product.aiReview.errors.captcha')
    return
  }

  requesting.value = true
  errorMessage.value = null

  try {
    await $fetch(`/api/products/${props.gtin}/review`, {
      method: 'POST',
      body: {
        hcaptchaResponse: captchaToken.value,
      },
    })

    startPolling()
  } catch (error) {
    console.error('Failed to trigger AI review', error)
    errorMessage.value = error instanceof Error ? error.message : t('product.aiReview.errors.generic')
  } finally {
    requesting.value = false
  }
}

const pollStatus = async () => {
  try {
    const response = await $fetch<ReviewGenerationStatus>(`/api/products/${props.gtin}/review`)
    if (!response || !response.status) {
      stopPolling()
      errorMessage.value = t('product.aiReview.errors.generic')
      return
    }

    status.value = response

    if (response.status === 'FAILED') {
      stopPolling()
      errorMessage.value =
        response.result?.enoughData === false
          ? t('product.aiReview.errors.notEnoughData')
          : response.errorMessage ?? t('product.aiReview.errors.generic')
      return
    }

    if (response.status === 'SUCCESS') {
      const hasReview = Boolean(response.result?.review)
      stopPolling()

      if (!hasReview) {
        errorMessage.value =
          response.result?.enoughData === false
            ? t('product.aiReview.errors.notEnoughData')
            : t('product.aiReview.errors.generic')
        return
      }

      review.value = normalizeReview(response.result.review)
      createdMs.value = response.result.createdMs ?? Date.now()
    }
  } catch (error) {
    console.error('Failed to fetch review status', error)
    errorMessage.value = error instanceof Error ? error.message : t('product.aiReview.errors.generic')
    stopPolling()
  }
}

const startPolling = () => {
  stopPolling()
  pollStatus()
  if (import.meta.client) {
    pollHandle.value = window.setInterval(() => {
      void pollStatus()
    }, 4000)
  }
}

const stopPolling = () => {
  if (pollHandle.value) {
    window.clearInterval(pollHandle.value)
    pollHandle.value = null
  }
}

watch(
  descriptionRef,
  (element, _, onCleanup) => {
    if (!element) {
      return
    }

    const handler = (event: Event) => handleReferenceClick(event)
    element.addEventListener('click', handler)

    onCleanup(() => {
      element.removeEventListener('click', handler)
    })
  },
  { immediate: true },
)

onBeforeUnmount(() => {
  stopPolling()
})

const handleReferenceClick = (event: Event) => {
  const target = event.target as HTMLElement | null
  if (!target) {
    return
  }

  const anchor = target.closest('.review-ref') as HTMLAnchorElement | null
  if (anchor?.hash) {
    event.preventDefault()
    const element = document.querySelector(anchor.hash) as HTMLElement | null
    if (element) {
      element.scrollIntoView({ behavior: 'smooth' })
    }
  }
}
</script>

<style scoped>
.product-ai-review {
  display: flex;
  flex-direction: column;
  gap: 1.75rem;
}

.product-ai-review__header {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.product-ai-review__title {
  font-size: clamp(1.6rem, 2.5vw, 2.2rem);
  font-weight: 700;
}

.product-ai-review__subtitle {
  color: rgba(var(--v-theme-text-neutral-secondary), 0.85);
}

.product-ai-review__metadata {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  color: rgba(var(--v-theme-text-neutral-soft), 0.95);
  font-size: 0.95rem;
  background: rgba(var(--v-theme-surface-primary-080), 0.6);
  padding: 0.45rem 0.85rem;
  border-radius: 999px;
  align-self: flex-start;
}

.product-ai-review__metadata-icon {
  color: rgb(var(--v-theme-accent-primary-highlight));
}

.product-ai-review__content {
  background: rgba(var(--v-theme-surface-glass), 0.92);
  border-radius: 24px;
  padding: 1.75rem;
  box-shadow: inset 0 0 0 1px rgba(var(--v-theme-border-primary-strong), 0.08),
    0 18px 40px -24px rgba(var(--v-theme-shadow-primary-600), 0.4);
  display: flex;
  flex-direction: column;
  gap: 1.75rem;
}

.product-ai-review__article {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.product-ai-review__intro-card {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 1rem;
  align-items: flex-start;
  padding: 1.4rem;
  border-radius: 20px;
  background: rgba(var(--v-theme-surface-primary-080), 0.9);
  box-shadow: inset 0 0 0 1px rgba(var(--v-theme-border-primary-strong), 0.12);
}

.product-ai-review__intro-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 3rem;
  height: 3rem;
  border-radius: 16px;
  background: rgba(var(--v-theme-accent-primary-highlight), 0.16);
  color: rgb(var(--v-theme-accent-primary-highlight));
}

.product-ai-review__article-title {
  font-size: clamp(1.3rem, 2vw, 1.6rem);
  font-weight: 600;
  margin-bottom: 0.35rem;
}

.product-ai-review__summary {
  margin: 0;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.95);
  line-height: 1.6;
}

.product-ai-review__analysis-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(230px, 1fr));
  gap: 1.25rem;
}

.product-ai-review__card {
  background: rgba(var(--v-theme-surface-glass-strong), 0.96);
  border-radius: 18px;
  padding: 1.35rem;
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
  box-shadow: inset 0 0 0 1px rgba(var(--v-theme-border-primary-strong), 0.06);
}

.product-ai-review__card--wide {
  grid-column: span 1;
}

.product-ai-review__card-header {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
}

.product-ai-review__card-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 2.5rem;
  height: 2.5rem;
  border-radius: 12px;
  background: rgba(var(--v-theme-surface-primary-080), 0.9);
  color: rgb(var(--v-theme-accent-primary-highlight));
}

.product-ai-review__card-icon--accent {
  background: rgba(var(--v-theme-surface-primary-100), 0.95);
}

.product-ai-review__card-icon--eco {
  background: rgba(var(--v-theme-accent-supporting), 0.18);
  color: rgb(var(--v-theme-accent-supporting));
}

.product-ai-review__card-title {
  font-size: 1.05rem;
  font-weight: 600;
  margin: 0;
}

.product-ai-review__card-text {
  margin: 0;
  line-height: 1.65;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.product-ai-review__card-body {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.product-ai-review__richtext {
  line-height: 1.7;
  display: grid;
  gap: 0.65rem;
}

.product-ai-review__richtext p {
  margin: 0;
}

.product-ai-review__richtext a {
  color: rgb(var(--v-theme-accent-primary-highlight));
  text-decoration: underline;
}

.product-ai-review__insights {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 1.25rem;
}

.product-ai-review__panel {
  background: rgba(var(--v-theme-surface-glass-strong), 0.98);
  border-radius: 18px;
  padding: 1.3rem;
  box-shadow: inset 0 0 0 1px rgba(var(--v-theme-border-primary-strong), 0.05);
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
}

.product-ai-review__panel-header {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.product-ai-review__panel-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 2.25rem;
  height: 2.25rem;
  border-radius: 999px;
  background: rgba(var(--v-theme-surface-primary-080), 0.8);
}

.product-ai-review__panel-icon--pros {
  color: rgb(var(--v-theme-accent-supporting));
  background: rgba(var(--v-theme-accent-supporting), 0.16);
}

.product-ai-review__panel-icon--cons {
  color: rgba(var(--v-theme-error), 0.8);
  background: rgba(var(--v-theme-error), 0.18);
}

.product-ai-review__panel-title {
  font-size: 1.05rem;
  font-weight: 600;
  margin: 0;
}

.product-ai-review__list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: 0.65rem;
}

.product-ai-review__list-item {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 0.6rem;
  align-items: flex-start;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.product-ai-review__list-icon {
  margin-top: 0.2rem;
}

.product-ai-review__list-icon--pros {
  color: rgb(var(--v-theme-accent-supporting));
}

.product-ai-review__list-icon--cons {
  color: rgba(var(--v-theme-error), 0.85);
}

.product-ai-review__attributes,
.product-ai-review__sources {
  display: flex;
  flex-direction: column;
  gap: 0.9rem;
}

.product-ai-review__section-header {
  display: flex;
  align-items: center;
  gap: 0.6rem;
}

.product-ai-review__section-header h4 {
  margin: 0;
  font-size: 1.05rem;
  font-weight: 600;
}

.product-ai-review__section-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 2.2rem;
  height: 2.2rem;
  border-radius: 999px;
  background: rgba(var(--v-theme-surface-primary-080), 0.85);
  color: rgb(var(--v-theme-accent-primary-highlight));
}

.product-ai-review__table {
  border-radius: 18px;
  overflow: hidden;
  background: rgba(var(--v-theme-surface-glass-strong), 0.98);
  box-shadow: inset 0 0 0 1px rgba(var(--v-theme-border-primary-strong), 0.06);
}

.product-ai-review__table :deep(table) {
  width: 100%;
  border-collapse: collapse;
}

.product-ai-review__table :deep(th),
.product-ai-review__table :deep(td) {
  padding: 0.8rem 1rem;
  border-bottom: 1px solid rgba(var(--v-theme-border-primary-strong), 0.08);
  text-align: left;
}

.product-ai-review__table :deep(thead th) {
  background: rgba(var(--v-theme-surface-primary-080), 0.8);
  color: rgb(var(--v-theme-text-neutral-strong));
  font-weight: 600;
}

.product-ai-review__table :deep(tbody tr:last-child th),
.product-ai-review__table :deep(tbody tr:last-child td) {
  border-bottom: none;
}

.product-ai-review__table--compact :deep(th),
.product-ai-review__table--compact :deep(td) {
  padding: 0.65rem 0.75rem;
}

.product-ai-review__source-info {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
}

.product-ai-review__source-favicon {
  border-radius: 4px;
}

.product-ai-review__empty {
  background: rgba(var(--v-theme-surface-glass), 0.92);
  border-radius: 24px;
  padding: 1.75rem;
  display: flex;
  flex-direction: column;
  gap: 1.1rem;
  align-items: flex-start;
  box-shadow: inset 0 0 0 1px rgba(var(--v-theme-border-primary-strong), 0.08);
}

.product-ai-review__empty-message {
  font-size: 1rem;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.92);
  line-height: 1.55;
}

.product-ai-review__status {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

@media (min-width: 960px) {
  .product-ai-review__card--wide {
    grid-column: span 2;
  }
}

@media (max-width: 768px) {
  .product-ai-review__content,
  .product-ai-review__empty {
    padding: 1.35rem;
  }

  .product-ai-review__intro-card {
    grid-template-columns: 1fr;
  }

  .product-ai-review__intro-icon {
    width: 2.75rem;
    height: 2.75rem;
  }
}
</style>
