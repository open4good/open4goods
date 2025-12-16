<template>
  <article class="product-alternative-card">
    <NuxtLink
      :to="productLink"
      class="product-alternative-card__link"
      :aria-label="
        t('product.impact.alternatives.viewProduct', { name: displayName })
      "
      :prefetch="false"
    >
      <div class="product-alternative-card__media">
        <NuxtImg
          v-if="image"
          :src="image"
          :alt="displayName"
          width="220"
          height="150"
          class="product-alternative-card__image"
        />
        <div v-else class="product-alternative-card__placeholder">
          <v-icon icon="mdi-image-off" size="32" />
        </div>
      </div>
      <div class="product-alternative-card__body">
        <h4 class="product-alternative-card__title">{{ title }}</h4>
        <div
          v-if="popularAttributesList.length"
          class="product-alternative-card__attributes"
          role="list"
        >
          <v-chip
            v-for="attribute in popularAttributesList"
            :key="attribute.key"
            size="small"
            class="product-alternative-card__attribute"
            variant="flat"
            color="surface-primary-080"
            role="listitem"
          >
            <v-icon
              v-if="attribute.icon"
              :icon="attribute.icon"
              size="16"
              class="product-alternative-card__attribute-icon"
            />
            <span class="product-alternative-card__attribute-value">{{
              attribute.value
            }}</span>
          </v-chip>
        </div>
        <div class="product-alternative-card__score">
          <ImpactScore
            v-if="impactScore != null"
            :score="impactScore"
            :max="5"
            size="small"
          />
          <span v-if="bestPrice" class="product-alternative-card__price">{{
            bestPrice
          }}</span>
        </div>
      </div>
    </NuxtLink>
    <div class="product-alternative-card__footer">
      <v-tooltip :text="compareButtonTitle" location="top" open-delay="150">
        <template #activator="{ props: tooltipProps }">
          <v-btn
            v-bind="tooltipProps"
            class="product-alternative-card__compare-btn"
            :class="{
              'product-alternative-card__compare-btn--active':
                isCompareSelected,
            }"
            variant="flat"
            color="primary"
            :aria-pressed="isCompareSelected"
            :aria-label="compareButtonAriaLabel"
            :title="compareButtonTitle"
            :disabled="isCompareDisabled"
            @click.stop.prevent="toggleCompare"
          >
            <v-icon :icon="compareButtonIcon" size="18" />
            <span class="product-alternative-card__compare-label">{{
              compareButtonLabel
            }}</span>
          </v-btn>
        </template>
      </v-tooltip>
    </div>
  </article>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { PropType } from 'vue'
import { useI18n } from 'vue-i18n'
import type { AttributeConfigDto, ProductDto } from '~~/shared/api-client'
import ImpactScore from '~/components/shared/ui/ImpactScore.vue'
import {
  MAX_COMPARE_ITEMS,
  useProductCompareStore,
  type CompareListBlockReason,
} from '~/stores/useProductCompareStore'
import { formatBestPrice } from '~/utils/_product-pricing'
import { resolvePrimaryImpactScore } from '~/utils/_product-scores'
import {
  formatAttributeValue,
  resolvePopularAttributes,
} from '~/utils/_product-attributes'

const props = defineProps({
  product: {
    type: Object as PropType<ProductDto>,
    required: true,
  },
  popularAttributes: {
    type: Array as PropType<AttributeConfigDto[]>,
    default: () => [],
  },
})

const { t, n, te } = useI18n()

const product = computed(() => props.product)

const image = computed(() => {
  const candidate =
    product.value.resources?.coverImagePath ??
    product.value.resources?.externalCover ??
    product.value.resources?.images?.[0]?.url ??
    null

  return candidate || null
})

const brand = computed(() => product.value.identity?.brand?.trim() ?? '')
const model = computed(() => product.value.identity?.model?.trim() ?? '')

const brandModelTitle = computed(() => {
  const segments = [brand.value, model.value].filter(value => value.length)
  return segments.join(' • ')
})

const displayName = computed(() => {
  return (
    brandModelTitle.value ||
    product.value.identity?.bestName ||
    product.value.base?.bestName ||
    product.value.names?.h1Title ||
    product.value.identity?.model ||
    product.value.identity?.brand ||
    t('product.impact.alternatives.untitled')
  )
})

const title = computed(() => brandModelTitle.value || displayName.value)

const bestPrice = computed(() => formatBestPrice(product.value, t, n))

const impactScore = computed(() => resolvePrimaryImpactScore(product.value))

const popularAttributeConfigs = computed(() => props.popularAttributes ?? [])

const popularAttributesList = computed(() =>
  resolvePopularAttributes(product.value, popularAttributeConfigs.value)
    .map(attribute => {
      const value = formatAttributeValue(attribute, t, n)
      if (!value) {
        return null
      }

      return {
        key: attribute.key,
        icon: attribute.icon ?? null,
        value,
      }
    })
    .filter(
      (
        attribute
      ): attribute is { key: string; icon: string | null; value: string } =>
        Boolean(attribute)
    )
)

const productLink = computed(() => {
  return product.value.fullSlug ?? product.value.slug ?? '#'
})

const compareStore = useProductCompareStore()

