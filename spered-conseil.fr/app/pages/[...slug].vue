<template>
  <SectionPage :slug-segments="resolvedSegments" />
</template>

<script setup lang="ts">
import SectionPage from '~/components/content/SectionPage.vue'

const route = useRoute()

const resolvedSegments = computed(() => {
  const slug = route.params.slug

  if (!slug) {
    return []
  }

  return Array.isArray(slug) ? slug : [slug]
})
</script>
