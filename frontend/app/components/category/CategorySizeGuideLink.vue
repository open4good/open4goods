<template>
  <p v-if="guide" class="category-size-guide-link">
    <v-icon icon="mdi-ruler" size="small" class="mr-1" aria-hidden="true" />
    <NuxtLink :to="postLink" class="category-size-guide-link__anchor">
      {{ $t(`${guide.i18nPrefix}.categoryLink`) }}
    </NuxtLink>
  </p>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useLocalePath } from '#i18n'
import { resolveVerticalSizeGuide } from '~/constants/vertical-size-guide'

const props = defineProps<{ verticalId?: string | null }>()

const localePath = useLocalePath()
const guide = computed(() => resolveVerticalSizeGuide(props.verticalId))
const postLink = computed(() =>
  guide.value ? (localePath(guide.value.postPath) ?? guide.value.postPath) : ''
)
</script>
