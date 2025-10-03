<script lang="ts" setup>
import { computed } from 'vue'
import { useDisplay } from 'vuetify'

const props = withDefaults(defineProps<{
  appBarHeight?: number
}>(), {
  appBarHeight: undefined,
})

const emit = defineEmits<{
  (event: 'toggle-drawer'): void
}>()

const { mdAndUp } = useDisplay()

const fallbackHeight = computed(() => (mdAndUp.value ? 64 : 56))
const resolvedAppBarHeight = computed(() => props.appBarHeight ?? fallbackHeight.value)
const appBarCssVariables = computed(() => ({
  '--app-bar-height': `${resolvedAppBarHeight.value}px`,
}))

const handleToggleDrawer = () => emit('toggle-drawer')
</script>

<template>
  <div class="main-menu-wrapper" :style="appBarCssVariables">
    <v-app-bar
      app
      flat
      :height="resolvedAppBarHeight"
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
