<script lang="ts" setup>
const emit = defineEmits<{
  (event: 'toggle-drawer'): void
}>()

const handleToggleDrawer = () => emit('toggle-drawer')
</script>

<template>
  <v-app-bar app flat color="surface-default" class="main-menu-app-bar">
    <v-container fluid class="py-0 mt-2">
      <div class="d-flex align-center w-100">
        <v-app-bar-title class="d-flex align-center mt-sm">
          <the-main-logo />
        </v-app-bar-title>
        <v-spacer />
        <the-hero-menu @toggle-drawer="handleToggleDrawer" />
      </div>
    </v-container>
  </v-app-bar>
</template>

<style lang="sass" scoped>
.main-menu-app-bar
  color: rgb(var(--v-theme-text-neutral-strong))
  background-color: rgb(var(--v-theme-surface-default))
  border-radius: 0 0 40px 40px
  box-shadow: 0 16px 28px rgba(var(--v-theme-shadow-primary-600), 0.12);

@media (max-width: 959px)
  .main-menu-app-bar
    padding-inline: 8px
</style>
