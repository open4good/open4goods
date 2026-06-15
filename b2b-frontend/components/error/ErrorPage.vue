<template>
  <div class="error-page" :data-theme="activeTheme">
    <!-- Background visual layer -->
    <div class="error-page__background" aria-hidden="true">
      <ClientOnly>
        <LandingHeroNetworkVisual
          :config="networkConfig"
          :theme="activeTheme"
          class="error-page__network"
        />
      </ClientOnly>
      <div class="error-page__overlay" />
    </div>

    <!-- Content layer -->
    <v-container class="error-page__container">
      <v-row justify="center" align="center" class="fill-height">
        <v-col cols="12" sm="10" md="8" lg="7">
          <div class="error-card-container">
            <!-- Large floating status code in background of the card -->
            <div class="error-status-bg" aria-hidden="true">
              {{ statusCode }}
            </div>

            <v-card class="error-card" variant="flat">
              <div class="error-card__content">
                <div class="error-card__badge-wrap">
                  <v-chip
                    color="primary"
                    variant="tonal"
                    size="small"
                    class="font-weight-bold mb-6 px-4"
                  >
                    <v-icon start icon="mdi-alert-circle-outline" size="small" />
                    NETWORK STATUS: {{ statusCode }}
                  </v-chip>
                </div>

                <h1 class="error-card__title text-h2 font-weight-black mb-4">
                  {{ title }}
                </h1>

                <p class="error-card__subtitle text-h6 font-weight-medium mb-10">
                  {{ subtitle }}
                </p>

                <div class="error-card__actions d-flex flex-column align-center ga-4">
                  <v-btn
                    to="/"
                    color="primary"
                    size="x-large"
                    variant="flat"
                    prepend-icon="mdi-home-variant"
                    class="error-card__btn rounded-xl px-12"
                    @click="$emit('clear')"
                  >
                    {{ t('error_pages.back_home') }}
                  </v-btn>

                  <v-btn
                    v-if="error?.stack && isDev"
                    variant="text"
                    color="primary"
                    size="small"
                    class="text-none"
                    :prepend-icon="showDetails ? 'mdi-chevron-up' : 'mdi-chevron-down'"
                    @click="showDetails = !showDetails"
                  >
                    {{ showDetails ? 'Hide technical diagnostics' : 'Show technical diagnostics' }}
                  </v-btn>
                </div>

                <v-expand-transition>
                  <div v-if="showDetails" class="error-card__details mt-8">
                    <div class="d-flex align-center justify-space-between mb-2">
                      <span class="text-caption font-weight-bold text-uppercase opacity-60">Stack Trace</span>
                      <v-btn icon="mdi-content-copy" variant="text" size="x-small" @click="copyStack" />
                    </div>
                    <pre class="error-card__stack">{{ error.stack }}</pre>
                  </div>
                </v-expand-transition>
              </div>
            </v-card>
          </div>
        </v-col>
      </v-row>
    </v-container>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import type { NuxtError } from '#app'
import type { NetworkDensity } from '~/components/landing/HeroNetworkVisual.vue'

const props = defineProps<{
  error: NuxtError
}>()

defineEmits(['clear'])

const { t } = useI18n()
const { activeTheme } = useThemePreference()
const isDev = import.meta.dev
const showDetails = ref(false)

const statusCode = computed(() => props.error?.statusCode || 500)

const title = computed(() => {
  if (statusCode.value === 404) return t('error_pages.404.title')
  if (statusCode.value >= 500) return t('error_pages.500.title')
  return t('error_pages.generic.title')
})

const subtitle = computed(() => {
  if (statusCode.value === 404) return t('error_pages.404.subtitle')
  if (statusCode.value >= 500) return t('error_pages.500.subtitle')
  return t('error_pages.generic.subtitle')
})

const copyStack = () => {
  if (props.error?.stack) {
    navigator.clipboard.writeText(props.error.stack)
  }
}

const networkConfig = computed(() => ({
  density: 'medium' as NetworkDensity,
  visual: {
    glow: 1.4,
    lineOpacity: 0.3,
    blur: 0.4
  },
  communication: {
    packetSpeed: 0.35,
    packetCount: 16
  }
}))
</script>

