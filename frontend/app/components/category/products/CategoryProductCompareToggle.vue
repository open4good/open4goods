<template>
  <v-tooltip :text="tooltip" location="top" open-delay="150">
    <template #activator="{ props: tooltipProps }">
      <v-btn
        v-bind="tooltipProps"
        class="category-product-compare-toggle"
        :class="[
          sizeClass,
          {
            'category-product-compare-toggle--active': isSelected,
            'category-product-compare-toggle--inactive': !isSelected,
          },
        ]"
        variant="flat"
        color="primary"
        :aria-pressed="isSelected"
        :aria-label="ariaLabel"
        :title="tooltip"
        :disabled="isDisabled"
        data-test="category-product-compare"
        @click.stop.prevent="toggle"
      >
        <template v-if="isSelected">
          <span
            class="category-product-compare-toggle__minus"
            aria-hidden="true"
            >&minus;</span
          >
        </template>
        <template v-else>
          <v-icon :icon="icon" :size="iconSize" />
        </template>
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
import { resolveProductShortName } from '~/utils/_product-title-resolver'

const props = withDefaults(
  defineProps<{
    product: ProductDto
    icon?: string
    size?: 'compact' | 'comfortable' | 'large'
  }>(),
  {
    icon: 'mdi-compare-horizontal',
    size: 'comfortable',
  }
)

defineOptions({ inheritAttrs: false })

const emit = defineEmits<{
  (event: 'toggled', product: ProductDto, selected: boolean): void
}>()

const { t, te, locale } = useI18n()
const compareStore = useProductCompareStore()

const isSelected = computed(() => compareStore.hasProduct(props.product))

const sizeClass = computed(
  () => `category-product-compare-toggle--size-${props.size ?? 'comfortable'}`
)

const iconSize = computed(() => {
  switch (props.size) {
    case 'compact':
      return 26
    case 'large':
      return 40
    default:
      return 32
  }
})

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
      return null
  }
}

const productDisplayName = (product: ProductDto) =>
  resolveProductShortName(product, locale.value) ||
  t('category.products.untitledProduct')

const eligibility = computed(() => compareStore.canAddProduct(props.product))

const tooltip = computed(() => {
  if (isSelected.value) {
    return t('category.products.compare.removeFromList')
  }

  if (!eligibility.value.success) {
    return (
      reasonMessage(eligibility.value.reason) ??
      t('category.products.compare.addToList')
    )
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

  return (
    reasonMessage(eligibility.value.reason) ??
    t('category.products.compare.addToList')
  )
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
  transition: background-color 0.2s ease, color 0.2s ease, opacity 0.2s ease
  background-color: rgba(var(--v-theme-surface-default), 0.92)
  box-shadow: 0 6px 16px rgba(21, 46, 73, 0.12)
  display: inline-flex
  align-items: center
  justify-content: center

  &--size-compact
    width: 2.5rem
    min-width: 2.5rem
    height: 2.5rem

    :deep(.v-icon)
      font-size: 1.35rem

  &--size-comfortable
    width: 3.125rem
    min-width: 3.125rem
    height: 3.125rem

  &--size-large
    width: 3.75rem
    min-width: 3.75rem
    height: 3.75rem

    :deep(.v-icon)
      font-size: 2.25rem

  &:hover
    opacity: 0.9

  &--inactive
    opacity: 0.6

  &--active
    opacity: 1
    background-color: rgba(var(--v-theme-primary), 0.16)
    color: rgb(var(--v-theme-primary))

  :deep(.v-icon)
    transition: transform 0.2s ease

  &--active :deep(.v-icon)
    transform: scale(1.05)

  &__minus
    font-size: 1.75rem
    font-weight: 600
    line-height: 1
    color: currentColor

  &--size-compact &__minus
    font-size: 1.4rem

  &--size-large &__minus
    font-size: 2rem
</style>
