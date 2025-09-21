<template>
  <menu>
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
</template>

<script setup lang="ts">
// content
</script>

<style scoped lang="sass"></style>
