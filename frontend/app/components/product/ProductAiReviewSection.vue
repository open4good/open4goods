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
      <p v-if="createdDate" class="product-ai-review__metadata">
        {{ $t('product.aiReview.generatedAt', { date: createdDate }) }}
      </p>

      <article class="product-ai-review__article">
        <h3 v-if="reviewContent.mediumTitle" class="product-ai-review__article-title">
          {{ reviewContent.mediumTitle }}
        </h3>
        <p v-if="reviewContent.shortDescription" class="product-ai-review__summary" itemprop="description">
          {{ reviewContent.shortDescription }}
        </p>

        <div v-if="reviewContent.summary" class="product-ai-review__block" itemprop="reviewBody">
          <h4>{{ $t('product.aiReview.sections.overall') }}</h4>
          <p>{{ reviewContent.summary }}</p>
        </div>

        <div v-if="reviewContent.description" class="product-ai-review__block">
          <h4>{{ $t('product.aiReview.sections.details') }}</h4>
          <!-- eslint-disable-next-line vue/no-v-html -->
          <div ref="descriptionRef" class="product-ai-review__richtext" v-html="reviewContent.description" />
        </div>

        <div v-if="reviewContent.technicalReview" class="product-ai-review__block">
          <h4>{{ $t('product.aiReview.sections.technical') }}</h4>
          <!-- eslint-disable-next-line vue/no-v-html -->
          <p v-html="reviewContent.technicalReview" />
        </div>

        <div v-if="reviewContent.ecologicalReview" class="product-ai-review__block">
          <h4>{{ $t('product.aiReview.sections.ecological') }}</h4>
          <!-- eslint-disable-next-line vue/no-v-html -->
          <p v-html="reviewContent.ecologicalReview" />
        </div>
      </article>

      <div class="product-ai-review__grid">
        <div v-if="reviewContent.pros?.length" class="product-ai-review__panel">
          <h4>{{ $t('product.aiReview.sections.pros') }}</h4>
          <ul>
            <!-- eslint-disable-next-line vue/no-v-html -->
            <li v-for="pro in reviewContent.pros" :key="pro" v-html="pro" />
          </ul>
        </div>
        <div v-if="reviewContent.cons?.length" class="product-ai-review__panel">
          <h4>{{ $t('product.aiReview.sections.cons') }}</h4>
          <ul>
            <!-- eslint-disable-next-line vue/no-v-html -->
            <li v-for="con in reviewContent.cons" :key="con" v-html="con" />
          </ul>
        </div>
      </div>

      <div v-if="reviewContent.attributes?.length" class="product-ai-review__attributes">
        <h4>{{ $t('product.aiReview.sections.identity') }}</h4>
        <v-table density="comfortable">
          <tbody>
            <tr v-for="attribute in reviewContent.attributes" :key="attribute.name">
              <th scope="row">{{ attribute.name }}</th>
              <td>{{ attribute.value }}</td>
            </tr>
          </tbody>
        </v-table>
      </div>

      <div v-if="reviewContent.sources?.length" class="product-ai-review__sources">
        <h4>{{ $t('product.aiReview.sections.sources') }}</h4>
        <v-table density="compact">
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
    status.value = response

    if (response.status === 'FAILED') {
      stopPolling()
      errorMessage.value = response.errorMessage ?? t('product.aiReview.errors.generic')
      return
    }

    if (response.status === 'SUCCESS' && response.result?.review) {
      stopPolling()
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
  color: rgba(var(--v-theme-text-neutral-soft), 0.9);
  font-size: 0.95rem;
}

.product-ai-review__content {
  background: rgba(var(--v-theme-surface-glass), 0.92);
  border-radius: 24px;
  padding: 1.75rem;
  box-shadow: inset 0 0 0 1px rgba(var(--v-theme-border-primary-strong), 0.08);
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.product-ai-review__article {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.product-ai-review__article-title {
  font-size: 1.3rem;
  font-weight: 600;
}

.product-ai-review__block h4 {
  margin-bottom: 0.5rem;
  font-size: 1.05rem;
}

.product-ai-review__richtext a {
  color: rgb(var(--v-theme-accent-primary-highlight));
  text-decoration: underline;
}

.product-ai-review__grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 1.5rem;
}

.product-ai-review__panel {
  background: rgba(var(--v-theme-surface-glass-strong), 0.95);
  border-radius: 18px;
  padding: 1.25rem;
  box-shadow: inset 0 0 0 1px rgba(var(--v-theme-border-primary-strong), 0.05);
}

.product-ai-review__panel h4 {
  margin-bottom: 0.75rem;
}

.product-ai-review__panel ul {
  margin: 0;
  padding-left: 1.1rem;
  display: grid;
  gap: 0.5rem;
}

.product-ai-review__attributes,
.product-ai-review__sources {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
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
  background: rgba(var(--v-theme-surface-glass), 0.9);
  border-radius: 24px;
  padding: 1.75rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
  align-items: flex-start;
}

.product-ai-review__empty-message {
  font-size: 1rem;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.9);
}

.product-ai-review__status {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

@media (max-width: 768px) {
  .product-ai-review__content,
  .product-ai-review__empty {
    padding: 1.25rem;
  }
}
</style>
