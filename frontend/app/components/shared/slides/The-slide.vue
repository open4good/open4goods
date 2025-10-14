<script setup lang="ts">
import { ref } from "vue";
import { useRouter } from "vue-router";

interface SlideItem {
  imageSmall: string;
  verticalHomeTitle: string;
}

const router = useRouter();
const selectedImage = ref(null);

withDefaults(
  defineProps<{
    items?: SlideItem[];
    height: number;
    width: number;
  }>(),
  {
    items: () => [],
  }
);

const loadedImages = ref<Set<number>>(new Set());

const handleClick = (verticalHomeTitle: string) => {
  router.push(`/produits?category=${verticalHomeTitle}`);
};

const onImageLoad = (index: number) => {
  loadedImages.value.add(index);
};
</script>

<template>
  <v-slide-group show-arrows :model-value="selectedImage">
    <v-slide-group-item
      v-for="(item, index) in items"
      :key="index"
    >
    <div>
      <v-img
        :src="item.imageSmall"
        :height="height"
        :width="width"
        cover
        class="cursor-pointer"
        @click.stop="() => handleClick(item.verticalHomeTitle)"
        @load="() => onImageLoad(index)"
      />
      <div v-if="loadedImages.has(index)" class="text-center pa-2 text-caption">
        {{ item.verticalHomeTitle }}
      </div>
    </div>
    </v-slide-group-item>
  </v-slide-group>
</template>