const reasonMessage = (reason: CompareListBlockReason | undefined) => {
  switch (reason) {
    case 'limit-reached':
      return t('category.products.compare.limitReached', {
        count: MAX_COMPARE_ITEMS,
      })
    case 'vertical-mismatch':
      return t('category.products.compare.differentCategory')
    case 'missing-identifier':
      return t('category.products.compare.missingIdentifier')
    default:
      return t('product.hero.compare.add')
  }
}

const compareEligibility = computed(() =>
  compareStore.canAddProduct(product.value)
)
const isCompareSelected = computed(() => compareStore.hasProduct(product.value))

const compareButtonLabel = computed(() =>
  isCompareSelected.value
    ? t('product.hero.compare.selected')
    : t('product.hero.compare.label')
)

const compareButtonIcon = computed(() =>
  isCompareSelected.value
    ? 'mdi-check-circle-outline'
    : 'mdi-compare-horizontal'
)

const compareButtonTitle = computed(() => {
  if (isCompareSelected.value) {
    return t('product.hero.compare.remove')
  }

  if (!compareEligibility.value.success) {
    return reasonMessage(compareEligibility.value.reason)
  }

  return t('product.hero.compare.add')
})

const compareButtonAriaLabel = computed(() => {
  const name = displayName.value

  if (isCompareSelected.value) {
    if (te('product.hero.compare.ariaSelected')) {
      return t('product.hero.compare.ariaSelected', { name })
    }

    return t('product.hero.compare.remove')
  }

  if (!compareEligibility.value.success) {
    return reasonMessage(compareEligibility.value.reason)
  }

  if (te('product.hero.compare.ariaAdd')) {
    return t('product.hero.compare.ariaAdd', { name })
  }

  return t('product.hero.compare.add')
})

const isCompareDisabled = computed(
  () => !isCompareSelected.value && !compareEligibility.value.success
)

const toggleCompare = () => {
  if (isCompareDisabled.value) {
    return
  }

  compareStore.toggleProduct(product.value)
}
</script>

<style scoped>
.product-alternative-card {
  position: relative;
  display: flex;
  flex-direction: column;
  border-radius: 20px;
  background: rgba(var(--v-theme-surface-default), 0.96);
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.12);
  overflow: hidden;
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease;
}

.product-alternative-card__link {
  display: flex;
  flex-direction: column;
  text-decoration: none;
  color: inherit;
  flex: 1 1 auto;
  gap: 0.75rem;
}

.product-alternative-card__link:hover,
.product-alternative-card__link:focus-visible {
  color: inherit;
}

.product-alternative-card:hover,
.product-alternative-card:focus-within {
  transform: translateY(-4px);
  box-shadow: 0 18px 32px rgba(15, 23, 42, 0.18);
}

.product-alternative-card__media {
  position: relative;
  padding: 0.75rem;
  background: rgba(var(--v-theme-surface-primary-080), 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
}

.product-alternative-card__image {
  width: 100%;
  height: auto;
  max-height: 160px;
  object-fit: contain;
  border-radius: 14px;
}

.product-alternative-card__placeholder {
  width: 100%;
  height: 140px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(var(--v-theme-text-neutral-soft), 0.6);
  background: rgba(var(--v-theme-surface-glass), 0.6);
}

.product-alternative-card__body {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  padding: 0 1rem 1rem;
  flex: 1 1 auto;
}

.product-alternative-card__title {
  margin: 0;
  font-size: 1rem;
  font-weight: 600;
  line-height: 1.3;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.product-alternative-card__attributes {
  display: flex;
  flex-wrap: wrap;
  gap: 0.4rem;
}

.product-alternative-card__attribute {
  font-weight: 500;
}

.product-alternative-card__attribute-icon {
  margin-right: 0.35rem;
}

.product-alternative-card__attribute-value {
  color: rgba(var(--v-theme-text-neutral-secondary), 0.85);
}

.product-alternative-card__score {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 0.4rem;
}

.product-alternative-card__price {
  font-weight: 600;
  font-size: 0.95rem;
  color: rgb(var(--v-theme-primary));
}

.product-alternative-card__footer {
  display: flex;
  justify-content: center;
  padding: 0 1rem 1rem;
}

.product-alternative-card__compare-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  border-radius: 999px;
  text-transform: none;
  font-weight: 600;
  letter-spacing: 0;
  padding: 0.45rem 0.9rem;
  min-width: unset;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.16);
  background-color: rgba(var(--v-theme-surface-default), 0.95);
  color: rgb(var(--v-theme-text-neutral-strong));
  transition:
    background-color 0.2s ease,
    box-shadow 0.2s ease,
    transform 0.2s ease;
}

.product-alternative-card__compare-btn:hover,
.product-alternative-card__compare-btn:focus-visible {
  transform: translateY(-1px);
  box-shadow: 0 14px 30px rgba(15, 23, 42, 0.22);
}

.product-alternative-card__compare-btn--active {
  background-color: rgba(var(--v-theme-primary), 0.16);
  color: rgb(var(--v-theme-primary));
}

.product-alternative-card__compare-label {
  font-size: 0.85rem;
}

.product-alternative-card__compare-btn :deep(.v-icon) {
  transition: transform 0.2s ease;
}

.product-alternative-card__compare-btn--active :deep(.v-icon) {
  transform: scale(1.05);
}
</style>
