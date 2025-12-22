<template>
  <v-btn
    :href="link.url"
    target="_blank"
    rel="noopener noreferrer"
    :variant="variant"
    :color="buttonColor"
    :size="size"
    :prepend-icon="iconName"
    class="external-link-button"
  >
    {{ link.label }}
    <template #append>
      <v-icon size="small" icon="mdi-open-in-new" />
    </template>
  </v-btn>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ExternalLink } from '~~/types/next-release'

const props = withDefaults(
  defineProps<{
    link: ExternalLink
    variant?: 'flat' | 'tonal' | 'outlined' | 'elevated'
    size?: 'small' | 'default' | 'large'
  }>(),
  {
    variant: 'tonal',
    size: 'default',
  }
)

const iconName = computed(() => {
  if (props.link.icon) {
    return props.link.icon
  }

  switch (props.link.type) {
    case 'linkedin':
      return 'mdi-linkedin'
    case 'twitter':
      return 'mdi-twitter'
    case 'github':
      return 'mdi-github'
    case 'survey':
      return 'mdi-poll'
    default:
      return 'mdi-link'
  }
})

const buttonColor = computed(() => {
  switch (props.link.type) {
    case 'linkedin':
      return 'primary'
    case 'github':
      return 'on-surface'
    default:
      return 'primary'
  }
})
</script>

<style scoped lang="sass">
.external-link-button
  font-weight: 600
  text-transform: none
  letter-spacing: 0.01em
</style>
