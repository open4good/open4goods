<template>
  <v-app>
    <v-app-bar class="auth-bar" flat height="56">
      <div class="auth-bar__inner">
        <NuxtLink to="/" class="auth-bar__logo">
          <v-img
            :src="logoSrc"
            :alt="t('app.name')"
            height="24"
            width="88"
            contain
          />
        </NuxtLink>
        <v-spacer />
        <InfThemeToggle />
      </div>
    </v-app-bar>

    <v-main>
      <v-container
        v-if="isContainerMode"
        :class="containerClass"
        :fluid="route.meta.width === 'fluid'"
        class="auth-container"
      >
        <slot />
      </v-container>
      <slot v-else />
    </v-main>

  </v-app>
</template>

<script setup lang="ts">
import { useTheme } from 'vuetify'
import InfThemeToggle from '~/components/infra/InfThemeToggle.vue'

const { t } = useI18n()
const route = useRoute()
const vuetifyTheme = useTheme()

const logoSrc = computed(() =>
  vuetifyTheme.current.value.dark
    ? '/brand/logo/svg/infera_lockup_horizontal_reverse.svg'
    : '/brand/logo/svg/infera_lockup_horizontal_primary.svg'
)

const isContainerMode = computed(() => ['container', 'semi-fluid', 'fluid'].includes(route.meta.width as string))

const containerClass = computed(() => [
  route.meta.width === 'semi-fluid' ? 'auth-container--semi-fluid' : ''
])
</script>


<style scoped lang="scss">
.auth-bar {
  background: color-mix(in oklab, var(--inf-token-color-bg-base) 88%, transparent) !important;
  border-bottom: 1px solid color-mix(in oklab, var(--inf-token-color-line-subtle) 70%, transparent);
  backdrop-filter: blur(10px);
}

.auth-bar__inner {
  display: flex;
  align-items: center;
  width: 100%;
  padding: 0 24px;
}

.auth-bar__logo {
  text-decoration: none;
}

.auth-container {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: calc(100vh - 56px);
  padding: 0;
}

.auth-container--semi-fluid {
  max-width: min(90vw, 1600px);
  width: 100%;
  margin: 0 auto;
}

@media (max-width: 960px) {
  .auth-container--semi-fluid {
    max-width: 100%;
  }
}
</style>

