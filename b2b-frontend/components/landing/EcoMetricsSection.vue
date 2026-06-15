<template>
  <section class="section">
    <LandingRevealBlock>
      <v-container>
        <v-card class="carbon-accounting mt-7" rounded="lg" variant="outlined">
          <v-card-text>
            <div class="carbon-accounting__copy">
              <v-chip
                class="mb-4"
                color="success"
                size="small"
                variant="tonal"
                prepend-icon="mdi-leaf-circle-outline"
              >
                {{ t('landing.metrics.carbon.badge') }}
              </v-chip>
              <h3>{{ t('landing.metrics.carbon.title') }}</h3>
              <p>{{ t('landing.metrics.carbon.description') }}</p>
            </div>

            <v-list class="carbon-accounting__list" density="compact" bg-color="transparent">
              <v-list-item v-for="point in carbonPoints" :key="point" class="px-0">
                <template #prepend>
                  <v-icon icon="mdi-check-decagram-outline" color="success" size="small" />
                </template>
                <v-list-item-title>
                  {{ t(`landing.metrics.carbon.points.${point}`) }}
                </v-list-item-title>
              </v-list-item>
            </v-list>

            <div class="carbon-accounting__badges">
              <v-chip
                v-for="badge in carbonBadges"
                :key="badge"
                color="success"
                size="small"
                variant="tonal"
              >
                {{ t(`landing.metrics.carbon.badges.${badge}`) }}
              </v-chip>
            </div>
          </v-card-text>
        </v-card>
      </v-container>
    </LandingRevealBlock>
  </section>
</template>

<script setup lang="ts">
const { t } = useI18n()

const _metrics = [
  { key: 'cost', icon: 'mdi-trending-down' },
  { key: 'commitment', icon: 'mdi-lock-open-variant-outline' },
  { key: 'datacenter', icon: 'mdi-leaf' }
]

const carbonPoints = ['usage', 'scope3', 'exports'] as const
const carbonBadges = ['csrd', 'scope3', 'audit'] as const
</script>

<style scoped lang="scss">
.metric-section__subtitle {
  color: var(--inf-token-color-text-secondary);
  line-height: 1.62;
  margin: 14px 0 0;
  max-width: 780px;
}

.metric__value {
  font-size: clamp(1.8rem, 4vw, 3rem);
  font-weight: 700;
}

.metric__label {
  color: var(--inf-token-color-text-secondary);
  font-size: 0.875rem;
}

.carbon-accounting {
  animation: carbon-accounting-rise 640ms cubic-bezier(0.16, 1, 0.3, 1) both;
  background:
    linear-gradient(135deg, color-mix(in oklab, rgb(var(--v-theme-success)) 10%, transparent), color-mix(in oklab, rgb(var(--v-theme-surface)) 84%, transparent));
  border-color: color-mix(in oklab, rgb(var(--v-theme-success)) 32%, var(--inf-token-color-line-subtle)) !important;
  border-radius: 8px !important;
  overflow: hidden;
  transition:
    border-color 220ms ease,
    box-shadow 220ms ease,
    transform 220ms ease;
}

.carbon-accounting:hover {
  border-color: color-mix(in oklab, rgb(var(--v-theme-success)) 52%, var(--inf-token-color-line-subtle)) !important;
  box-shadow: 0 18px 54px color-mix(in oklab, rgb(var(--v-theme-success)) 14%, transparent);
  transform: translateY(-4px);
}

.carbon-accounting :deep(.v-card-text) {
  align-items: flex-start;
  display: grid;
  gap: clamp(18px, 3vw, 28px);
  grid-template-columns: minmax(0, 1.25fr) minmax(280px, 0.8fr) auto;
  padding: clamp(22px, 4vw, 36px);
}

.carbon-accounting__copy h3 {
  font-size: clamp(1.55rem, 3vw, 2.45rem);
  font-weight: 840;
  letter-spacing: 0;
  line-height: 1.08;
  margin: 0;
}

.carbon-accounting__copy p {
  color: var(--inf-token-color-text-secondary);
  line-height: 1.66;
  margin: 14px 0 0;
}

.carbon-accounting__list {
  display: grid;
  gap: 10px;
}

.carbon-accounting__list :deep(.v-list-item) {
  align-items: flex-start;
  min-height: 0;
}

.carbon-accounting__list :deep(.v-list-item__prepend) {
  align-self: flex-start;
  padding-top: 2px;
}

.carbon-accounting__list :deep(.v-list-item-title) {
  color: var(--inf-token-color-text-primary);
  font-size: 0.96rem;
  line-height: 1.5;
  white-space: normal;
}

.carbon-accounting__badges {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
}

@keyframes carbon-accounting-rise {
  from {
    opacity: 0;
    transform: translateY(16px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 960px) {
  .carbon-accounting :deep(.v-card-text) {
    grid-template-columns: 1fr;
  }

  .carbon-accounting__badges {
    justify-content: flex-start;
  }
}

@media (prefers-reduced-motion: reduce) {
  .carbon-accounting {
    animation: none;
    transition: none;
  }

  .carbon-accounting:hover {
    transform: none;
  }
}
</style>
