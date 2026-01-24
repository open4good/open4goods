<template>
  <section :id="sectionId" ref="rootRef" class="product-ai-review">
    <header class="product-ai-review__header">
      <v-row>
        <!-- Right Column: Title, Metadata, Summary -->
        <v-col cols="12">
          <h2 class="product-ai-review__title">
            {{ $t('product.aiReview.title', titleParams) }}
          </h2>

          <div class="product-ai-review__badge mb-2">
            <v-chip
              color="primary"
              variant="tonal"
              size="small"
              class="font-weight-bold"
            >
              <v-icon start icon="mdi-robot" size="16" />
              Synthèse IA
            </v-chip>
          </div>

          <div v-if="createdDate" class="product-ai-review__metadata mb-4">
            <v-icon
              icon="mdi-calendar-clock"
              size="16"
              class="product-ai-review__metadata-icon mr-1"
            />
            <span class="text-caption text-medium-emphasis">{{
              $t('product.aiReview.generatedAt', { date: createdDate })
            }}</span>
          </div>

          <p class="product-ai-review__subtitle mb-4">
            {{
              $t(
                sourcesCount === 1
                  ? 'product.aiReview.subtitle.one'
                  : 'product.aiReview.subtitle.other',
                { count: sourcesCount }
              )
            }}
          </p>

          <div v-if="reviewContent?.summary" class="product-ai-review__summary">
            <!-- eslint-disable vue/no-v-html -->
            <p class="text-body-1" v-html="reviewContent.summary" />
            <!-- eslint-enable vue/no-v-html -->
          </div>
        </v-col>
      </v-row>
    </header>

    <div
      v-if="reviewContent"
      class="product-ai-review__content"
      itemscope
      itemtype="https://schema.org/Review"
    >
      <div class="product-ai-review__grid">
        <!-- Row 1: Pros & Cons -->
        <v-row v-if="reviewContent.pros?.length || reviewContent.cons?.length">
          <v-col cols="12" md="6">
            <v-card
              v-if="reviewContent.pros?.length"
              class="product-ai-review__card h-100"
              elevation="0"
            >
              <v-card-text>
                <header
                  class="product-ai-review__sub-header d-flex align-center mb-3"
                >
                  <v-icon
                    icon="mdi-thumb-up-outline"
                    color="success"
                    class="mr-2"
                  />
                  <h4 class="text-subtitle-1 font-weight-bold">
                    {{ $t('product.aiReview.sections.pros') }}
                  </h4>
                </header>
                <ul class="product-ai-review__list">
                  <li
                    v-for="pro in reviewContent.pros"
                    :key="pro"
                    class="product-ai-review__list-item"
                  >
                    <v-icon
                      icon="mdi-check-circle-outline"
                      size="18"
                      color="success"
                      class="product-ai-review__list-icon mt-1"
                    />
                    <!-- eslint-disable vue/no-v-html -->
                    <span v-html="pro" />
                    <!-- eslint-enable vue/no-v-html -->
                  </li>
                </ul>
              </v-card-text>
            </v-card>
          </v-col>

          <v-col cols="12" md="6">
            <v-card
              v-if="reviewContent.cons?.length"
              class="product-ai-review__card h-100"
              elevation="0"
            >
              <v-card-text>
                <header
                  class="product-ai-review__sub-header d-flex align-center mb-3"
                >
                  <v-icon
                    icon="mdi-alert-circle-outline"
                    color="error"
                    class="mr-2"
                  />
                  <h4 class="text-subtitle-1 font-weight-bold">
                    {{ $t('product.aiReview.sections.cons') }}
                  </h4>
                </header>
                <ul class="product-ai-review__list">
                  <li
                    v-for="con in reviewContent.cons"
                    :key="con"
                    class="product-ai-review__list-item"
                  >
                    <v-icon
                      icon="mdi-close-circle-outline"
                      size="18"
                      color="error"
                      class="product-ai-review__list-icon mt-1"
                    />
                    <!-- eslint-disable vue/no-v-html -->
                    <span v-html="con" />
                    <!-- eslint-enable vue/no-v-html -->
                  </li>
                </ul>
              </v-card-text>
            </v-card>
          </v-col>
        </v-row>

        <!-- Technical Analysis -->
        <v-row v-if="technicalContent" id="ai-review-technical">
          <v-col cols="12">
            <v-card class="product-ai-review__card" elevation="0">
              <v-card-text>
                <header
                  class="product-ai-review__card-heading mb-4 d-flex align-center justify-space-between flex-wrap gap-4"
                >
                  <div
                    class="product-ai-review__card-header d-flex align-center"
                  >
                    <div class="product-ai-review__card-icon mr-3">
                      <v-icon icon="mdi-cog-outline" size="24" />
                    </div>
                    <h3 class="product-ai-review__card-title text-h6">
                      {{ $t('product.aiReview.sections.technical') }}
                    </h3>
                  </div>
                  <div class="product-ai-review__level-toggle-wrap">
                    <v-btn-toggle
                      v-model="technicalLevel"
                      mandatory
                      density="comfortable"
                      class="product-ai-review__level-toggle"
                      :aria-label="$t('product.aiReview.levels.label')"
                    >
                      <v-btn
                        v-for="option in levelOptions"
                        :key="option.value"
                        :value="option.value"
                        size="default"
                        :variant="
                          technicalLevel === option.value ? 'tonal' : 'outlined'
                        "
                        :color="
                          technicalLevel === option.value
                            ? 'primary'
                            : 'secondary'
                        "
                        :aria-label="option.label"
                        class="px-4"
                      >
                        <v-icon :icon="option.icon" size="20" class="mr-2" />
                        {{ option.label }}
                      </v-btn>
                    </v-btn-toggle>
                  </div>
                </header>
                <ProductAiReviewInsightBlock
                  :content-html="technicalContent ?? ''"
                  image-src="/images/product/ai-review-technical.svg"
                  :image-alt="$t('product.aiReview.illustrations.technicalAlt')"
                />
              </v-card-text>
            </v-card>
          </v-col>
        </v-row>

        <!-- Ecological Analysis -->
        <v-row v-if="ecologicalContent" id="ai-review-ecological">
          <v-col cols="12">
            <v-card class="product-ai-review__card" elevation="0">
              <v-card-text>
                <header
                  class="product-ai-review__card-heading mb-4 d-flex align-center justify-space-between flex-wrap gap-4"
                >
                  <div
                    class="product-ai-review__card-header d-flex align-center"
                  >
                    <div
                      class="product-ai-review__card-icon product-ai-review__card-icon--eco mr-3"
                    >
                      <v-icon icon="mdi-leaf" size="24" />
                    </div>
                    <h3 class="product-ai-review__card-title text-h6">
                      {{ $t('product.aiReview.sections.ecological') }}
                    </h3>
                  </div>
                  <div class="product-ai-review__level-toggle-wrap">
                    <v-btn-toggle
                      v-model="ecologicalLevel"
                      mandatory
                      density="comfortable"
                      class="product-ai-review__level-toggle"
                      :aria-label="$t('product.aiReview.levels.label')"
                    >
                      <v-btn
                        v-for="option in levelOptions"
                        :key="option.value"
                        :value="option.value"
                        size="default"
                        :variant="
                          ecologicalLevel === option.value
                            ? 'tonal'
                            : 'outlined'
                        "
                        :color="
                          ecologicalLevel === option.value
                            ? 'primary'
                            : 'secondary'
                        "
                        :aria-label="option.label"
                        class="px-4"
                      >
                        <v-icon :icon="option.icon" size="20" class="mr-2" />
                        {{ option.label }}
                      </v-btn>
                    </v-btn-toggle>
                  </div>
                </header>
                <ProductAiReviewInsightBlock
                  :content-html="ecologicalContent ?? ''"
                  image-src="/images/product/ai-review-ecological.svg"
                  :image-alt="
                    $t('product.aiReview.illustrations.ecologicalAlt')
                  "
                  image-position="right"
                />
              </v-card-text>
            </v-card>
          </v-col>
        </v-row>

        <!-- Community Analysis -->
        <v-row v-if="communityContent" id="ai-review-community">
          <v-col cols="12">
            <v-card class="product-ai-review__card" elevation="0">
              <v-card-text>
                <header
                  class="product-ai-review__card-heading mb-4 d-flex align-center justify-space-between flex-wrap gap-4"
                >
                  <div
                    class="product-ai-review__card-header d-flex align-center"
                  >
                    <div
                      class="product-ai-review__card-icon product-ai-review__card-icon--community mr-3"
                    >
                      <v-icon icon="mdi-account-group-outline" size="24" />
                    </div>
                    <h3 class="product-ai-review__card-title text-h6">
                      {{ $t('product.aiReview.sections.community') }}
                    </h3>
                  </div>
                  <div class="product-ai-review__level-toggle-wrap">
                    <v-btn-toggle
                      v-model="communityLevel"
                      mandatory
                      density="comfortable"
                      class="product-ai-review__level-toggle"
                      :aria-label="$t('product.aiReview.levels.label')"
                    >
                      <v-btn
                        v-for="option in levelOptions"
                        :key="option.value"
                        :value="option.value"
                        size="default"
                        :variant="
                          communityLevel === option.value ? 'tonal' : 'outlined'
                        "
                        :color="
                          communityLevel === option.value
                            ? 'primary'
                            : 'secondary'
                        "
                        :aria-label="option.label"
                        class="px-4"
                      >
                        <v-icon :icon="option.icon" size="20" class="mr-2" />
                        {{ option.label }}
                      </v-btn>
                    </v-btn-toggle>
                  </div>
                </header>
                <ProductAiReviewInsightBlock
                  :content-html="communityContent ?? ''"
                  image-src="/images/product/ai-review-community.svg"
                  :image-alt="$t('product.aiReview.illustrations.communityAlt')"
                />
              </v-card-text>
            </v-card>
          </v-col>
        </v-row>
      </div>

      <div
        v-if="reviewContent.sources?.length"
        class="product-ai-review__sources mt-4"
      >
        <v-row class="product-ai-review__sources-row">
          <v-col cols="12" :md="dataQualityValue ? 8 : 12">
            <header
              id="ai-review-sources-header"
              class="product-ai-review__section-header mb-2"
            >
              <div class="product-ai-review__section-icon">
                <v-icon icon="mdi-book-open-variant" size="20" />
              </div>
              <h4>{{ sourcesTitle }}</h4>
            </header>
            <v-table
              density="compact"
              class="product-ai-review__table product-ai-review__table--compact"
            >
              <thead>
                <tr>
                  <th scope="col">
                    {{ $t('product.aiReview.sources.index') }}
                  </th>
                  <th scope="col">
                    {{ $t('product.aiReview.sources.source') }}
                  </th>
                  <th scope="col">
                    {{ $t('product.aiReview.sources.description') }}
                  </th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="source in visibleSources"
                  :id="`review-ref-${source.number}`"
                  :key="source.number"
                >
                  <td>
                    <a
                      :href="source.url"
                      target="_blank"
                      rel="noopener noreferrer nofollow"
                    >
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
                      />
                      <span>{{ source.name }}</span>
                    </div>
                  </td>
                  <td>
                    <a
                      :href="source.url"
                      target="_blank"
                      rel="noopener noreferrer nofollow"
                    >
                      {{ source.description }}
                    </a>
                  </td>
                </tr>
                <tr
                  v-if="hasMoreSources"
                  class="product-ai-review__sources-toggle"
                >
                  <td :colspan="3">
                    <button
                      type="button"
                      class="product-ai-review__sources-toggle-btn"
                      @click="toggleSources"
                    >
                      <v-icon
                        :icon="
                          showAllSources ? 'mdi-chevron-up' : 'mdi-chevron-down'
                        "
                        size="18"
                      />
                      <span>{{ sourcesToggleLabel }}</span>
                    </button>
                  </td>
                </tr>
              </tbody>
            </v-table>
          </v-col>
          <v-col v-if="dataQualityValue" cols="12" md="4">
            <v-card class="product-ai-review__quality" elevation="0">
              <v-card-text>
                <header
                  class="product-ai-review__quality-header d-flex align-center mb-3"
                >
                  <v-icon
                    icon="mdi-shield-check-outline"
                    size="22"
                    class="mr-2"
                  />
                  <h3 class="text-subtitle-1 font-weight-bold">
                    {{ $t('product.aiReview.dataQuality.title') }}
                  </h3>
                </header>
                <p class="text-body-2 mb-4">
                  {{ $t('product.aiReview.dataQuality.description') }}
                </p>
                <div class="product-ai-review__quality-value">
                  {{ dataQualityValue }}
                </div>
              </v-card-text>
            </v-card>
          </v-col>
        </v-row>
      </div>
    </div>

    <div v-else class="product-ai-review__empty">
      <v-card class="product-ai-review__cta-card" elevation="0" border>
        <v-card-text class="d-flex align-center justify-space-between pa-6">
          <div class="d-flex align-center gap-4">
            <v-avatar color="primary" variant="tonal" size="56" class="mr-4">
              <v-icon icon="mdi-robot-outline" size="32" />
            </v-avatar>
            <div>
              <h3 class="text-h6 font-weight-bold mb-1">
                {{ $t('product.aiReview.request.callToAction.title') }}
              </h3>
              <p class="text-body-2 text-medium-emphasis">
                {{ $t('product.aiReview.request.callToAction.subtitle') }}
              </p>
            </div>
          </div>
          <v-btn
            color="primary"
            size="large"
            prepend-icon="mdi-sparkles"
            @click="isDialogOpen = true"
          >
            {{ $t('product.aiReview.requestButton') }}
          </v-btn>
        </v-card-text>
      </v-card>

      <v-dialog v-model="isDialogOpen" max-width="600" scrollable>
        <v-card class="product-ai-review__dialog-card">
          <v-card-title class="d-flex justify-space-between align-center pa-4">
            <span class="text-h6">{{
              $t('product.aiReview.request.title')
            }}</span>
            <v-btn
              icon="mdi-close"
              variant="text"
              density="comfortable"
              @click="isDialogOpen = false"
            />
          </v-card-title>
          <v-divider />
          <v-card-text class="pa-4">
            <ProductAiReviewRequestPanel
              v-model:agreement-accepted="agreementAccepted"
              :product-name="productLabel"
              :remaining-generations-label="remainingGenerationsLabel"
              :requires-captcha="requiresCaptcha"
              :site-key="siteKey"
              :captcha-theme="captchaTheme"
              :captcha-locale="captchaLocale"
              :requesting="requesting"
              :submit-disabled="submitDisabled"
              :error-message="errorMessage"
              :status-message="statusMessage"
              :is-generating="isGenerating"
              :status-percent="statusPercent"
              @submit="startRequest"
              @captcha-verify="handleCaptchaVerify"
              @captcha-expired="handleCaptchaExpired"
              @captcha-error="handleCaptchaError"
            />
          </v-card-text>
        </v-card>
      </v-dialog>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import type { PropType } from 'vue'
