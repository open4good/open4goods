<script lang="ts" setup>
import { ref } from 'vue'

const search = ref('')
</script>

<template>
  <!-- TODO:  https://vuetifyjs.com/en/components/toolbars/#floating-with-search  -->
  <div class="search-bar-container">
    <v-text-field
      v-model="search"
      label="Nudge ton produit ..."
      prepend-inner-icon="mdi-magnify"
      variant="outlined"
      clearable
      hide-details
      single-line
      class="search-bar"
    >
    </v-text-field>
  </div>
</template>

<style lang="sass" scoped>

.search-bar-container
  display: flex
  align-items: center
  background-color: white
  border-radius: 10px
  border-block-color: #ccc
  width: 70%

.search-bar
  flex-grow: 1
</style>
