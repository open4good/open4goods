<template>
  <div class="share-target-page">
    <v-container class="py-8 py-md-12">
      <v-row justify="center">
        <v-col cols="12" md="10" lg="8">
          <v-sheet class="share-target-page__hero" rounded="xl" elevation="6">
            <div class="share-target-page__badge">{{ $t('share.landing.badge') }}</div>
            <h1 class="share-target-page__title">{{ pageTitle }}</h1>
            <p class="share-target-page__subtitle">
              {{ $t('share.landing.subtitle') }}
            </p>
            <div v-if="sharedUrl" class="share-target-page__origin">
              <v-icon size="18" color="accent-supporting" class="me-1">mdi-link</v-icon>
              <span class="share-target-page__origin-label">
                {{ $t('share.landing.originUrl', { url: sharedUrl }) }}
              </span>
            </div>
          </v-sheet>

          <v-alert
            v-if="isTimeout"
            type="warning"
            variant="tonal"
            class="mt-6"
            :text="$t('share.landing.timeout')"
            border="start"
          />

          <v-alert
            v-if="errorMessage"
            type="error"
            variant="tonal"
            class="mt-6"
            :text="errorMessage"
            border="start"
          />

          <v-skeleton-loader
            v-else-if="pending && !isTimeout"
            class="mt-6"
            type="card-avatar, article"
          />

          <div v-else-if="hasMultipleResults" class="share-target-page__grid mt-6">
            <div class="share-target-page__grid-header">
              <h2 class="text-h5 mb-1">{{ $t('share.selection.title') }}</h2>
              <p class="text-body-2 mb-0 text-medium-emphasis">
                {{ $t('share.selection.subtitle') }}
              </p>
            </div>
            <v-row dense>
              <v-col
                v-for="productItem in products"
                :key="productItem.gtin ?? productItem.slug ?? productItem.fullSlug"
                cols="12"
                md="6"
              >
                <v-card class="share-target-page__card" elevation="4">
                  <v-card-text>
                    <div class="share-target-page__pill share-target-page__pill--muted">
                      {{ originLabel }}
                    </div>
                    <div class="d-flex align-center mb-3">
                      <v-avatar size="64" class="me-3" color="surface-glass">
                        <v-img
                          v-if="productItem.resources?.images?.[0]?.url"
                          :src="productItem.resources.images[0]?.url"
                          :alt="productItem.identity?.bestName || productItem.base?.bestName"
                          cover
                        />
                        <v-icon v-else size="32" color="primary">mdi-image-outline</v-icon>
                      </v-avatar>
                      <div class="flex-grow-1">
                        <div class="share-target-page__product-title">
                          {{ productItem.identity?.bestName || productItem.base?.bestName || $t('share.card.untitled') }}
                        </div>
                        <p class="share-target-page__brand">
                          {{ productItem.identity?.brand || productItem.identity?.model }}
                        </p>
                      </div>
                    </div>

                    <div class="share-target-page__metrics mb-4">
                      <div class="share-target-page__metric">
                        <span class="share-target-page__metric-label">{{
                          $t('share.card.ecoScoreLabel')
                        }}</span>
                        <ImpactScore
                          v-if="resolveImpact(productItem) != null"
                          :score="resolveImpact(productItem)"
                          :show-value="true"
                        />
                        <span v-else class="share-target-page__metric-placeholder">{{
                          $t('share.card.missingEcoScore')
                        }}</span>
                      </div>

                      <div class="share-target-page__metric">
                        <span class="share-target-page__metric-label">{{
                          $t('share.card.bestPriceLabel')
                        }}</span>
                        <div
                          v-if="resolveProductPrice(productItem)"
                          class="share-target-page__price"
                        >
                          <v-icon size="18" class="me-2" color="accent-supporting">
                            mdi-tag-multiple
                          </v-icon>
                          <span>{{ resolveProductPrice(productItem) }}</span>
                        </div>
                        <span v-else class="share-target-page__metric-placeholder">{{
                          $t('share.card.missingPrice')
                        }}</span>
                      </div>
                    </div>

                    <div class="share-target-page__actions">
                      <v-btn
                        color="primary"
                        size="large"
                        :to="resolveProductLink(productItem)"
                        variant="flat"
                      >
                        {{ $t('share.card.viewProduct') }}
                      </v-btn>
                      <v-btn
                        color="secondary"
                        size="large"
                        variant="outlined"
                        :to="fallbackSearchLink"
                      >
                        {{ $t('share.card.searchMore') }}
                      </v-btn>
                    </div>
                  </v-card-text>
                </v-card>
              </v-col>
            </v-row>
          </div>

          <v-card v-else-if="resolution?.primary" class="share-target-page__card" elevation="8">
            <v-card-text>
              <v-row align="center" no-gutters class="share-target-page__card-body">
                <v-col cols="12" md="4" class="pe-md-6 mb-4 mb-md-0">
                  <v-sheet class="share-target-page__image-wrapper" rounded="lg" color="surface-glass">
                    <v-img
                      v-if="productImage"
                      :src="productImage"
                      :alt="productTitle"
                      cover
                      height="220"
                      class="rounded-lg"
                    />
                    <div v-else class="share-target-page__image-fallback">
                      <v-icon size="56" color="primary">mdi-image-search</v-icon>
                      <span>{{ $t('share.card.noImage') }}</span>
                    </div>
                  </v-sheet>
                </v-col>

                <v-col cols="12" md="8">
                  <div class="share-target-page__pill">{{ originLabel }}</div>
                  <h2 class="share-target-page__product-title">{{ productTitle }}</h2>
                  <p v-if="brandLabel" class="share-target-page__brand">{{ brandLabel }}</p>

                  <div class="share-target-page__metrics">
                    <div class="share-target-page__metric">
                      <span class="share-target-page__metric-label">{{
                        $t('share.card.ecoScoreLabel')
                      }}</span>
                      <ImpactScore
                        v-if="impactScore != null"
                        :score="impactScore"
                        :show-value="true"
                      />
                      <span v-else class="share-target-page__metric-placeholder">{{
                        $t('share.card.missingEcoScore')
                      }}</span>
                    </div>

                    <div class="share-target-page__metric">
                      <span class="share-target-page__metric-label">{{
                        $t('share.card.bestPriceLabel')
                      }}</span>
                      <div v-if="bestPrice" class="share-target-page__price">
                        <v-icon size="18" class="me-2" color="accent-supporting">mdi-tag-multiple</v-icon>
                        <span>{{ bestPrice }}</span>
                      </div>
                      <span v-else class="share-target-page__metric-placeholder">{{
                        $t('share.card.missingPrice')
                      }}</span>
                    </div>
                  </div>

                  <div class="share-target-page__actions">
                    <v-btn
                      color="primary"
                      size="large"
                      :to="productLink"
                      variant="flat"
                    >
                      {{ $t('share.card.viewProduct') }}
                    </v-btn>
                    <v-btn
                      color="secondary"
                      size="large"
                      variant="outlined"
                      :to="fallbackSearchLink"
                    >
                      {{ $t('share.card.searchMore') }}
                    </v-btn>
                  </div>
                </v-col>
              </v-row>
            </v-card-text>
          </v-card>

          <v-card v-else class="share-target-page__card" elevation="2">
            <v-card-text class="text-center">
              <v-icon size="52" color="primary" class="mb-2">mdi-magnify</v-icon>
              <h2 class="text-h5 mb-2">{{ $t('share.empty.title') }}</h2>
              <p class="text-body-1 mb-4">{{ emptyHelper }}</p>
              <v-btn color="primary" :to="fallbackSearchLink">{{
                $t('share.empty.cta')
              }}</v-btn>
            </v-card-text>
          </v-card>
        </v-col>
      </v-row>
    </v-container>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useSeoMeta } from '#imports'
