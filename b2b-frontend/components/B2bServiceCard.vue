<template>
  <v-card
    :class="['b2b-service-card', { 'b2b-service-card--coming-soon': service.status === 'coming-soon' }]"
    variant="outlined"
    :to="service.status === 'live' ? `/solutions/${service.slug}` : undefined"
    :href="undefined"
  >
    <v-card-text class="pa-4">
      <div class="d-flex align-start justify-space-between mb-3">
        <v-avatar :color="service.status === 'live' ? 'primary' : 'secondary'" variant="tonal" rounded="lg" size="40">
          <v-icon :icon="service.icon" />
        </v-avatar>
        <div class="d-flex ga-1 flex-wrap justify-end">
          <v-chip
            v-if="service.status === 'live'"
            color="success"
            size="x-small"
            variant="tonal"
            prepend-icon="mdi-check-circle-outline"
          >
            {{ t('services.badge.live') }}
          </v-chip>
          <v-chip
            v-else
            color="secondary"
            size="x-small"
            variant="tonal"
            prepend-icon="mdi-clock-outline"
          >
            {{ t('services.badge.comingSoon') }}
          </v-chip>
          <v-chip v-if="service.credits > 0" size="x-small" variant="outlined">
            {{ service.credits }} {{ t('services.badge.credits') }}
          </v-chip>
          <v-chip v-else size="x-small" variant="outlined">
            {{ t('services.badge.free') }}
          </v-chip>
        </div>
      </div>

      <h3 class="text-subtitle-1 font-weight-bold mb-1">
        {{ t(`services.${service.slug}.name`) }}
      </h3>
      <p class="text-body-2 text-medium-emphasis mb-3 b2b-service-card__short">
        {{ t(`services.${service.slug}.short`) }}
      </p>

      <div class="d-flex ga-2 flex-wrap">
        <template v-if="service.status === 'live'">
          <v-btn
            color="primary"
            size="small"
            variant="tonal"
            :to="`/solutions/${service.slug}`"
            prepend-icon="mdi-arrow-right"
          >
            {{ t('services.cta.learn') }}
          </v-btn>
          <v-btn
            v-if="service.playgroundPath"
            color="primary"
            size="small"
            variant="text"
            :to="service.playgroundPath"
            prepend-icon="mdi-console-line"
          >
            {{ t('services.cta.playground') }}
          </v-btn>
        </template>
        <v-btn
          v-else
          :to="`/solutions/${service.slug}`"
          size="small"
          variant="text"
          prepend-icon="mdi-information-outline"
        >
          {{ t('services.cta.preview') }}
        </v-btn>
      </div>
    </v-card-text>
  </v-card>
</template>

<script setup lang="ts">
import type { ServiceDescriptor } from '~/domains/b2b/services'

defineProps<{
  service: ServiceDescriptor
}>()

const { t } = useI18n()
</script>

<style scoped>
.b2b-service-card {
  height: 100%;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}

.b2b-service-card:not(.b2b-service-card--coming-soon):hover {
  border-color: rgb(var(--v-theme-primary));
  box-shadow: 0 2px 12px rgba(var(--v-theme-primary), 0.12);
}

.b2b-service-card--coming-soon {
  opacity: 0.72;
}

.b2b-service-card__short {
  min-height: 2.8em;
}
</style>