import { useI18n } from 'vue-i18n'
import { useTheme } from 'vuetify'
import { format } from 'date-fns'
import { fr, enUS } from 'date-fns/locale'
import DOMPurify from 'isomorphic-dompurify'
import type {
  AiReviewDto,
  AiReviewSourceDto,
  AiReviewAttributeDto,
  ReviewGenerationStatus,
} from '~~/shared/api-client'
import { IpQuotaCategory } from '~~/shared/api-client'
import { useAuth } from '~/composables/useAuth'
import { useIpQuota } from '~/composables/useIpQuota'

import ProductAiReviewInsightBlock from '~/components/product/ProductAiReviewInsightBlock.vue'
import ProductAiReviewRequestPanel from '~/components/product/ProductAiReviewRequestPanel.vue'

interface ReviewContent {
  description?: string | null
  shortDescription?: string | null
  mediumTitle?: string | null
  shortTitle?: string | null
  technicalReview?: string | null
  technicalReviewNovice?: string | null
  technicalReviewIntermediate?: string | null
  technicalReviewAdvanced?: string | null
  ecologicalReview?: string | null
  ecologicalReviewNovice?: string | null
  ecologicalReviewIntermediate?: string | null
  ecologicalReviewAdvanced?: string | null
  communityReviewNovice?: string | null
  communityReviewIntermediate?: string | null
  communityReviewAdvanced?: string | null
  summary?: string | null
  pros?: string[]
  cons?: string[]
  sources?: AiReviewSourceDto[]
  attributes?: AiReviewAttributeDto[]
  dataQuality?: string | null
}

