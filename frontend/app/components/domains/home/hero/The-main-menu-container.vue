<script lang="ts" setup>
const emit = defineEmits<{
  (event: 'toggle-drawer'): void
}>()

</script>

<template>
    <v-sheet
      tag="header"
      elevation="0"
      class="main-menu-container position-fixed"
    >
      <v-container fluid class="main-menu-content">
        <v-row>
          <v-col cols="3">
            <the-main-logo />
          </v-col>
          <v-col cols="9">
            <the-hero-menu @toggle-drawer="emit('toggle-drawer')" />
          </v-col>
        </v-row>
      </v-container>
    </v-sheet>
</template>

<style lang="sass" scoped>
.main-menu-container
  top: 0
  left: 0
  z-index: 100
  width: 100%
  background-color: white

.main-menu-content
  max-width: 100%
</style>
