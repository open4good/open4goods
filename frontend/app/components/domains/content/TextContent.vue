<script setup lang="ts">
import { computed, unref, toRef } from 'vue'
import { useContentBloc } from '~/composables/content/useContentBloc'
import {
  DEFAULT_LOREM_LENGTH,
  _generateLoremIpsum,
} from '~/utils/content/_loremIpsum'

// Props

const props = withDefaults(
  defineProps<{
    blocId: string
    defaultLength?: number
    ipsumLength?: number
    fallbackText?: string
  }>(),
  {
    defaultLength: DEFAULT_LOREM_LENGTH,
    ipsumLength: undefined,
    fallbackText: undefined,
  }
)

// Composables
const blocId = toRef(props, 'blocId')
const fallbackText = toRef(props, 'fallbackText')
const { htmlContent, pending, error } = await useContentBloc(blocId)

const fallbackLoremLength = computed(
  () => props.ipsumLength ?? props.defaultLength ?? DEFAULT_LOREM_LENGTH
)

const escapeHtml = (value: string) =>
  value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;')

const fallbackHtml = computed(() => {
  const raw = fallbackText.value?.trim()

  if (!raw) {
    return null
  }

  return `<p>${escapeHtml(raw)}</p>`
})

const displayHtml = computed(() => {
  const rawContent = (unref(htmlContent) ?? '').trim()
  if (rawContent) {
    return rawContent
  }

  if (fallbackHtml.value) {
    return fallbackHtml.value
  }

  return _generateLoremIpsum(fallbackLoremLength.value)
})
</script>

<template>
  <div class="text-content">
    <v-progress-circular v-if="pending" indeterminate />
    <v-alert v-else-if="error" type="error" variant="tonal">{{
      error
    }}</v-alert>

    <!-- Encapsulated content bloc -->
    <!-- eslint-disable-next-line vue/no-v-html -->
    <div v-else class="cms-sandbox" v-html="displayHtml" />
  </div>
</template>

<style scoped>
.text-content {
  padding: 1rem 0;
  position: relative;
}

/* Scoped sandbox to contain inherited rich-text styles */
.cms-sandbox {
  display: block;
  font-family: inherit;
}

.cms-sandbox * {
  box-sizing: border-box;
  font-family: inherit;
}
</style>