import { useI18n } from 'vue-i18n'
import type { ProductDto } from '~~/shared/api-client'
import { extractGtinParam } from '~~/shared/utils/_gtin'
import { resolvePrimaryImpactScore } from '~/utils/_product-scores'
import { formatBestPrice } from '~/utils/_product-pricing'
import ImpactScore from '~/components/shared/ui/ImpactScore.vue'

definePageMeta({
  layout: 'default',
})

const route = useRoute()
const requestURL = useRequestURL()
const { t, n } = useI18n()

const gtinParam = computed(() =>
  extractGtinParam(typeof route.query.gtin === 'string' ? route.query.gtin : null)
)
const searchQuery = computed(() =>
  typeof route.query.q === 'string' && route.query.q.trim().length
    ? route.query.q.trim().slice(0, 160)
    : null
)
const sharedUrl = computed(() =>
  typeof route.query.url === 'string' && route.query.url.trim().length
    ? route.query.url.trim()
    : null
)
const sharedTitle = computed(() =>
  typeof route.query.title === 'string' ? route.query.title.trim() : null
)
const sharedText = computed(() =>
  typeof route.query.text === 'string' ? route.query.text.trim() : null
)

const resolutionKey = computed(
  () =>
    `share-resolution-${gtinParam.value ?? searchQuery.value ?? sharedUrl.value ?? 'empty'}`
)

