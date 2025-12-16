<template>
  <v-slide-y-transition>
    <v-sheet
      v-if="open"
      class="top-banner"
      :color="color"
      :elevation="elevation"
      rounded="0"
      border="none"
      role="region"
      :aria-label="ariaLabel"
    >
      <div class="top-banner__content">
        <div class="top-banner__text">
          <p class="top-banner__title">
            {{ message }}
          </p>
          <p v-if="subtitle" class="top-banner__subtitle">
            {{ subtitle }}
          </p>
        </div>

        <div class="top-banner__actions">
          <v-btn
            v-if="ctaLabel"
            class="top-banner__cta"
            :color="ctaColor"
            :variant="ctaVariant"
            :size="ctaSize"
            :href="ctaHref"
            :target="ctaTarget"
            :rel="ctaRel"
            :aria-label="ctaAriaLabel"
            @click="onCtaClick"
          >
            {{ ctaLabel }}
          </v-btn>

          <v-btn
            v-if="dismissible"
            icon
            variant="text"
            color="on-surface"
            size="small"
            :aria-label="closeLabel"
            @click="closeBanner"
          >
            <v-icon icon="mdi-close" size="18" />
          </v-btn>
        </div>
      </div>
    </v-sheet>
  </v-slide-y-transition>
</template>

<script setup lang="ts">
import { computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'

const props = withDefaults(
  defineProps<{
    open?: boolean
    message: string
    subtitle?: string | null
    color?: string
    elevation?: string | number
    ctaLabel?: string | null
    ctaColor?: string
    ctaVariant?: 'flat' | 'elevated' | 'tonal' | 'text' | 'outlined'
    ctaSize?: 'x-small' | 'small' | 'default' | 'large' | 'x-large'
    ctaHref?: string | null
    ctaTarget?: '_blank' | '_self' | '_parent' | '_top'
    ctaRel?: string | null
    dismissible?: boolean
    ariaLabel?: string
    ctaAriaLabel?: string
  }>(),
  {
    open: false,
    subtitle: null,
    color: 'surface-primary-080',
    elevation: 8,
    ctaLabel: null,
    ctaColor: 'primary',
    ctaVariant: 'elevated',
    ctaSize: 'small',
    ctaHref: null,
    ctaTarget: undefined,
    ctaRel: null,
    dismissible: false,
    ariaLabel: undefined,
    ctaAriaLabel: undefined,
  }
)

const emit = defineEmits<{
  (e: 'update:open', value: boolean): void
  (e: 'show' | 'hide' | 'cta-click' | 'close'): void
}>()

const { t } = useI18n()

const closeLabel = computed(() => t('ui.topBanner.close'))

const open = computed(() => props.open)

watch(
  () => props.open,
  (next, previous) => {
    if (next && !previous) {
      emit('show')
    } else if (!next && previous) {
      emit('hide')
    }
  },
  { immediate: true }
)

const updateVisibility = (value: boolean) => {
  if (value !== props.open) {
    emit('update:open', value)
  }
}

const closeBanner = () => {
  updateVisibility(false)
  emit('close')
}

const onCtaClick = () => {
  emit('cta-click')
}
</script>

<style scoped>
.top-banner {
  position: sticky;
  top: 0;
  inset-inline: 0;
  z-index: 30;
  padding: 0.75rem 1rem;
  background: rgba(var(--v-theme-hero-gradient-mid), 0.95);
  color: rgb(var(--v-theme-text-on-accent));
  box-shadow: 0 6px 24px rgba(var(--v-theme-shadow-primary-600), 0.18);
}

.top-banner__content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  max-width: 1440px;
  margin: 0 auto;
}

.top-banner__text {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  min-width: 0;
}

.top-banner__title {
  margin: 0;
  font-weight: 700;
  font-size: 0.95rem;
  line-height: 1.35;
}

.top-banner__subtitle {
  margin: 0;
  font-size: 0.85rem;
  color: rgba(var(--v-theme-text-on-accent), 0.88);
}

.top-banner__actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-shrink: 0;
}

.top-banner__cta {
  font-weight: 700;
  text-transform: none;
}

@media (max-width: 960px) {
  .top-banner {
    padding: 0.75rem 0.75rem;
  }

  .top-banner__content {
    flex-direction: column;
    align-items: flex-start;
  }

  .top-banner__actions {
    width: 100%;
    justify-content: flex-start;
  }

  .top-banner__cta {
    width: 100%;
  }
}
</style>
