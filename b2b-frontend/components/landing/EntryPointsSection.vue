<template>
  <section id="entry-points" class="section entry-points">
    <LandingRevealBlock>
      <v-container>
        <div class="entry-points__head landing-section__head">
          <v-chip
            class="mb-4"
            size="small"
            variant="tonal"
            color="primary"
            prepend-icon="mdi-sign-direction"
          >
            {{ t('landing.entryPoints.badge') }}
          </v-chip>
          <h2>{{ t('landing.entryPoints.title') }}</h2>
          <p class="section__subtitle mb-0">{{ t('landing.entryPoints.subtitle') }}</p>
          <div class="entry-points__actions">
            <v-btn
              to="/offres/entreprises"
              variant="flat"
              color="primary"
              prepend-icon="mdi-domain"
            >
              {{ t('landing.entryPoints.actions.offers') }}
            </v-btn>
            <v-btn
              to="/models"
              variant="tonal"
              color="primary"
              prepend-icon="mdi-cube-outline"
            >
              {{ t('landing.entryPoints.actions.models') }}
            </v-btn>
          </div>
        </div>

        <div class="entry-points__stack mt-8">
          <v-card
            v-for="(path, index) in entryPaths"
            :key="path.key"
            class="entry-path"
            variant="outlined"
            rounded="lg"
            hover
            :class="{
              'entry-path--offset': index % 2 === 1,
              'entry-path--govern': path.key === 'govern'
            }"
            :style="{ '--entry-index': `${index}` }"
          >
            <v-card-text>
              <div class="entry-path__signal">
                <v-avatar size="52" :color="path.color" variant="tonal" rounded="lg">
                  <v-icon :icon="path.icon" :color="path.color" size="28" />
                </v-avatar>
                <div class="entry-path__meta">
                  <v-chip size="x-small" variant="tonal" :color="path.color" label>
                    {{ t(`landing.entryPoints.paths.${path.key}.tag`) }}
                  </v-chip>
                  <h3>{{ t(`landing.entryPoints.paths.${path.key}.title`) }}</h3>
                  <p>{{ t(`landing.entryPoints.paths.${path.key}.kicker`) }}</p>
                </div>
              </div>

              <div class="entry-path__body">
                <p class="entry-path__description mb-0">
                  {{ t(`landing.entryPoints.paths.${path.key}.description`) }}
                </p>

                <v-list class="entry-path__list" density="compact" bg-color="transparent">
                  <v-list-item v-for="point in path.points" :key="point" class="px-0">
                    <template #prepend>
                      <v-icon
                        icon="mdi-check-circle-outline"
                        size="small"
                        :color="path.color"
                      />
                    </template>
                    <v-list-item-title>
                      {{ t(`landing.entryPoints.paths.${path.key}.points.${point}`) }}
                    </v-list-item-title>
                  </v-list-item>
                </v-list>
              </div>

              <div class="entry-path__aside">
                <div class="entry-path__chips">
                  <v-chip
                    v-for="chip in path.chips"
                    :key="chip"
                    size="small"
                    variant="tonal"
                    :color="path.color"
                  >
                    {{ t(`landing.entryPoints.paths.${path.key}.chips.${chip}`) }}
                  </v-chip>
                </div>
                <v-btn
                  :to="path.to"
                  :color="path.color"
                  variant="tonal"
                  append-icon="mdi-arrow-right"
                  class="entry-path__cta"
                >
                  {{ t(`landing.entryPoints.paths.${path.key}.cta`) }}
                </v-btn>
              </div>
            </v-card-text>
          </v-card>
        </div>
      </v-container>
    </LandingRevealBlock>
  </section>
</template>

<script setup lang="ts">
const { t } = useI18n()

const entryPaths = [
  {
    key: 'consume',
    icon: 'mdi-api',
    color: 'primary',
    to: '/offres/entreprises',
    points: ['1', '2', '3'],
    chips: ['openai', 'rag', 'policies']
  },
  {
    key: 'provide',
    icon: 'mdi-chip',
    color: 'success',
    to: '/offres/particuliers',
    points: ['1', '2', '3'],
    chips: ['pull', 'control', 'earn']
  },
  {
    key: 'federate',
    icon: 'mdi-transit-connection-variant',
    color: 'warning',
    to: '/offres/institutionnelles',
    points: ['1', '2', '3'],
    chips: ['territories', 'clouders', 'continuity']
  },
  {
    key: 'govern',
    icon: 'mdi-shield-check-outline',
    color: 'info',
    to: '/offres/entreprises',
    points: ['1', '2', '3'],
    chips: ['audit', 'privacy', 'costs']
  }
] as const
</script>

