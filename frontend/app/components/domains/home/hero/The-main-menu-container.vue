<script lang="ts" setup>
defineEmits<{
  'toggle-drawer': []
}>()
</script>

<template>
    <v-container fluid class="main-menu-container position-sticky">
      <v-row>
        <v-col cols="3">
          <the-main-logo />
        </v-col>
        <v-col cols="9">
          <the-hero-menu @toggle-drawer="$emit('toggle-drawer')" />
        </v-col>
      </v-row>
    </v-container>
</template>

<style lang="sass" scoped>
.main-menu-container
  top: 0
  z-index: 100
  background-color: white
</style>
