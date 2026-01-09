<template>
  <div class="product-impact-cta mb-6">
    <HeroActionCard variant="default">
      <div class="product-impact-cta__content">
        <div class="product-impact-cta__header">
          <v-icon
            icon="mdi-leaf-circle"
            size="24"
            class="product-impact-cta__icon"
          />
          <span class="product-impact-cta__title font-weight-bold">
            {{ $t('product.impact.cta.title', 'Impact Score') }}
          </span>
        </div>

        <p class="product-impact-cta__description text-body-2 mb-3">
          {{
            $t(
              'product.impact.cta.description',
              'See how this product compares to others in {category}',
              { category: categoryName }
            )
          }}
        </p>

        <v-btn
          :to="categoryLink"
          class="product-impact-cta__button"
          color="white"
          variant="flat"
          block
          rounded="lg"
          prepend-icon="mdi-arrow-right"
        >
          {{ $t('product.impact.cta.button', 'View Category') }}
        </v-btn>
      </div>
    </HeroActionCard>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import HeroActionCard from '~/components/shared/HeroActionCard.vue'
import type { CategoryDto } from '~~/shared/api-client'

const props = defineProps<{
  categoryDetail: CategoryDto
}>()

const categoryName = computed(() => {
  return props.categoryDetail.title ?? props.categoryDetail.name ?? ''
})

const categoryLink = computed(() => {
  return props.categoryDetail.verticalHomeUrl ?? ''
})
</script>

<style scoped>
.product-impact-cta__content {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.product-impact-cta__header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.25rem;
}

/** 
 * Ensure text colors work well on the SVG background.
 * The SVG is likely dark/rich, so white text is safer.
 * Helper classes or scoped styles can enforce this.
 */
.product-impact-cta__title,
.product-impact-cta__description,
.product-impact-cta__icon {
  color: white;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

.product-impact-cta__button {
  color: rgb(
    var(--v-theme-primary)
  ) !important; /* Text color for the white button */
  font-weight: 700;
  text-transform: none;
}
</style>
