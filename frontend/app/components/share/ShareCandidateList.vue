<template>
  <v-card class="share-candidates" color="surface-glass-strong" variant="flat">
    <v-card-title class="share-candidates__title">
      {{ t('share.selection.title') }}
    </v-card-title>
    <v-card-subtitle class="share-candidates__subtitle">
      {{ t('share.selection.subtitle') }}
    </v-card-subtitle>

    <v-divider class="my-2" />

    <v-list density="comfortable" lines="two">
      <v-list-item
        v-for="candidate in candidates"
        :key="candidate.productId"
        :active="candidate.productId === selectedProductId"
        color="primary"
        class="share-candidates__item"
        @click="$emit('select', candidate)"
      >
        <template #prepend>
          <v-avatar color="surface-glass" size="56" rounded="lg">
            <v-img
              v-if="candidate.image"
              :src="candidate.image"
              :alt="candidate.name"
              cover
            />
            <v-icon v-else icon="mdi-image-off-outline" />
          </v-avatar>
        </template>

        <v-list-item-title class="share-candidates__name">
          {{ candidate.name }}
        </v-list-item-title>
        <v-list-item-subtitle class="share-candidates__meta">
          <ImpactScore
            v-if="candidate.impactScore != null"
            :score="candidate.impactScore"
            size="small"
            show-value
          />
          <ImpactScore
            v-else-if="candidate.ecoScore != null"
            :score="candidate.ecoScore"
            size="small"
            show-value
          />

          <v-chip v-if="candidate.confidence != null" size="small" class="ml-2">
            {{
              t('share.selection.confidence', {
                value: Math.round(candidate.confidence * 100),
              })
            }}
          </v-chip>
        </v-list-item-subtitle>

        <template #append>
          <div v-if="candidate.bestPrice" class="share-candidates__price">
            <span class="share-candidates__price-value">
              {{ formatPrice(candidate.bestPrice) }}
            </span>
            <span class="share-candidates__price-source">
              {{ candidate.bestPrice.datasourceName ?? t('share.selection.unknownSource') }}
            </span>
          </div>
          <v-btn icon="mdi-chevron-right" variant="text" color="primary" aria-hidden="true" />
        </template>
      </v-list-item>
    </v-list>
  </v-card>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import type { PropType } from 'vue'
import ImpactScore from '~/components/shared/ui/ImpactScore.vue'
import type { ShareCandidateDto } from '~~/shared/api-client'

const _props = defineProps({
  candidates: {
    type: Array as PropType<ShareCandidateDto[]>,
    required: true,
  },
  selectedProductId: {
    type: String,
    default: null,
  },
})

defineEmits<{
  (e: 'select', candidate: ShareCandidateDto): void
}>()

const { t } = useI18n()

const formatPrice = (price?: ShareCandidateDto['bestPrice']) => {
  if (!price?.price && !price?.shortPrice) {
    return t('share.selection.noPrice')
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
.share-candidates {
  border-radius: 18px;
}

.share-candidates__title {
  font-weight: 700;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.share-candidates__subtitle {
  color: rgb(var(--v-theme-text-neutral-secondary));
}

.share-candidates__item {
  border-radius: 12px;
  margin-bottom: 0.5rem;
}

.share-candidates__name {
  font-weight: 600;
  white-space: normal;
}

.share-candidates__meta {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.share-candidates__price {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 0.1rem;
  min-width: 120px;
}

.share-candidates__price-value {
  font-weight: 700;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.share-candidates__price-source {
  font-size: 0.85rem;
  color: rgb(var(--v-theme-text-neutral-secondary));
}
</style>
