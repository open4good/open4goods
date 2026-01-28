<template>
  <CtaCard
    v-if="ecoscoreUrl"
    :to="ecoscoreUrl"
    icon="mdi-leaf"
    :title="t('category.filters.ecoscore.title')"
    :subtitle="
      t('category.filters.ecoscore.cta', { category: normalizedCategoryName })
    "
    :aria-label="t('category.filters.ecoscore.ariaLabel')"
    data-test="category-ecoscore-card"
  />
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import CtaCard from '~/components/shared/CtaCard.vue'

const props = defineProps<{
  verticalHomeUrl?: string | null
  categoryName?: string | null
}>()

const { t, locale } = useI18n()

const normalizedCategoryName = computed(() => {
  if (!props.categoryName) {
    return ''
  }

  return props.categoryName.toLocaleLowerCase(locale.value)
})

const ecoscoreUrl = computed(() => {
  if (!props.verticalHomeUrl) {
    return null
  }

  const normalizedBase = props.verticalHomeUrl.endsWith('/')
    ? props.verticalHomeUrl.slice(0, -1)
    : props.verticalHomeUrl

  return `${normalizedBase}/ecoscore`
})
</script>
