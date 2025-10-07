<template>
  <v-slide-group
    show-arrows
    :model-value="selectedImage"
  >
    <v-slide-group-item
      v-for="(item, index) in items"
      :key="index"
      v-slot="{ isSelected, toggle }"
    >
      <v-card
        :color="isSelected ? 'primary' : 'grey-lighten-1'"
        class="ma-2"
        :height="height"
        width="auto"
        @click="toggle"
      >
        <v-img
          :src="item"
          :height="height"
          cover
          @click.stop="handleClick"
        ></v-img>
      </v-card>
    </v-slide-group-item>
  </v-slide-group>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const selectedImage = ref(null)

withDefaults(defineProps<{
  items?: string[],
  height: number,
}>(), {
  items: () => [],
})

const handleClick = () => {
  router.push('/produits')
}
</script>