<style scoped lang="scss">
.entry-points {
  background:
    linear-gradient(180deg, rgb(var(--v-theme-background)) 0%, color-mix(in oklab, rgb(var(--v-theme-surface)) 70%, rgb(var(--v-theme-background))) 100%);
}

.entry-points__head {
  display: grid;
  justify-items: center;
}

.entry-points__head h2 {
  font-size: clamp(2rem, 4vw, 3.35rem);
  font-weight: 850;
  letter-spacing: 0;
  line-height: 1.05;
  margin: 0;
}

.entry-points__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: center;
  margin-top: 24px;
}

.entry-points__stack {
  display: grid;
  gap: clamp(16px, 2.4vw, 24px);
}

.entry-path {
  animation: entry-path-rise 520ms cubic-bezier(0.16, 1, 0.3, 1) both;
  animation-delay: calc(var(--entry-index) * 90ms);
  background:
    linear-gradient(135deg, color-mix(in oklab, rgb(var(--v-theme-surface)) 94%, transparent), color-mix(in oklab, rgb(var(--v-theme-surface)) 72%, transparent));
  border-color: color-mix(in oklab, var(--inf-token-color-line-subtle) 86%, transparent) !important;
  border-radius: 8px !important;
  justify-self: start;
  max-width: min(1180px, calc(100% - 48px));
  overflow: hidden;
  transition:
    transform 220ms ease,
    border-color 220ms ease,
    box-shadow 220ms ease;
  width: 100%;
}

.entry-path::before {
  background: linear-gradient(90deg, rgba(var(--v-theme-primary), 0.82), transparent);
  content: "";
  display: block;
  height: 3px;
}

.entry-path--offset {
  justify-self: end;
}

.entry-path :deep(.v-card-text) {
  align-items: stretch;
  display: grid;
  gap: clamp(18px, 3vw, 32px);
  grid-template-columns: minmax(220px, 0.72fr) minmax(0, 1.2fr) minmax(220px, 0.56fr);
  padding: clamp(22px, 3.4vw, 34px);
}

.entry-path:hover {
  border-color: color-mix(in oklab, rgb(var(--v-theme-primary)) 38%, var(--inf-token-color-line-subtle)) !important;
  box-shadow: 0 18px 48px color-mix(in oklab, rgb(var(--v-theme-primary)) 13%, transparent);
  transform: translateY(-4px) translateX(6px);
}

.entry-path--offset:hover {
  transform: translateY(-4px) translateX(-6px);
}

.entry-path__signal {
  align-content: start;
  display: grid;
  gap: 18px;
}

.entry-path__meta {
  display: grid;
  gap: 8px;
}

.entry-path__meta h3 {
  font-size: clamp(1.35rem, 2.4vw, 2.18rem);
  font-weight: 840;
  letter-spacing: 0;
  line-height: 1.08;
  margin: 0;
  overflow-wrap: anywhere;
}

.entry-path__meta p {
  color: var(--inf-token-color-text-secondary);
  line-height: 1.42;
  margin: 0;
}

.entry-path__description {
  color: var(--inf-token-color-text-secondary);
  font-size: 1.02rem;
  line-height: 1.66;
}

.entry-path__list {
  display: grid;
  gap: 8px;
  margin-top: 18px;
}

.entry-path__list :deep(.v-list-item) {
  align-items: flex-start;
  min-height: 0;
}

.entry-path__list :deep(.v-list-item__prepend) {
  align-self: flex-start;
  padding-top: 2px;
}

.entry-path__list :deep(.v-list-item-title) {
  color: var(--inf-token-color-text-primary);
  font-size: 0.95rem;
  line-height: 1.48;
  white-space: normal;
}

.entry-path__chips,
.entry-path__aside {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.entry-path__aside {
  align-content: space-between;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.entry-path__cta {
  border-radius: 8px !important;
  justify-content: space-between;
}

@keyframes entry-path-rise {
  from {
    opacity: 0;
    transform: translateY(16px) translateX(-18px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.entry-path--offset {
  animation-name: entry-path-rise-offset;
}

@keyframes entry-path-rise-offset {
  from {
    opacity: 0;
    transform: translateY(16px) translateX(18px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 959px) {
  .entry-path {
    justify-self: stretch;
    max-width: 100%;
  }

  .entry-path :deep(.v-card-text) {
    grid-template-columns: 1fr;
  }

  .entry-path:hover,
  .entry-path--offset:hover {
    transform: translateY(-4px);
  }

  .entry-path__aside {
    align-content: start;
    justify-content: flex-start;
  }
}

@media (prefers-reduced-motion: reduce) {
  .entry-path {
    animation: none;
    transition: none;
  }

  .entry-path:hover {
    transform: none;
  }
}
</style>
