<template>
  <section
    class="product-hero"
    itemscope
    itemtype="https://schema.org/Product"
  >
    <meta itemprop="sku" :content="String(product.base?.gtin ?? '')" />
    <meta itemprop="brand" :content="product.identity?.brand ?? ''" />

    <div class="product-hero__gallery">
      <ClientOnly>
        <component
          :is="lightGalleryComponent"
          v-if="lightGalleryComponent && galleryItems.length"
          class="product-hero__lightgallery"
          :plugins="lightGalleryPlugins"
          :settings="gallerySettings"
        >
          <a
            v-for="item in galleryItems"
            :key="item.id"
            class="product-hero__gallery-item"
            :href="item.url"
            :data-src="item.url"
            :data-lg-size="item.size"
            :data-sub-html="item.caption"
          >
            <img
              :src="item.preview"
              :alt="item.alt"
              loading="lazy"
            >
          </a>
        </component>
        <template #fallback>
          <NuxtImg
            v-if="coverImage"
            :src="coverImage"
            :alt="title"
            class="product-hero__fallback"
            format="webp"
            :width="600"
            :height="600"
          />
        </template>
      </ClientOnly>
    </div>

    <div class="product-hero__details">
      <p v-if="product.identity?.brand" class="product-hero__eyebrow">
        {{ product.identity.brand }}
      </p>
      <h1 class="product-hero__title" itemprop="name">
        {{ title }}
      </h1>
      <p v-if="product.identity?.model" class="product-hero__subtitle">
        {{ product.identity.model }}
      </p>

      <div class="product-hero__meta">
        <ImpactScore
          v-if="impactScore !== null"
          :score="impactScore"
          :max="5"
          size="large"
          :show-value="true"
          class="product-hero__impact"
        />
        <div v-if="product.base?.gtinInfo" class="product-hero__origin">
          <NuxtImg
            v-if="product.base.gtinInfo.countryFlagUrl"
            :src="product.base.gtinInfo.countryFlagUrl"
            :alt="product.base.gtinInfo.countryName ?? ''"
            width="32"
            height="24"
            class="product-hero__flag"
          />
          <span>
            {{ product.base.gtinInfo.countryName }}
          </span>
        </div>
      </div>

      <div class="product-hero__facts">
        <div class="product-hero__fact">
          <span class="product-hero__fact-label">{{ $t('product.hero.gtin') }}</span>
          <span class="product-hero__fact-value" itemprop="gtin13">
            {{ product.gtin }}
          </span>
        </div>
        <div class="product-hero__fact">
          <span class="product-hero__fact-label">{{ $t('product.hero.offersCount') }}</span>
          <span class="product-hero__fact-value">
            {{ offersCountLabel }}
          </span>
        </div>
      </div>
    </div>

    <aside class="product-hero__pricing">
      <div class="product-hero__pricing-card" itemprop="offers" itemscope itemtype="https://schema.org/AggregateOffer">
        <meta itemprop="offerCount" :content="String(product.offers?.offersCount ?? 0)" />
        <meta itemprop="priceCurrency" :content="product.offers?.bestPrice?.currency ?? 'EUR'" />
        <h2 class="product-hero__pricing-title">
          {{ $t('product.hero.bestPriceTitle') }}
        </h2>
        <div class="product-hero__price">
          <span class="product-hero__price-value" itemprop="lowPrice">
            {{ bestPriceLabel }}
          </span>
          <span class="product-hero__price-currency">
            {{ product.offers?.bestPrice?.currency ?? '€' }}
          </span>
        </div>
        <p v-if="product.offers?.bestPrice?.datasourceName" class="product-hero__price-source">
          {{ $t('product.hero.priceFrom', { source: product.offers.bestPrice.datasourceName }) }}
        </p>
        <div v-if="product.offers?.bestPrice?.compensation" class="product-hero__price-meta">
          <v-chip
            size="small"
            color="success"
            variant="tonal"
            class="product-hero__price-chip"
          >
            {{ $t('product.hero.compensation', { amount: compensationLabel }) }}
          </v-chip>
        </div>
        <div class="product-hero__price-actions">
          <v-btn
            color="primary"
            :href="product.offers?.bestPrice?.url ?? '#prix'"
            :target="product.offers?.bestPrice?.url ? '_blank' : undefined"
            rel="nofollow noopener"
            variant="flat"
          >
            {{ $t('product.hero.viewOffers') }}
          </v-btn>
        </div>
      </div>
    </aside>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, type Component, type PropType } from 'vue'
