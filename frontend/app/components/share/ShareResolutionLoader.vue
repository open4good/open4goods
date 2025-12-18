<template>
  <v-card class="share-loader" color="surface-glass" variant="flat">
    <v-card-text class="share-loader__content">
      <div class="share-loader__header">
        <v-progress-circular
          :size="64"
          :width="6"
          color="primary"
          indeterminate
          aria-hidden="true"
        />
        <div>
          <p class="share-loader__eyebrow">{{ t('share.loader.eyebrow') }}</p>
          <h1 class="share-loader__title">{{ t('share.loader.title') }}</h1>
          <p class="share-loader__subtitle">
            {{ statusLabel }}
          </p>
        </div>
      </div>

      <div class="share-loader__status">
        <v-progress-linear
          :model-value="progress"
          height="8"
          color="primary"
          rounded
          :aria-label="t('share.loader.progressAria')"
        />
        <p class="share-loader__helper">
          {{
            t('share.loader.helper', {
              seconds: Math.min(maxSeconds, Math.ceil(elapsedMs / 1000)),
            })
          }}
        </p>
        <p v-if="originUrl" class="share-loader__url" :title="originUrl">
          {{ originUrl }}
        </p>
        <p v-if="isStandalone" class="share-loader__hint">
          {{ t('share.loader.standaloneHint') }}
        </p>
        <p v-else class="share-loader__hint">
          {{ t('share.loader.browserHint') }}
        </p>
      </div>
    </v-card-text>
  </v-card>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { PropType } from 'vue'
import { useI18n } from 'vue-i18n'
import type { ShareResolutionStatus } from '~~/shared/api-client'

const props = defineProps({
  status: {
    type: String as PropType<ShareResolutionStatus | null>,
    default: null,
  },
  message: {
    type: String,
    default: null,
  },
  originUrl: {
    type: String,
    default: null,
  },
  elapsedMs: {
    type: Number,
    default: 0,
  },
  maxSeconds: {
    type: Number,
    default: 4,
  },
  isStandalone: {
    type: Boolean,
    default: false,
  },
})

const { t } = useI18n()

const statusLabel = computed(() => {
  if (props.status === 'TIMEOUT') {
    return t('share.loader.timeout')
  }
  if (props.status === 'ERROR') {
    return props.message ?? t('share.loader.error')
  }
  return t('share.loader.pending')
})

const progress = computed(() => {
  const ratio = Math.min(props.elapsedMs / (props.maxSeconds * 1000), 1)
  return Math.round(ratio * 100)
})
</script>

<style scoped>
.share-loader {
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.7);
  border-radius: 18px;
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.08);
}

.share-loader__content {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  padding: 1.5rem;
}

.share-loader__header {
  display: flex;
  gap: 1rem;
  align-items: center;
}

.share-loader__eyebrow {
  color: rgb(var(--v-theme-text-neutral-soft));
  text-transform: uppercase;
  letter-spacing: 0.08em;
  font-weight: 600;
  font-size: 0.85rem;
  margin: 0 0 0.25rem;
}

.share-loader__title {
  margin: 0;
  font-weight: 700;
  font-size: 1.2rem;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.share-loader__subtitle {
  margin: 0.25rem 0 0;
  color: rgb(var(--v-theme-text-neutral-secondary));
}

.share-loader__status {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.share-loader__helper {
  margin: 0;
  color: rgb(var(--v-theme-text-neutral-secondary));
  font-size: 0.95rem;
}

.share-loader__url {
  margin: 0;
  font-size: 0.9rem;
  color: rgb(var(--v-theme-text-neutral-strong));
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.share-loader__hint {
  margin: 0;
  font-size: 0.9rem;
  color: rgb(var(--v-theme-text-neutral-secondary));
}
</style>
