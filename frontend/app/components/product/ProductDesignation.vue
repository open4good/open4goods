<template>
  <div class="product-designation" :class="rootClass">
    <slot
      :short-name="shortName"
      :long-name="longName"
      :short-description="undefined"
      :display-title="displayTitle"
    >
      <component :is="titleTag" :class="titleClass">
        {{ prefixedDisplayTitle }}
      </component>
    </slot>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ProductDto } from '~~/shared/api-client'
import {
  resolveProductCardName,
  resolveProductLongName,
  resolveProductShortName,
} from '~/utils/_product-title-resolver'

const props = withDefaults(
  defineProps<{
    product: ProductDto
    variant?: 'card' | 'page'
    titleTag?: keyof HTMLElementTagNameMap
    titleClass?: string
    descriptionClass?: string
    showShortDescription?: boolean
    titlePrefix?: string
  }>(),
  {
    variant: 'card',
    titleTag: 'h5',
    titleClass: 'product-designation__title',
    descriptionClass: 'product-designation__description',
    showShortDescription: undefined,
    titlePrefix: undefined,
  }
)

const { locale } = useI18n()

const shortName = computed(() =>
  resolveProductShortName(props.product, locale.value)
)
const cardName = computed(() =>
  resolveProductCardName(props.product, locale.value)
)
const longName = computed(() =>
  resolveProductLongName(props.product, locale.value)
)
const displayTitle = computed(() =>
  props.variant === 'page' ? longName.value : cardName.value
)

const prefixedDisplayTitle = computed(() => {
  const prefix = props.titlePrefix?.trim()
  if (!prefix || displayTitle.value.toLowerCase().startsWith(prefix.toLowerCase())) {
    return displayTitle.value
  }
  return `${prefix} ${displayTitle.value}`
})

const rootClass = computed(() =>
  props.variant === 'card'
    ? 'product-designation--card'
    : 'product-designation--page'
)
</script>

<style scoped lang="scss">
.product-designation {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;

  &__title {
    margin: 0;
  }

  &--card {
    align-items: center;
  }

  &--card &__title {
    text-align: center;
  }

  &__description {
    margin: 0;
    color: rgb(var(--v-theme-text-neutral-secondary));
    font-size: 0.95rem;
  }
}
</style>
