<template>
  <v-tooltip :text="tooltip" location="top" open-delay="150">
    <template #activator="{ props: tooltipProps }">
      <v-btn
        v-bind="tooltipProps"
        class="compare-toggle-button"
        :class="[
          `compare-toggle-button--variant-${variant}`,
          `compare-toggle-button--size-${size}`,
          {
            'compare-toggle-button--active': isSelected,
            'compare-toggle-button--inactive': !isSelected,
          },
        ]"
        :variant="variant === 'icon-only' ? 'flat' : 'elevated'"
        :color="buttonColor"
        :aria-pressed="isSelected"
        :aria-label="ariaLabel"
        :title="tooltip"
        :disabled="isDisabled"
        :loading="loading"
        data-test="product-compare-toggle"
        @click.stop.prevent="toggle"
      >
        <template v-if="variant === 'icon-only'">
          <template v-if="isSelected">
            <span class="compare-toggle-button__minus" aria-hidden="true"
              >&minus;</span
            >
          </template>
          <template v-else>
            <v-icon :icon="icon" :size="iconSize" />
          </template>
        </template>

        <template v-else>
          <v-icon
            :icon="currentIcon"
            :start="variant !== 'icon-only'"
            size="small"
          />
          <span class="compare-toggle-button__label">
            {{ buttonLabel }}
          </span>
        </template>
      </v-btn>
    </template>
  </v-tooltip>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
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
    variant?: 'icon-only' | 'button-icon' | 'button-text'
    size?: 'compact' | 'comfortable' | 'large'
    icon?: string
  }>(),
  {
    variant: 'icon-only',
    size: 'comfortable',
    icon: 'mdi-compare-horizontal',
  }
)

const emit = defineEmits<{
  (event: 'toggled', product: ProductDto, selected: boolean): void
}>()

const { t, te, locale } = useI18n()
const compareStore = useProductCompareStore()
const loading = ref(false)

const isSelected = computed(() => compareStore.hasProduct(props.product))

const iconSize = computed(() => {
  switch (props.size) {
    case 'compact':
      return 20
    case 'large':
      return 28
    default:
      return 24
  }
})

const currentIcon = computed(() => {
  if (isSelected.value) return 'mdi-check'
  return props.icon
})

const buttonColor = computed(() => {
  if (props.variant === 'icon-only') return 'primary'
  return isSelected.value ? 'primary' : 'surface'
})

const buttonLabel = computed(() => {
  if (isSelected.value) {
    return props.variant === 'button-icon'
      ? t('products.compare.addedButtonShort')
      : t('products.compare.removeButtonFull')
  }
  return props.variant === 'button-icon'
    ? t('products.compare.addButtonShort')
    : t('products.compare.addButtonFull')
})

const productDisplayName = (product: ProductDto) =>
  resolveProductShortName(product, locale.value) ||
  t('category.products.untitledProduct')

const eligibility = computed(() => compareStore.canAddProduct(props.product))

const reasonMessage = (reason: CompareListBlockReason | undefined) => {
  switch (reason) {
    case 'limit-reached':
      return t('products.compare.limitReached', {
        count: MAX_COMPARE_ITEMS,
      })
    case 'vertical-mismatch':
      return t('products.compare.differentCategory')
    case 'missing-identifier':
      return t('products.compare.missingIdentifier')
    default:
      return null
  }
}

const tooltip = computed(() => {
  if (isSelected.value) {
    return t('products.compare.removeFromList')
  }

  if (!eligibility.value.success) {
    return (
      reasonMessage(eligibility.value.reason) ?? t('products.compare.addToList')
    )
  }

  return t('products.compare.addToList')
})

const ariaLabel = computed(() => {
  if (isSelected.value) {
    if (te('products.compare.removeSingle')) {
      return t('products.compare.removeSingle', {
        name: productDisplayName(props.product),
      })
    }
    return t('products.compare.removeFromList')
  }

  return (
    reasonMessage(eligibility.value.reason) ?? t('products.compare.addToList')
  )
})

const isDisabled = computed(() => {
  if (isSelected.value) return false
  return !eligibility.value.success
})

const toggle = async () => {
  if (isDisabled.value && !isSelected.value) return

  loading.value = true
  // Simulate small delay for better UX on button variants
  if (props.variant !== 'icon-only') {
    await new Promise(resolve => setTimeout(resolve, 150))
  }

  compareStore.toggleProduct(props.product)
  emit('toggled', props.product, isSelected.value)
  loading.value = false
}
</script>

<style scoped lang="sass">
.compare-toggle-button
  transition: all 0.2s ease
  text-transform: none
  letter-spacing: 0
  font-weight: 600

  /* Icon only variant - Circular */
  &--variant-icon-only
    border-radius: 50%
    min-width: 0
    padding: 0
    box-shadow: 0 4px 12px rgba(21, 46, 73, 0.12)
    background-color: rgba(var(--v-theme-surface-default), 0.92)

    &:hover
      transform: translateY(-2px)
      box-shadow: 0 8px 16px rgba(21, 46, 73, 0.16)

    &.compare-toggle-button--active
      background-color: rgba(var(--v-theme-primary), 0.16)
      color: rgb(var(--v-theme-primary))
      box-shadow: none

  /* Size adjustments for Icon Only */
  &--variant-icon-only.compare-toggle-button--size-compact
    width: 2.5rem
    height: 2.5rem

  &--variant-icon-only.compare-toggle-button--size-comfortable
    width: 3.125rem
    height: 3.125rem

  &--variant-icon-only.compare-toggle-button--size-large
    width: 3.75rem
    height: 3.75rem

  /* Check/Minus styles for icon-only */
  &__minus
    font-size: 1.5em
    line-height: 1
    font-weight: 300

  /* Button variants */
  &--variant-button-icon,
  &--variant-button-text
    border-radius: 99px
    box-shadow: 0 2px 8px rgba(0,0,0, 0.08)

    &:hover
      background-color: rgb(var(--v-theme-surface-default))
      box-shadow: 0 4px 12px rgba(0,0,0, 0.12)

    &.compare-toggle-button--active
      background-color: rgb(var(--v-theme-primary))
      color: white !important

  &--inactive
    opacity: 0.85

  &:disabled
    opacity: 0.5
    cursor: not-allowed
    box-shadow: none !important
    transform: none !important
</style>