import { useI18n } from 'vue-i18n'
import ImpactScore from '~/components/shared/ui/ImpactScore.vue'
import type { ProductDto } from '~~/shared/api-client'

const props = defineProps({
  product: {
    type: Object as PropType<ProductDto>,
    required: true,
  },
})

const lightGalleryComponent = ref<Component | null>(null)
const lightGalleryPlugins = ref<unknown[]>([])

const { n } = useI18n()

const gallerySettings = {
  selector: '.product-hero__gallery-item',
  download: false,
  speed: 400,
  mobileSettings: {
    controls: true,
    showCloseIcon: true,
    download: false,
  },
}

const coverImage = computed(() => props.product.resources?.coverImagePath ?? props.product.resources?.externalCover ?? props.product.base?.coverImagePath ?? null)

const title = computed(() => props.product.names?.h1Title ?? props.product.identity?.bestName ?? props.product.base?.bestName ?? '')

const bestPrice = computed(() => props.product.offers?.bestPrice ?? null)

const bestPriceLabel = computed(() => {
  if (!bestPrice.value?.price) {
    return '—'
  }

  return n(bestPrice.value.price, {
    style: 'currency',
    currency: bestPrice.value.currency ?? 'EUR',
    maximumFractionDigits: 0,
  })
})

const compensationLabel = computed(() => {
  if (!bestPrice.value?.compensation) {
    return null
  }

  return n(bestPrice.value.compensation, {
    style: 'currency',
    currency: bestPrice.value.currency ?? 'EUR',
    maximumFractionDigits: 2,
  })
})

const offersCountLabel = computed(() => {
  const count = props.product.offers?.offersCount ?? 0
  return n(count)
})

const impactScore = computed(() => {
  const ecoscore = props.product.base?.ecoscoreValue
  if (typeof ecoscore === 'number') {
    return ecoscore
  }

  const relative = props.product.scores?.ecoscore?.relativeValue
  return typeof relative === 'number' ? relative : null
})

interface GalleryItem {
  id: string
  url: string
  preview: string
  alt: string
  caption: string
  size: string
}

const galleryItems = computed<GalleryItem[]>(() => {
  const images = props.product.resources?.images ?? []
  const videos = props.product.resources?.videos ?? []

  const imageItems = images.map((image) => ({
    id: `image-${image.cacheKey ?? image.url ?? Math.random()}`,
    url: image.originalUrl ?? image.url ?? '',
    preview: image.url ?? image.originalUrl ?? '',
    alt: image.fileName ?? title.value,
    caption: image.datasourceName ?? title.value,
    size: image.width && image.height ? `${image.width}-${image.height}` : 'auto',
  }))

  const videoItems = videos.map((video) => ({
    id: `video-${video.cacheKey ?? video.url ?? Math.random()}`,
    url: video.url ?? '',
    preview: coverImage.value ?? props.product.resources?.images?.[0]?.url ?? '',
    alt: video.fileName ?? title.value,
    caption: video.datasourceName ?? title.value,
    size: '1280-720',
  }))

  return [...imageItems, ...videoItems].filter((item) => Boolean(item.url))
})