type ReviewLevel = 'novice' | 'intermediate' | 'advanced'

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
  titleParams: {
    type: Object as PropType<Record<string, string> | undefined>,
    default: undefined,
  },
  productName: {
    type: String,
    default: '',
  },
  productImage: {
    type: String,
    default: null,
  },
})

const { locale, t, n } = useI18n()
const theme = useTheme()
const { isLoggedIn } = useAuth()
const { getRemaining, refreshQuota, recordUsage } = useIpQuota()

const review = ref<ReviewContent | null>(normalizeReview(props.initialReview))
const createdMs = ref<number | null>(props.reviewCreatedAt ?? null)
const requesting = ref(false)
const errorMessage = ref<string | null>(null)
const status = ref<ReviewGenerationStatus | null>(null)
const pollHandle = ref<number | null>(null)
const captchaToken = ref<string | null>(null)
const rootRef = ref<HTMLElement | null>(null)
const isDialogOpen = ref(false)
const agreementAccepted = ref(true)
const showAllSources = ref(false)
const technicalLevel = ref<ReviewLevel>('intermediate')
const ecologicalLevel = ref<ReviewLevel>('intermediate')
const communityLevel = ref<ReviewLevel>('intermediate')

const quotaCategory = IpQuotaCategory.ReviewGeneration

