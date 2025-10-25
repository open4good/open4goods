<template>
  <v-tooltip :text="tooltip" location="top" open-delay="150">
    <template #activator="{ props: tooltipProps }">
      <v-btn
        v-bind="tooltipProps"
        class="category-product-compare-toggle"
        :class="{ 'category-product-compare-toggle--active': isSelected }"
        variant="text"
        size="small"
        color="primary"
        :icon="isSelected ? activeIcon : icon"
        :aria-pressed="isSelected"
        :aria-label="ariaLabel"
        :title="tooltip"
        :disabled="isDisabled"
        density="comfortable"
        data-test="category-product-compare"
        @click.stop.prevent="toggle"
      >
        <v-icon :icon="isSelected ? activeIcon : icon" size="20" />
      </v-btn>
    </template>
  </v-tooltip>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ProductDto } from '~~/shared/api-client'
import {
  MAX_COMPARE_ITEMS,
  useProductCompareStore,
  type CompareListBlockReason,
} from '~/stores/useProductCompareStore'

const props = withDefaults(
  defineProps<{
    product: ProductDto
    icon?: string
    activeIcon?: string
  }>(),
  {
    icon: 'mdi-compare',
    activeIcon: 'mdi-compare',
  },
)

defineOptions({ inheritAttrs: false })

const emit = defineEmits<{
  (event: 'toggled', product: ProductDto, selected: boolean): void
}>()

const { t, te } = useI18n()
const compareStore = useProductCompareStore()

const isSelected = computed(() => compareStore.hasProduct(props.product))

const reasonMessage = (reason: CompareListBlockReason | undefined) => {
  switch (reason) {
    case 'limit-reached':
      return t('category.products.compare.limitReached', { count: MAX_COMPARE_ITEMS })
    case 'vertical-mismatch':
      return t('category.products.compare.differentCategory')
    case 'missing-identifier':
      return t('category.products.compare.missingIdentifier')
    default:
      return null
  }
}

const productDisplayName = (product: ProductDto) =>
  product.identity?.bestName ??
  product.base?.bestName ??
  product.identity?.model ??
  product.identity?.brand ??
  t('category.products.untitledProduct')

const eligibility = computed(() => compareStore.canAddProduct(props.product))

const tooltip = computed(() => {
  if (isSelected.value) {
    return t('category.products.compare.removeFromList')
  }

  if (!eligibility.value.success) {
    return reasonMessage(eligibility.value.reason) ?? t('category.products.compare.addToList')
  }

  return t('category.products.compare.addToList')
})

const ariaLabel = computed(() => {
  if (isSelected.value) {
    if (te('category.products.compare.removeSingle')) {
      return t('category.products.compare.removeSingle', {
        name: productDisplayName(props.product),
      })
    }

    return t('category.products.compare.removeFromList')
  }

  return reasonMessage(eligibility.value.reason) ?? t('category.products.compare.addToList')
})

const isDisabled = computed(() => {
  if (isSelected.value) {
    return false
  }

  return !eligibility.value.success
})

const toggle = () => {
  if (isDisabled.value && !isSelected.value) {
    return
  }

  compareStore.toggleProduct(props.product)
  emit('toggled', props.product, isSelected.value)
}
</script>

<style scoped lang="sass">
.category-product-compare-toggle
  border-radius: 50%
  transition: background-color 0.2s ease, color 0.2s ease

  &--active
    background-color: rgba(var(--v-theme-primary), 0.12)
    color: rgb(var(--v-theme-primary))

  :deep(.v-icon)
    transition: transform 0.2s ease

  &--active :deep(.v-icon)
    transform: scale(1.05)
</style>
