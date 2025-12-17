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
</script>

<template>
  <v-slide-group show-arrows :model-value="selectedImage">
    <v-slide-group-item v-for="(item, index) in items" :key="index">
      <NuxtLink
        :to="item.href"
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
      </NuxtLink>
    </v-slide-group-item>
  </v-slide-group>
</template>
