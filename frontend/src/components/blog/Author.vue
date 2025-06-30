<template>
  <div class="flex items-center gap-2">
    <img :src="avatar" alt="" class="w-8 h-8 rounded-full">
    <span>{{ name }}</span>
    <slot />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{ name: string }>()
const avatar = computed(() => `/img/authors/${props.name.toLowerCase().replace(/\s+/g, '-')}.png`)
</script>
