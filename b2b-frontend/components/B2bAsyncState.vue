<template>
  <div>
    <v-sheet v-if="loading" class="b2b-async-state pa-6 text-center" rounded="lg">
      <v-progress-circular indeterminate color="primary" class="mb-4" />
      <p class="text-body-2 text-medium-emphasis mb-0">{{ t('common.loading') }}</p>
    </v-sheet>

    <v-sheet v-else-if="error" class="b2b-async-state pa-6 text-center" rounded="lg">
      <v-avatar color="error" variant="tonal" size="52" class="mb-4">
        <v-icon icon="mdi-alert-circle-outline" size="28" />
      </v-avatar>
      <h2 class="text-h6 font-weight-bold mb-2">{{ t('common.errorGeneric') }}</h2>
      <p v-if="error.i18nKey" class="text-body-2 text-medium-emphasis mb-4 b2b-async-state__description">
        {{ t(error.i18nKey) }}
      </p>
      <v-btn variant="tonal" prepend-icon="mdi-refresh" @click="emit('retry')">
        {{ t('common.retry') }}
      </v-btn>
    </v-sheet>

    <template v-else>
      <slot />
    </template>
  </div>
</template>

<script setup lang="ts">
import type { AppApiError } from '~/composables/useApiClient'

defineProps<{
  loading: boolean
  error: AppApiError | null
}>()

const emit = defineEmits<{ retry: [] }>()
const { t } = useI18n()
</script>

<style scoped>
.b2b-async-state {
  border: 1px solid rgba(var(--v-border-color), var(--v-border-opacity));
}

.b2b-async-state__description {
  max-width: 560px;
  margin-inline: auto;
}
</style>
