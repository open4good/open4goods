<template>
  <v-select
    v-model="model"
    :items="items"
    :placeholder="placeholder"
    :aria-label="ariaLabel"
    :menu-props="menuProps"
    density="comfortable"
    variant="outlined"
    hide-details
    class="product-sticky-banner-select"
    return-object
    item-title="label"
    item-value="key"
  >
    <template #item="{ props: itemProps, item }">
      <v-list-item v-bind="itemProps">
        <template v-if="item.raw.favicon" #prepend>
          <v-avatar size="28" class="product-sticky-banner-select__avatar">
            <img :src="item.raw.favicon" :alt="item.raw.merchant" loading="lazy" />
          </v-avatar>
        </template>
        <v-list-item-title class="product-sticky-banner-select__title">{{ item.raw.merchant }}</v-list-item-title>
        <v-list-item-subtitle class="product-sticky-banner-select__subtitle">{{ item.raw.priceLabel }}</v-list-item-subtitle>
      </v-list-item>
    </template>

    <template #selection="{ item }">
      <div class="product-sticky-banner-select__selection">
        <v-avatar
          v-if="item?.raw?.favicon"
          size="24"
          class="product-sticky-banner-select__selection-avatar"
        >
          <img :src="item.raw.favicon" :alt="item.raw.merchant" loading="lazy" />
        </v-avatar>
        <span class="product-sticky-banner-select__selection-label">{{ item?.raw?.label ?? placeholder }}</span>
      </div>
    </template>
  </v-select>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface OfferSelectItem {
  key: string
  label: string
  merchant: string
  priceLabel: string
  favicon: string | null
}

const model = defineModel<OfferSelectItem | null>({ default: null })

const props = withDefaults(
  defineProps<{
    items?: OfferSelectItem[]
    placeholder?: string
    ariaLabel?: string
  }>(),
  {
    items: () => [],
    placeholder: '',
    ariaLabel: '',
  },
)

const items = computed(() => props.items)
const placeholder = computed(() => props.placeholder)
const ariaLabel = computed(() => props.ariaLabel)

const menuProps = computed(() => ({
  contentClass: 'product-sticky-banner-select__menu',
  maxHeight: 320,
}))
</script>

<style scoped>
.product-sticky-banner-select {
  min-width: 200px;
}

.product-sticky-banner-select__selection {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  max-width: 100%;
}

.product-sticky-banner-select__selection-label {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-sticky-banner-select__avatar,
.product-sticky-banner-select__selection-avatar {
  border-radius: 6px;
  background: rgb(var(--v-theme-surface-muted));
}

.product-sticky-banner-select__title {
  font-weight: 600;
}

.product-sticky-banner-select__subtitle {
  font-size: 0.85rem;
}
</style>
