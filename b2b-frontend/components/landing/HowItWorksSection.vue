<template>
  <section id="how" class="section">
    <LandingRevealBlock>
      <v-container>
        <div class="how__head landing-section__head">
          <div>
            <h2>{{ t('landing.how.title') }}</h2>
            <p class="section__subtitle mb-0">{{ t('landing.how.subtitle') }}</p>
          </div>
          <v-chip size="small" variant="tonal" color="primary">
            {{ t('landing.how.badge') }}
          </v-chip>
        </div>

        <v-row class="mt-8" density="comfortable">
          <v-col v-for="step in steps" :key="step.key" cols="12" md="4">
            <v-card class="landing-card how-card fill-height" variant="outlined" rounded="xl">
              <v-card-item>
                <template #prepend>
                  <v-avatar size="40" color="primary" variant="tonal">
                    <v-icon :icon="step.icon" color="primary" />
                  </v-avatar>
                </template>
                <template #append>
                  <div class="how-card__index">0{{ step.index }}</div>
                </template>
                <v-card-title class="text-h6">{{ t(step.labelKey) }}</v-card-title>
              </v-card-item>
              <v-card-text>
                <p class="how-card__description">{{ t(`landing.how.steps.${step.key}.description`) }}</p>
                <v-list density="compact" bg-color="transparent">
                  <v-list-item v-for="point in step.points" :key="point" class="px-0">
                    <template #prepend>
                      <v-icon icon="mdi-arrow-right-thin" color="primary" />
                    </template>
                    <v-list-item-title>{{ t(`landing.how.steps.${step.key}.points.${point}`) }}</v-list-item-title>
                  </v-list-item>
                </v-list>
              </v-card-text>
            </v-card>
          </v-col>
        </v-row>
      </v-container>
    </LandingRevealBlock>
  </section>
</template>

<script setup lang="ts">
const { t } = useI18n()

const steps = [
  { key: 'client', labelKey: 'landing.how.client', icon: 'mdi-code-json', index: 1, points: ['1', '2'] },
  { key: 'router', labelKey: 'landing.how.router', icon: 'mdi-router-network', index: 2, points: ['1', '2'] },
  { key: 'network', labelKey: 'landing.how.network', icon: 'mdi-chip', index: 3, points: ['1', '2'] }
]
</script>

<style scoped lang="scss">
.how__head {
  display: grid;
  gap: 16px;
  justify-items: center;
}

.how-card {
  border-color: var(--inf-token-color-line-subtle) !important;
  background:
    linear-gradient(180deg, rgba(var(--v-theme-surface), 0.76), rgba(var(--v-theme-surface), 0.48));
}

.how-card__index {
  color: var(--inf-token-color-text-secondary);
  font-size: 0.875rem;
  font-weight: 600;
}

.how-card__description {
  color: var(--inf-token-color-text-secondary);
}

@media (max-width: 959px) {
  .how__head {
    justify-items: center;
  }
}
</style>