const remainingGenerations = computed(() => getRemaining(quotaCategory))
const remainingGenerationsLabel = computed(() => {
  if (
    remainingGenerations.value === null ||
    remainingGenerations.value === undefined
  ) {
    return t('product.aiReview.request.remainingUnknown')
  }

  return String(remainingGenerations.value)
})

const productLabel = computed(() =>
  props.productName.length > 0
    ? props.productName
    : t('product.aiReview.request.productFallback')
)

watch(
  () => props.initialReview,
  value => {
    review.value = normalizeReview(value)
    showAllSources.value = false
  }
)

watch(
  () => props.reviewCreatedAt,
  value => {
    createdMs.value = value ?? null
  }
)

const hasSiteKey = computed(() => props.siteKey.length > 0)
const requiresCaptcha = computed(() => !isLoggedIn.value)
const captchaTheme = computed(() =>
  theme.global.current.value.dark ? 'dark' : 'light'
)
const captchaLocale = computed(() =>
  locale.value.startsWith('fr') ? 'fr' : 'en'
)

const reviewContent = computed(() => review.value)
const sourcesCount = computed(() => reviewContent.value?.sources?.length ?? 0)
const dataQualityValue = computed(() => {
  const value = reviewContent.value?.dataQuality
  return typeof value === 'string' ? value.trim() : ''
})
const maxVisibleSources = 5
const visibleSources = computed(() => {
  const sources = reviewContent.value?.sources ?? []
  return showAllSources.value ? sources : sources.slice(0, maxVisibleSources)
})
const hasMoreSources = computed(() => sourcesCount.value > maxVisibleSources)
const sourcesTitle = computed(() =>
  t('product.aiReview.sections.sourcesCount', { count: n(sourcesCount.value) })
)
const sourcesToggleLabel = computed(() =>
  showAllSources.value
    ? t('product.aiReview.sources.showLess')
    : t('product.aiReview.sources.showMore')
)

