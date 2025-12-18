<template>
  <v-card class="share-result" color="surface-default" variant="flat">
    <v-img
      v-if="candidate.image"
      :src="candidate.image"
      :alt="candidate.name"
      height="220"
      cover
      class="share-result__image"
    />

    <v-card-text class="share-result__content">
      <p class="share-result__eyebrow">{{ t('share.result.eyebrow') }}</p>
      <h2 class="share-result__title">{{ candidate.name }}</h2>

      <div class="share-result__scores">
        <ImpactScore
          v-if="candidate.impactScore != null"
          :score="candidate.impactScore"
          size="large"
          show-value
        />
        <ImpactScore
          v-else-if="candidate.ecoScore != null"
          :score="candidate.ecoScore"
          size="large"
          show-value
        />
      </div>

      <p v-if="candidate.bestPrice" class="share-result__price">
        {{ formatPrice(candidate.bestPrice) }}
        <span class="share-result__price-source">
          {{
            candidate.bestPrice.datasourceName ??
            t('share.result.unknownSource')
          }}
        </span>
      </p>

      <p class="share-result__helper">
        {{ t('share.result.helper') }}
      </p>

      <div class="share-result__actions">
        <NuxtLink :to="productLink" class="share-result__cta">
          <v-btn
            color="primary"
            variant="flat"
            block
            prepend-icon="mdi-open-in-new"
          >
            {{ t('share.result.viewProduct') }}
          </v-btn>
        </NuxtLink>
      </div>
    </v-card-text>
  </v-card>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { PropType } from 'vue'
import { useI18n } from 'vue-i18n'
import ImpactScore from '~/components/shared/ui/ImpactScore.vue'
import type { ShareCandidateDto } from '~~/shared/api-client'

const props = defineProps({
  candidate: {
    type: Object as PropType<ShareCandidateDto>,
    required: true,
  },
})

const { t } = useI18n()
const localePath = useLocalePath()

const productLink = computed(() => {
  const path = `/${props.candidate.productId}`
  return localePath({ path })
})

const formatPrice = (price?: ShareCandidateDto['bestPrice']) => {
  if (!price?.price && !price?.shortPrice) {
    return t('share.result.noPrice')
  }

  if (price.shortPrice) {
    return price.shortPrice
  }

  return new Intl.NumberFormat(undefined, {
    style: 'currency',
    currency: price.currency ?? 'EUR',
    maximumFractionDigits: 0,
  }).format(price.price ?? 0)
}
</script>

<style scoped>
.share-result {
  border-radius: 18px;
  overflow: hidden;
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.7);
}

.share-result__image {
  border-bottom: 1px solid rgba(var(--v-theme-border-primary-strong), 0.3);
}

.share-result__content {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.share-result__eyebrow {
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: rgb(var(--v-theme-text-neutral-soft));
  margin: 0;
  font-weight: 600;
}

.share-result__title {
  margin: 0;
  font-size: 1.35rem;
  font-weight: 800;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.share-result__scores {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.share-result__price {
  font-size: 1.2rem;
  font-weight: 700;
  margin: 0;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.share-result__price-source {
  display: block;
  font-size: 0.9rem;
  font-weight: 500;
  color: rgb(var(--v-theme-text-neutral-secondary));
}

.share-result__helper {
  margin: 0;
  color: rgb(var(--v-theme-text-neutral-secondary));
}

.share-result__actions {
  margin-top: 0.5rem;
}

.share-result__cta {
  text-decoration: none;
}
</style>
