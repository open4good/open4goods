<template>
  <div>
    <SectionRenderer v-for="section in sectionsList" :key="section.id" :section="section" />
    <slot name="after-content" />
  </div>
</template>

<script setup lang="ts">
import SectionRenderer from '~/components/content/SectionRenderer.vue'
import { usePageSections } from '~/composables/usePageSections'

const props = defineProps<{
  slugSegments: string[]
}>()

const { sections, pageKey } = await usePageSections(props.slugSegments)

const sectionsList = computed(() => sections.value ?? [])

if (sectionsList.value.length === 0) {
  throw createError({
    statusCode: 404,
    statusMessage: `No content found for page '${pageKey}'.`,
  })
}
</script>
