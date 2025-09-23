<template>
  <menu id="container-main-menu" class="d-none d-md-block">
  <!-- Mobile menu command -->
    <!-- TODO: translate -->
    <v-list class="d-flex justify-end font-weight-bold">
      <v-list-item
        v-for="(item, index) in ['Impact-score', 'Les produis', 'Blog', 'Contact']"
        :key="index"
        class="main-menu-items"
      >
        <v-list-item-title>{{ item }}</v-list-item-title>
      </v-list-item>
    </v-list>
  </menu>

  <!-- Desktop menu command -->
  <div class="d-flex justify-end d-md-none">
    <v-btn icon aria-label="Ouvrir le menu" @click="$emit('toggle-drawer')">
      <v-icon>mdi-menu</v-icon>
    </v-btn>
  </div>
</template>

<script setup lang="ts">
defineEmits<{
  "toggle-drawer": [];
}>();
</script>

<style scoped lang="sass">
.main-menu-items
  color: black
  font-size: 1rem
  cursor: pointer
  font-weight: bolder
  &:hover
    color: green
</style>
