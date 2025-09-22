<template>
  <!-- Mobile menu command -->
  <menu class="d-none d-md-block">
    <!-- TODO: translate -->
    <v-list class="d-flex justify-end">
      <v-list-item
        v-for="(item, index) in ['Impact-score', 'Les produis', 'Blog', 'Contact']"
        :key="index"
      >
        <v-list-item-title>{{ item }}</v-list-item-title>
      </v-list-item>
    </v-list>
  </menu>

  <!-- Desktop menu command -->
  <div class="d-flex justify-end d-md-none">
    <v-btn
      icon
      aria-label="Ouvrir le menu"
      @click="$emit('toggle-drawer')"
    >
      <v-icon>mdi-menu</v-icon>
    </v-btn>
  </div>
</template>

<script setup lang="ts">
defineEmits<{
  'toggle-drawer': []
}>()
</script>

<style scoped lang="sass"></style>