const levelOptions = computed(() => [
  {
    value: 'novice',
    icon: 'mdi-signal-cellular-1',
    label: t('product.aiReview.levels.simple'),
  },
  {
    value: 'intermediate',
    icon: 'mdi-signal-cellular-2',
    label: t('product.aiReview.levels.intermediate'),
  },
  {
    value: 'advanced',
    icon: 'mdi-signal-cellular-3',
    label: t('product.aiReview.levels.advanced'),
  },
])

const createdDate = computed(() => {
  if (!createdMs.value) {
    return null
  }

  return format(createdMs.value, 'PPP', {
    locale: locale.value.startsWith('fr') ? fr : enUS,
  })
})

const isGenerating = computed(
  () =>
    status.value?.status &&
    status.value.status !== 'SUCCESS' &&
    status.value.status !== 'FAILED'
)
const statusPercent = computed(() => Math.round(status.value?.percent ?? 0))
const submitDisabled = computed(() => {
  if (!agreementAccepted.value || requesting.value) {
    return true
  }

  if (requiresCaptcha.value) {
    return !hasSiteKey.value
  }

  return false
})

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

const technicalContent = computed(() =>
  selectReviewContent(technicalLevel.value, {
    novice: reviewContent.value?.technicalReviewNovice,
    intermediate: reviewContent.value?.technicalReviewIntermediate,
    advanced: reviewContent.value?.technicalReviewAdvanced,
    legacy: reviewContent.value?.technicalReview,
  })
)

const ecologicalContent = computed(() =>
  selectReviewContent(ecologicalLevel.value, {
    novice: reviewContent.value?.ecologicalReviewNovice,
    intermediate: reviewContent.value?.ecologicalReviewIntermediate,
    advanced: reviewContent.value?.ecologicalReviewAdvanced,
    legacy: reviewContent.value?.ecologicalReview,
  })
)

const communityContent = computed(() =>
  selectReviewContent(communityLevel.value, {
    novice: reviewContent.value?.communityReviewNovice,
    intermediate: reviewContent.value?.communityReviewIntermediate,
    advanced: reviewContent.value?.communityReviewAdvanced,
  })
)

function sanitizeHtml(content: string | null): string | null {
  if (!content) {
    return null
  }

  const sanitized = DOMPurify.sanitize(content, {
    ADD_ATTR: ['target', 'rel', 'class'],
  })

  // Ensure we don't return empty HTML tags or whitespace
  const textContent = sanitized.replace(/<[^>]*>/g, '').trim()
  if (textContent.length === 0) {
    return null
  }

  return sanitized
}

function selectReviewContent(
  level: ReviewLevel,
  content: {
    novice?: string | null
    intermediate?: string | null
    advanced?: string | null
    legacy?: string | null
  }
): string | null {
  const selected =
    level === 'novice'
      ? content.novice
      : level === 'advanced'
        ? content.advanced
        : content.intermediate

  if (selected) {
    return selected
  }

  if (level === 'intermediate' && content.legacy) {
    return content.legacy
  }

  return (
    content.intermediate ??
    content.legacy ??
    content.novice ??
    content.advanced ??
    null
  )
}

