<script lang="ts" setup>
const emit = defineEmits<{
  (event: 'toggle-drawer'): void
}>()
</script>

<template>
  <v-container fluid class="main-menu-container position-fixed">
    <v-row>
      <v-col cols="3">
        <the-main-logo />
      </v-col>
      <v-col cols="9">
        <the-hero-menu @toggle-drawer="emit('toggle-drawer')" />
      </v-col>
    </v-row>
  </v-container>
</template>

<style lang="sass" scoped>
.main-menu-container
  top: 0
  left: 0
  z-index: 100
  background-color: white
</style>