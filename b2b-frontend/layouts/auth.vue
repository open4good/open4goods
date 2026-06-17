<template>
  <v-app>
    <v-app-bar color="surface" flat border="b" height="56">
      <v-container class="d-flex align-center">
        <NuxtLink to="/" class="d-flex align-center text-decoration-none">
          <v-avatar color="primary" variant="tonal" rounded="lg" size="28" class="mr-2">
            <v-icon icon="mdi-barcode-scan" size="16" />
          </v-avatar>
          <span class="font-weight-bold text-body-2">{{ t('app.name') }}</span>
        </NuxtLink>
        <v-spacer />
        <InfThemeToggle />
      </v-container>
    </v-app-bar>

    <v-main>
      <v-container
        v-if="isContainerMode"
        :style="route.meta.width === 'semi-fluid' ? 'max-width: min(90vw, 1600px); width: 100%;' : ''"
        class="d-flex align-center justify-center"
        style="min-height: calc(100vh - 56px);"
      >
        <slot />
      </v-container>
      <slot v-else />
    </v-main>
  </v-app>
</template>

<script setup lang="ts">
import InfThemeToggle from '~/components/infra/InfThemeToggle.vue'

const { t } = useI18n()
const route = useRoute()

const isContainerMode = computed(() => ['container', 'semi-fluid', 'fluid'].includes(route.meta.width as string))
</script>