<style scoped lang="scss">
.error-page {
  position: relative;
  min-height: 100svh;
  width: 100%;
  display: flex;
  overflow: hidden;
  background-color: #06131f; // Force dark background for premium feel even during transition
  isolation: isolate;

  &__background {
    position: absolute;
    inset: 0;
    z-index: 0;
  }

  &__network {
    width: 100%;
    height: 100%;
  }

  &__overlay {
    position: absolute;
    inset: 0;
    background: radial-gradient(circle at center, transparent 0%, #06131f 100%);
    opacity: 0.7;
  }

  &__container {
    position: relative;
    z-index: 1;
    display: flex;
    flex-direction: column;
    justify-content: center;
  }
}

.error-card-container {
  position: relative;
  padding: 2rem;
}

.error-status-bg {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: clamp(12rem, 30vw, 24rem);
  font-weight: 900;
  line-height: 1;
  color: white;
  opacity: 0.03;
  pointer-events: none;
  z-index: 0;
  user-select: none;
  font-family: 'Inter', sans-serif;
  letter-spacing: -0.05em;
}

.error-card {
  position: relative;
  z-index: 1;
  background: rgba(15, 23, 42, 0.65) !important;
  backdrop-filter: blur(32px) saturate(200%);
  border: 1px solid rgba(255, 255, 255, 0.08) !important;
  border-radius: 48px !important;
  padding: clamp(2.5rem, 6vw, 5rem);
  text-align: center;
  box-shadow: 
    0 4px 24px -1px rgba(0, 0, 0, 0.2),
    0 24px 48px -8px rgba(0, 0, 0, 0.3) !important;
  transition: transform 0.6s cubic-bezier(0.16, 1, 0.3, 1);

  &::before {
    content: "";
    position: absolute;
    inset: 0;
    border-radius: inherit;
    padding: 1px;
    background: linear-gradient(135deg, rgba(255, 255, 255, 0.15), transparent 40%, transparent 60%, rgba(255, 255, 255, 0.1));
    -webkit-mask: linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0);
    mask: linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0);
    -webkit-mask-composite: xor;
    mask-composite: exclude;
    pointer-events: none;
  }

  &__title {
    background: linear-gradient(135deg, #60a5fa 0%, #a78bfa 50%, #f472b6 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    line-height: 1;
    letter-spacing: -0.02em;
  }

  &__subtitle {
    color: rgba(255, 255, 255, 0.6);
    line-height: 1.6;
    max-width: 520px;
    margin-inline: auto;
  }

  &__btn {
    text-transform: none;
    font-weight: 700;
    letter-spacing: 0.02em;
    transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1);
    background: linear-gradient(135deg, rgb(var(--v-theme-primary)) 0%, #6366f1 100%) !important;
    box-shadow: 0 16px 32px -8px rgba(var(--v-theme-primary), 0.5) !important;

    &:hover {
      transform: translateY(-6px);
      box-shadow: 0 24px 48px -12px rgba(var(--v-theme-primary), 0.6) !important;
    }
  }

  &__details {
    text-align: left;
    background: rgba(0, 0, 0, 0.4);
    border: 1px solid rgba(255, 255, 255, 0.05);
    border-radius: 20px;
    padding: 1.5rem;
    overflow: hidden;
  }

  &__stack {
    font-family: 'Fira Code', 'Cascadia Code', monospace;
    font-size: 0.75rem;
    overflow-x: auto;
    color: rgba(255, 255, 255, 0.45);
    line-height: 1.5;
    white-space: pre-wrap;
    word-break: break-all;
  }
}

// Light theme refinements
[data-theme="light"] {
  .error-page {
    background-color: #f7fbff;
    
    &__overlay {
      background: radial-gradient(circle at center, transparent 0%, #f7fbff 100%);
    }
  }

  .error-status-bg {
    color: #0b1720;
    opacity: 0.04;
  }

  .error-card {
    background: rgba(255, 255, 255, 0.75) !important;
    border-color: rgba(var(--v-theme-primary), 0.05) !important;
    box-shadow: 0 24px 48px -12px rgba(13, 110, 253, 0.15) !important;

    &::before {
      background: linear-gradient(135deg, rgba(var(--v-theme-primary), 0.2), transparent 50%);
    }

    &__title {
      background: linear-gradient(135deg, #0d3b8a 0%, #4a2d91 100%);
      -webkit-background-clip: text;
    }

    &__subtitle {
      color: #475569;
    }

    &__details {
      background: rgba(241, 245, 249, 0.8);
      border-color: rgba(15, 23, 42, 0.05);
    }

    &__stack {
      color: #64748b;
    }
  }
}
</style>
