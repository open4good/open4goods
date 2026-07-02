<script setup lang="ts">
import { ref } from 'vue'

interface SlideItem {
  imageSmall: string
  verticalHomeTitle: string
  href: string
}

const selectedImage = ref(null)

withDefaults(
  defineProps<{
    items?: SlideItem[]
    height: number
    width: number
  }>(),
  {
    items: () => [],
  }
)

const loadedImages = ref<Set<number>>(new Set())

const onImageLoad = (index: number) => {
  loadedImages.value.add(index)
}

const isExternalUrl = (href: string) => /^https?:\/\//i.test(href)
</script>

<template>
  <v-slide-group show-arrows :model-value="selectedImage">
    <v-slide-group-item v-for="(item, index) in items" :key="index">
      <component
        :is="isExternalUrl(item.href) ? 'a' : 'NuxtLink'"
        :to="isExternalUrl(item.href) ? undefined : item.href"
        :href="isExternalUrl(item.href) ? item.href : undefined"
        :target="isExternalUrl(item.href) ? '_blank' : undefined"
        :rel="isExternalUrl(item.href) ? 'noopener noreferrer' : undefined"
        class="d-inline-flex flex-column align-center text-decoration-none"
      >
        <v-img
          :src="item.imageSmall"
          :height="height"
          :width="width"
          cover
          class="cursor-pointer"
          @load="() => onImageLoad(index)"
        />
        <div
          v-if="loadedImages.has(index)"
          class="text-center pa-2 text-caption text-high-emphasis"
        >
          {{ item.verticalHomeTitle }}
        </div>
      </component>
    </v-slide-group-item>
  </v-slide-group>
</template>