function transformReferences(content: string | null): string | null {
  if (!content) {
    return null
  }

  // Regex to match [1] or [1, 2] or [1, 2, 3] etc.
  // It captures the numbers part inside the brackets
  return content.replace(/\[(\d+(?:,\s*\d+)*)\]/g, (_match, numbers) => {
    // Split the numbers by comma
    const refs = (numbers as string).split(',').map((n: string) => {
      const num = n.trim()
      // Create a link for each number
      // We use data-source-id to help with event handling if needed,
      // and href to allow standard navigation/hover behavior
      return `<a href="#review-ref-${num}" class="review-ref" data-source-id="${num}">${num}</a>`
    })

    // Reconstruct the string with links inside brackets
    return `[${refs.join(', ')}]`
  })
}

function normalizeReview(reviewData: AiReviewDto | null): ReviewContent | null {
  if (!reviewData) {
    return null
  }

  const sanitizeAndTransform = (text: string | null | undefined) => {
    return transformReferences(sanitizeHtml(text ?? null))
  }

  const pros = Array.isArray(reviewData.pros)
    ? reviewData.pros
        .map(entry => sanitizeAndTransform(String(entry)))
        .filter(
          (entry): entry is string =>
            typeof entry === 'string' && entry.length > 0
        )
    : []
  const cons = Array.isArray(reviewData.cons)
    ? reviewData.cons
        .map(entry => sanitizeAndTransform(String(entry)))
        .filter(
          (entry): entry is string =>
            typeof entry === 'string' && entry.length > 0
        )
    : []

  const sources: AiReviewSourceDto[] = (reviewData.sources ?? [])
    .map(source => ({
      number: source.number ?? undefined,
      name: source.name ?? undefined,
      description: source.description ?? undefined,
      url: source.url ?? undefined,
      favicon: source.favicon ?? undefined,
    }))
    .filter(source => Boolean(source.url))

  const rawAttributes = reviewData.attributes
  const attributesList = Array.isArray(rawAttributes)
    ? rawAttributes
    : typeof rawAttributes === 'object' && rawAttributes !== null
      ? Object.values(rawAttributes)
      : []

  const attributes: AiReviewAttributeDto[] = attributesList
    .map((attribute: unknown) => {
      const attr = attribute as AiReviewAttributeDto
      return {
        name: attr?.name ?? '',
        value: attr?.value ?? '',
        number: attr?.number ?? undefined,
      }
    })
    .filter(attribute => attribute.name.length > 0)

  return {
    description: sanitizeAndTransform(reviewData.description),
    shortDescription: reviewData.shortDescription ?? null,
    mediumTitle: reviewData.mediumTitle ?? null,
    shortTitle: reviewData.shortTitle ?? null,
    technicalReview: sanitizeAndTransform(reviewData.technicalReview),
    technicalReviewNovice: sanitizeAndTransform(
      reviewData.technicalReviewNovice
    ),
    technicalReviewIntermediate: sanitizeAndTransform(
      reviewData.technicalReviewIntermediate
    ),
    technicalReviewAdvanced: sanitizeAndTransform(
      reviewData.technicalReviewAdvanced
    ),
    ecologicalReview: sanitizeAndTransform(reviewData.ecologicalReview),
    ecologicalReviewNovice: sanitizeAndTransform(
      reviewData.ecologicalReviewNovice
    ),
    ecologicalReviewIntermediate: sanitizeAndTransform(
      reviewData.ecologicalReviewIntermediate
    ),
    ecologicalReviewAdvanced: sanitizeAndTransform(
      reviewData.ecologicalReviewAdvanced
    ),
    communityReviewNovice: sanitizeAndTransform(
      reviewData.communityReviewNovice
    ),
    communityReviewIntermediate: sanitizeAndTransform(
      reviewData.communityReviewIntermediate
    ),
    communityReviewAdvanced: sanitizeAndTransform(
      reviewData.communityReviewAdvanced
    ),
    summary: sanitizeAndTransform(reviewData.summary),
    pros,
    cons,
    sources,
    attributes,
    dataQuality: reviewData.dataQuality ?? null,
  }
}

const toggleSources = () => {
  showAllSources.value = !showAllSources.value
}

const startRequest = () => {
  errorMessage.value = null
  void triggerGeneration()
}

const handleCaptchaVerify = async (token: string) => {
  captchaToken.value = token
}

const handleCaptchaExpired = () => {
  captchaToken.value = null
}

const handleCaptchaError = () => {
  captchaToken.value = null
  errorMessage.value = t('product.aiReview.errors.captcha')
}

