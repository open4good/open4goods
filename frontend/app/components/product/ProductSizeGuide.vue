<template>
  <v-card v-if="guide" class="product-size-guide" variant="tonal" rounded="lg">
    <v-card-item>
      <template #prepend>
        <v-icon icon="mdi-ruler" aria-hidden="true" />
      </template>
      <v-card-title class="text-subtitle-1">
        {{ $t(`${guide.i18nPrefix}.productTitle`) }}
      </v-card-title>
    </v-card-item>
    <v-card-text>
      {{ sentence }}
      <NuxtLink :to="postLink" class="product-size-guide__anchor">
        {{ $t(`${guide.i18nPrefix}.productLink`) }}
      </NuxtLink>
    </v-card-text>
  </v-card>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useLocalePath } from '#i18n'
import {
  inchesToCm,
  resolveVerticalSizeGuide,
} from '~/constants/vertical-size-guide'
import type { ProductIndexedAttributeDto } from '~~/shared/api-client'

const props = defineProps<{
  verticalId?: string | null
  indexedAttributes?: Record<string, ProductIndexedAttributeDto> | null
}>()

const { t } = useI18n()
const localePath = useLocalePath()

const guide = computed(() => resolveVerticalSizeGuide(props.verticalId))
const postLink = computed(() =>
  guide.value ? (localePath(guide.value.postPath) ?? guide.value.postPath) : ''
)
const diagonalCm = computed(() => {
  if (!guide.value) {
    return null
  }
  return inchesToCm(
    props.indexedAttributes?.[guide.value.diagonalInchesAttribute]?.numericValue
  )
})
const sentence = computed(() => {
  if (!guide.value) {
    return ''
  }
  const prefix = guide.value.i18nPrefix
  return diagonalCm.value === null
    ? t(`${prefix}.productText`)
    : t(`${prefix}.productTextWithDiagonal`, { cm: diagonalCm.value })
})
</script>