onMounted(async () => {
  if (!import.meta.client) {
    return
  }

  try {
    await Promise.all([
      import('lightgallery/css/lightgallery.css'),
      import('lightgallery/css/lg-zoom.css'),
      import('lightgallery/css/lg-thumbnail.css'),
    ])

    const [{ default: LightGallery }, { default: lgZoom }, { default: lgThumbnail }] = await Promise.all([
      import('lightgallery/vue'),
      import('lightgallery/plugins/zoom'),
      import('lightgallery/plugins/thumbnail'),
    ])

    lightGalleryComponent.value = LightGallery
    lightGalleryPlugins.value = [lgZoom, lgThumbnail]
  } catch (error) {
    console.error('Failed to load lightgallery', error)
  }
})
</script>

<style scoped>
.product-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(0, 1fr) minmax(0, 0.8fr);
  gap: 2.5rem;
  padding: 2.5rem;
  border-radius: 32px;
  background: linear-gradient(135deg, rgba(var(--v-theme-surface-ice-050), 0.9), rgba(var(--v-theme-surface-glass), 0.95));
  box-shadow: 0 32px 70px rgba(15, 23, 42, 0.08);
}

.product-hero__gallery {
  border-radius: 24px;
  overflow: hidden;
  position: relative;
  min-height: 420px;
  background: rgba(15, 23, 42, 0.02);
}

.product-hero__gallery-item img,
.product-hero__fallback {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.product-hero__details {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.product-hero__eyebrow {
  text-transform: uppercase;
  letter-spacing: 0.1em;
  font-weight: 600;
  font-size: 0.85rem;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.8);
}

.product-hero__title {
  font-size: clamp(2rem, 2.8vw, 3rem);
  font-weight: 700;
  line-height: 1.1;
  margin: 0;
}

.product-hero__subtitle {
  font-size: 1.1rem;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.9);
}

.product-hero__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
  align-items: center;
}

.product-hero__impact {
  display: inline-flex;
}

.product-hero__origin {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  font-weight: 600;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.product-hero__flag {
  border-radius: 4px;
  width: 32px;
  height: 24px;
  object-fit: cover;
}

.product-hero__facts {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 1rem;
  margin-top: 0.5rem;
}

.product-hero__fact {
  background: rgba(var(--v-theme-surface-glass-strong), 0.6);
  border-radius: 14px;
  padding: 0.875rem 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.product-hero__fact-label {
  font-size: 0.75rem;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: rgba(var(--v-theme-text-neutral-soft), 0.9);
}

.product-hero__fact-value {
  font-weight: 700;
  font-size: 1rem;
}

.product-hero__pricing {
  align-self: stretch;
}

.product-hero__pricing-card {
  border-radius: 24px;
  background: rgba(var(--v-theme-surface-glass-strong), 0.9);
  padding: 1.75rem;
  box-shadow: inset 0 0 0 1px rgba(var(--v-theme-border-primary-strong), 0.1);
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.product-hero__pricing-title {
  margin: 0;
  font-size: 1.2rem;
  font-weight: 700;
}

.product-hero__price {
  display: flex;
  align-items: baseline;
  gap: 0.5rem;
}

.product-hero__price-value {
  font-size: clamp(2rem, 3.4vw, 2.6rem);
  font-weight: 700;
}

.product-hero__price-currency {
  font-size: 1.1rem;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.8);
}

.product-hero__price-source {
  font-size: 0.95rem;
  color: rgba(var(--v-theme-text-neutral-soft), 0.9);
}

.product-hero__price-actions {
  margin-top: 0.5rem;
}

@media (max-width: 1280px) {
  .product-hero {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    grid-template-rows: auto auto;
  }

  .product-hero__pricing {
    grid-column: 1 / -1;
  }
}

@media (max-width: 960px) {
  .product-hero {
    grid-template-columns: 1fr;
    padding: 1.5rem;
    gap: 1.5rem;
  }

  .product-hero__gallery {
    min-height: 280px;
  }
}
</style>