const triggerGeneration = async () => {
  if (requiresCaptcha.value && !captchaToken.value) {
    errorMessage.value = t('product.aiReview.errors.captcha')
    return
  }

  requesting.value = true
  errorMessage.value = null

  try {
    await $fetch(`/api/products/${props.gtin}/review`, {
      method: 'POST',
      body: {
        hcaptchaResponse: captchaToken.value ?? undefined,
      },
    })

    recordUsage(quotaCategory)
    startPolling()
  } catch (error) {
    console.error('Failed to trigger AI review', error)
    errorMessage.value =
      error instanceof Error
        ? error.message
        : t('product.aiReview.errors.generic')
  } finally {
    requesting.value = false
  }
}

const pollStatus = async () => {
  try {
    const response = await $fetch<ReviewGenerationStatus>(
      `/api/products/${props.gtin}/review`
    )
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
          : (response.errorMessage ?? t('product.aiReview.errors.generic'))
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
    errorMessage.value =
      error instanceof Error
        ? error.message
        : t('product.aiReview.errors.generic')
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
  rootRef,
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
  { immediate: true }
)

/*
const openDialog = () => {
  errorMessage.value = null
  captchaToken.value = null
  agreementAccepted.value = true
  isDialogOpen.value = true
}
*/

onMounted(() => {
  if (import.meta.client) {
    void refreshQuota(quotaCategory)
    window.addEventListener(
      'ai-review:scroll-to-source',
      handleGlobalScrollEvent as EventListener
    )
  }
})

onBeforeUnmount(() => {
  stopPolling()
  if (import.meta.client) {
    window.removeEventListener(
      'ai-review:scroll-to-source',
      handleGlobalScrollEvent as EventListener
    )
  }
})

/*
watch(
  isDialogOpen,
  value => {
    if (value && import.meta.client) {
      void refreshQuota(quotaCategory)
      return
    }

    if (!value) {
      captchaToken.value = null
    }
  },
  { immediate: true }
)
*/

const handleReferenceClick = async (event: Event) => {
  const target = event.target as HTMLElement | null
  if (!target) {
    return
  }

  const anchor = target.closest('.review-ref') as HTMLAnchorElement | null
  if (anchor?.hash) {
    event.preventDefault()
    const id = anchor.hash.replace('#', '')
    await scrollToSourceId(id)
  }
}

const scrollToSourceId = async (id: string) => {
  // 1. Ensure the sources table is fully visible
  if (!showAllSources.value) {
    showAllSources.value = true
    await nextTick()
  }

  // 2. Scroll to the section header "Sources"
  const headerElement = document.getElementById('ai-review-sources-header')
  if (headerElement) {
    scrollToElement(headerElement)
  }

  // 3. Highlight the specific row if it's a reference navigation
  if (id.startsWith('review-ref-')) {
    // Small delay to ensure smooth scroll to header starts first/finishes mainly
    // or just execute immediately, the eye will see it.
    // We already waited for nextTick if we expanded sources.
    const element = document.getElementById(id)
    if (element) {
      // We do NOT call scrollToElement here because we want to show the whole table context
      // starting from the header. We just highlight the row.
      element.classList.add('review-ref--highlight')
      setTimeout(() => {
        element.classList.remove('review-ref--highlight')
      }, 2000)
    }
  }
}

const scrollToElement = (element: HTMLElement) => {
  const headerOffset = 120 // Adjust as needed for sticky header
  const bodyRect = document.body.getBoundingClientRect().top
  const elementRect = element.getBoundingClientRect().top
  const elementPosition = elementRect - bodyRect
  const offsetPosition = elementPosition - headerOffset

  window.scrollTo({
    top: offsetPosition,
    behavior: 'smooth',
  })

  // Highlight effect
  element.classList.add('review-ref--highlight')
  setTimeout(() => {
    element.classList.remove('review-ref--highlight')
  }, 2000)
}

const handleGlobalScrollEvent = (event: Event) => {
  const customEvent = event as CustomEvent<{ id: string }>
  if (customEvent.detail?.id) {
    // Remove # if present
    const id = customEvent.detail.id.replace('#', '')
    void scrollToSourceId(id)
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
  box-shadow:
    inset 0 0 0 1px rgba(var(--v-theme-border-primary-strong), 0.08),
    0 18px 40px -24px rgba(var(--v-theme-shadow-primary-600), 0.4);
  display: flex;
  flex-direction: column;
  gap: 1.75rem;
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

.product-ai-review__card-header {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
}

.product-ai-review__card-heading {
  display: flex;
  align-items: flex-start;
  flex-direction: column;
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

.product-ai-review__card-icon--eco {
  background: rgba(var(--v-theme-accent-supporting), 0.18);
  color: rgb(var(--v-theme-accent-supporting));
}

.product-ai-review__card-icon--community {
  background: rgba(var(--v-theme-accent-primary-highlight), 0.15);
  color: rgb(var(--v-theme-accent-primary-highlight));
}

.product-ai-review__card-title {
  font-size: 1.05rem;
  font-weight: 600;
  margin: 0;
}

.product-ai-review__level-toggle-wrap {
  width: 100%;
  display: flex;
  justify-content: center;
}

.product-ai-review__level-toggle {
  border-radius: 999px;
  padding: 0.2rem;
  gap: 0.35rem;
}

.product-ai-review__level-toggle :deep(.v-btn) {
  min-width: 2.25rem;
}

.product-ai-review__card-text {
  margin: 0;
  line-height: 1.65;
  font-size: 1.2em;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.product-ai-review__card-text :deep(.review-ref) {
  display: inline-flex;
  align-items: center;
  gap: 0.2rem;
  padding: 0.1rem 0.35rem;
  border-radius: 6px;
  background: rgba(var(--v-theme-surface-primary-080), 0.9);
  color: rgb(var(--v-theme-accent-primary-highlight));
  font-size: 1.05em;
  font-weight: 600;
  text-decoration: none;
}

.product-ai-review__card-text :deep(.review-ref:hover),
.product-ai-review__card-text :deep(.review-ref:focus-visible) {
  background: rgba(var(--v-theme-surface-primary-100), 0.95);
  color: rgb(var(--v-theme-accent-primary-highlight));
  box-shadow: 0 4px 10px rgba(var(--v-theme-shadow-primary-600), 0.18);
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

.product-ai-review__list-item span {
  font-size: 1.2em;
}

.product-ai-review__list-icon {
  margin-top: 0.2rem;
}

.product-ai-review__sources {
  display: flex;
  flex-direction: column;
  gap: 0.9rem;
}

.product-ai-review__sources-row {
  margin: 0;
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

.product-ai-review__sources-toggle td {
  padding: 0;
}

.product-ai-review__sources-toggle-btn {
  width: 100%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.4rem;
  padding: 0.75rem;
  border: none;
  background: rgba(var(--v-theme-surface-primary-050), 0.7);
  color: rgb(var(--v-theme-text-neutral-strong));
  font-weight: 600;
  cursor: pointer;
}

.product-ai-review__sources-toggle-btn:hover,
.product-ai-review__sources-toggle-btn:focus-visible {
  background: rgba(var(--v-theme-surface-primary-080), 0.9);
}

.product-ai-review__source-info {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
}

.product-ai-review__source-favicon {
  border-radius: 4px;
}

.product-ai-review__quality {
  border-radius: 20px;
  background: rgba(var(--v-theme-surface-glass-strong), 0.94);
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.35);
  box-shadow: 0 16px 32px rgba(15, 23, 42, 0.12);
}

.product-ai-review__quality-header {
  color: rgb(var(--v-theme-text-neutral-strong));
}

.product-ai-review__quality-value {
  padding: 0.75rem 1rem;
  border-radius: 999px;
  background: rgba(var(--v-theme-surface-primary-080), 0.95);
  color: rgb(var(--v-theme-text-neutral-strong));
  font-weight: 600;
  width: fit-content;
}

.product-ai-review__empty {
  background: rgba(var(--v-theme-surface-glass), 0.92);
  border-radius: 24px;
  padding: 1.75rem;
  display: flex;
  flex-direction: column;
  gap: 1.1rem;
  box-shadow: inset 0 0 0 1px rgba(var(--v-theme-border-primary-strong), 0.08);
}

.product-ai-review__empty-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1.5rem;
  flex-wrap: wrap;
}

.product-ai-review__empty-content {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  max-width: 32rem;
}

.product-ai-review__empty-label {
  margin: 0;
  font-size: 0.95rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: rgba(var(--v-theme-text-neutral-soft), 0.9);
}

.product-ai-review__empty-message {
  margin: 0;
  font-size: 1rem;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.92);
  line-height: 1.55;
}

.product-ai-review__empty-cta {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 0.75rem;
}

.product-ai-review__quota {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.35rem 0.8rem;
  border-radius: 999px;
  background: rgba(var(--v-theme-surface-primary-080), 0.8);
  color: rgb(var(--v-theme-text-neutral-strong));
  font-size: 0.85rem;
}

.product-ai-review__quota-label {
  color: rgba(var(--v-theme-text-neutral-soft), 0.9);
}

.product-ai-review__quota-value {
  font-weight: 700;
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
    padding: 1.35rem;
  }

  .product-ai-review__empty-banner {
    flex-direction: column;
    align-items: flex-start;
  }

  .product-ai-review__empty-cta {
    width: 100%;
    align-items: stretch;
  }
}
</style>
