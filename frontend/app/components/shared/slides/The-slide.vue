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
        height="80"
        width="80"
        @click="toggle"
      >
        <v-img
          :src="item"
          height="80"
          cover
        ></v-img>
      </v-card>
    </v-slide-group-item>
  </v-slide-group>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const selectedImage = ref(null)

withDefaults(defineProps<{
  items?: string[]
}>(), {
  items: () => [],
})
</script>