const slaTimerMs = 4000
const timedOut = ref(false)
let timeoutHandle: ReturnType<typeof setTimeout> | null = null

const { data: resolution, pending, error } = await useAsyncData<{
  status: 'resolved' | 'empty' | 'error' | 'timeout'
  products: ProductDto[]
  primary: ProductDto | null
  origin: {
    gtin: string | null
    query: string | null
    url: string | null
    title: string | null
    text: string | null
  }
}>(() => resolutionKey.value, async () =>
  $fetch('/api/share/resolution', {
    query: {
      gtin: gtinParam.value ?? undefined,
      q: searchQuery.value ?? undefined,
      url: sharedUrl.value ?? undefined,
      title: sharedTitle.value ?? undefined,
      text: sharedText.value ?? undefined,
    },
  })
)

const startTimeout = () => {
  if (timeoutHandle) {
    clearTimeout(timeoutHandle)
  }
  timedOut.value = false
  timeoutHandle = setTimeout(() => {
    timedOut.value = true
  }, slaTimerMs)
}

onMounted(() => {
  startTimeout()
})

onBeforeUnmount(() => {
  if (timeoutHandle) {
    clearTimeout(timeoutHandle)
  }
})

watch(pending, value => {
  if (value) {
    startTimeout()
  } else if (timeoutHandle) {
    clearTimeout(timeoutHandle)
  }
})

watch(resolution, value => {
  if (value?.status === 'resolved') {
    timedOut.value = false
  }
})

const pageTitle = computed(() => t('share.landing.title'))
const productTitle = computed(() => {
  const entity = resolution.value?.primary
  if (!entity) {
    return t('share.card.untitled')
  }

  return (
    entity.identity?.bestName ||
    entity.base?.bestName ||
    entity.names?.h1Title ||
    entity.identity?.model ||
    entity.identity?.brand ||
    t('share.card.untitled')
  )
})

const brandLabel = computed(() => {
  const entity = resolution.value?.primary
  const brand = entity?.identity?.brand?.trim()
  const model = entity?.identity?.model?.trim()

  if (!brand && !model) {
    return ''
  }

  return [brand, model].filter(Boolean).join(' • ')
})

const impactScore = computed(() =>
  resolution.value?.primary ? resolvePrimaryImpactScore(resolution.value.primary) : null
)
const bestPrice = computed(() =>
  resolution.value?.primary ? formatBestPrice(resolution.value.primary, t, n) : null
)

const productImage = computed(() => {
  const images = resolution.value?.primary?.resources?.images ?? []
  return images.length ? images[0]?.url ?? null : null
})

const originLabel = computed(() =>
  gtinParam.value
    ? t('share.card.sourceGtin', { gtin: gtinParam.value })
    : resolution.value?.origin?.query
      ? t('share.card.sourceQuery', { query: resolution.value.origin.query })
      : sharedUrl.value
        ? t('share.card.sourceUrl', { url: sharedUrl.value })
        : t('share.card.sourceUnknown')
)

const fallbackSearchLink = computed(() =>
  resolution.value?.origin?.query
    ? `/search?q=${encodeURIComponent(resolution.value.origin.query)}`
    : '/search'
)

const productLink = computed(() => {
  const slug = resolution.value?.primary?.fullSlug || resolution.value?.primary?.slug
  return slug || fallbackSearchLink.value
})

const emptyHelper = computed(() =>
  resolution.value?.origin?.query
    ? t('share.empty.withQuery', { query: resolution.value.origin.query })
    : t('share.empty.helper')
)

const errorMessage = computed(() =>
  error.value ? t('share.errors.resolution') : null
)

const hasMultipleResults = computed(
  () => (resolution.value?.products?.length ?? 0) > 1
)
const products = computed(() => resolution.value?.products ?? [])
const isTimeout = computed(
  () => timedOut.value || resolution.value?.status === 'timeout'
)

const resolveProductLink = (product: ProductDto) =>
  product.fullSlug || product.slug || fallbackSearchLink.value

const resolveProductPrice = (product: ProductDto) => formatBestPrice(product, t, n)
const resolveImpact = (product: ProductDto) => resolvePrimaryImpactScore(product)

