<script lang="ts" setup>
defineEmits<{
  'toggle-drawer': []
}>()
</script>

<template>
  <header class="main-menu-header">
    <v-container fluid class="main-menu-container">
      <v-row>
        <v-col cols="3">
          <the-main-logo />
        </v-col>
        <v-col cols="9">
          <the-hero-menu @toggle-drawer="$emit('toggle-drawer')" />
        </v-col>
      </v-row>
    </v-container>
  </header>
</template>

<style lang="sass" scoped>
.main-menu-header
  position: sticky
  top: 0
  z-index: 100
  width: 100%
  background-color: white

.main-menu-container
  background-color: white
</style>
