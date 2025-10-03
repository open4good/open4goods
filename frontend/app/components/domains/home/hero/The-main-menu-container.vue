<script lang="ts" setup>
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  appBarHeight?: number
}>(), {
  appBarHeight: 64,
})

const emit = defineEmits<{
  (event: 'toggle-drawer'): void
}>()

const appBarCssVariables = computed(() => ({
  '--app-bar-height': `${props.appBarHeight}px`,
}))

const handleToggleDrawer = () => emit('toggle-drawer')
</script>

<template>
  <div class="main-menu-wrapper" :style="appBarCssVariables">
    <v-app-bar
      app
      flat
      :height="props.appBarHeight"
      color="surface-default"
      class="main-menu-app-bar"
    >
      <v-container fluid class="py-0">
        <div class="d-flex align-center w-100">
          <v-app-bar-title class="d-flex align-center">
            <the-main-logo />
          </v-app-bar-title>
          <v-spacer />
          <the-hero-menu @toggle-drawer="handleToggleDrawer" />
        </div>
      </v-container>
    </v-app-bar>
  </div>
</template>

<style lang="sass" scoped>
.main-menu-wrapper
  --app-bar-height: 64px

.main-menu-app-bar
  color: rgb(var(--v-theme-text-neutral-strong))

@media (max-width: 959px)
  .main-menu-app-bar
    padding-inline: 8px
</style>