useSeoMeta({
  title: pageTitle,
  description: () => t('share.landing.description'),
  ogTitle: pageTitle,
  ogDescription: () => t('share.landing.description'),
  ogUrl: requestURL.href,
})

useHead({
  link: [
    {
      rel: 'canonical',
      href: `${requestURL.origin}${requestURL.pathname}${requestURL.search}`,
    },
  ],
})
</script>

<style scoped>
.share-target-page {
  background: radial-gradient(
      circle at 20% 20%,
      rgba(var(--v-theme-hero-gradient-start), 0.08),
      transparent 40%
    ),
    radial-gradient(
      circle at 80% 0%,
      rgba(var(--v-theme-hero-gradient-end), 0.08),
      transparent 35%
    ),
    rgb(var(--v-theme-surface-default));
}

.share-target-page__hero {
  background: linear-gradient(135deg, rgba(var(--v-theme-hero-gradient-start), 0.14), rgba(var(--v-theme-hero-gradient-end), 0.16));
  padding: 1.5rem;
  color: rgb(var(--v-theme-text-neutral-strong));
  position: relative;
  overflow: hidden;
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.5);
}

.share-target-page__badge {
  display: inline-flex;
  align-items: center;
  padding: 0.35rem 0.75rem;
  border-radius: 999px;
  background: rgba(var(--v-theme-hero-gradient-start), 0.16);
  color: rgb(var(--v-theme-text-neutral-strong));
  font-weight: 600;
  letter-spacing: 0.01em;
  margin-bottom: 0.5rem;
}

.share-target-page__title {
  font-size: clamp(1.6rem, 2vw + 1rem, 2.3rem);
  margin: 0;
  font-weight: 700;
}

.share-target-page__subtitle {
  margin: 0.4rem 0 0;
  color: rgb(var(--v-theme-text-neutral-secondary));
  font-size: 1.05rem;
}

.share-target-page__origin {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  margin-top: 0.75rem;
  padding: 0.45rem 0.75rem;
  border-radius: 10px;
  background: rgba(var(--v-theme-surface-glass), 0.6);
  color: rgb(var(--v-theme-text-neutral-secondary));
  font-weight: 600;
}

.share-target-page__origin-label {
  word-break: break-all;
}

.share-target-page__card {
  margin-top: 1.5rem;
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.5);
  background: linear-gradient(180deg, rgba(var(--v-theme-surface-glass), 0.9), rgba(var(--v-theme-surface-default), 0.98));
}

.share-target-page__card-body {
  gap: 1rem;
}

.share-target-page__image-wrapper {
  background: rgba(var(--v-theme-surface-glass), 0.6);
  border: 1px dashed rgba(var(--v-theme-border-primary-strong), 0.6);
  padding: 0.75rem;
}

.share-target-page__image-fallback {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  color: rgb(var(--v-theme-text-neutral-secondary));
  padding: 2rem 1rem;
}

.share-target-page__pill {
  display: inline-flex;
  padding: 0.35rem 0.75rem;
  border-radius: 12px;
  background: rgba(var(--v-theme-hero-gradient-end), 0.12);
  color: rgb(var(--v-theme-text-neutral-strong));
  font-weight: 600;
  margin-bottom: 0.5rem;
}

.share-target-page__product-title {
  margin: 0;
  font-size: 1.5rem;
  font-weight: 700;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.share-target-page__brand {
  margin: 0.35rem 0 0.75rem;
  color: rgb(var(--v-theme-text-neutral-secondary));
}

.share-target-page__metrics {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 0.75rem;
  padding: 0.75rem 0;
}

.share-target-page__metric-label {
  display: inline-block;
  font-weight: 600;
  color: rgb(var(--v-theme-text-neutral-secondary));
  margin-bottom: 0.35rem;
}

.share-target-page__metric-placeholder {
  color: rgb(var(--v-theme-text-neutral-soft));
}

.share-target-page__price {
  display: inline-flex;
  align-items: center;
  font-weight: 700;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.share-target-page__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  margin-top: 1rem;
}

.share-target-page__grid-header {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
  margin-bottom: 0.5rem;
}

.share-target-page__grid {
  display: flex;
  flex-direction: column;
}

.share-target-page__pill--muted {
  background: rgba(var(--v-theme-surface-glass), 0.8);
}

@media (max-width: 960px) {
  .share-target-page__hero {
    padding: 1.25rem;
  }

  .share-target-page__card-body {
    gap: 0.5rem;
  }
}
</style>
