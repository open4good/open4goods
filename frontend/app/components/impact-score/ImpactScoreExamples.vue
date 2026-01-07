<template>
  <section class="impact-score-examples">
    <header class="impact-score-examples__header">
      <v-chip class="impact-score-examples__eyebrow" color="accent-primary-highlight" variant="tonal">
        {{ t('impactScorePage.examples.eyebrow') }}
      </v-chip>
      <h2 class="impact-score-examples__title">{{ t('impactScorePage.examples.title') }}</h2>
      <p class="impact-score-examples__subtitle">{{ t('impactScorePage.examples.subtitle') }}</p>
    </header>

    <v-row :gutter="16">
      <v-col
        v-for="example in examples"
        :key="example.key"
        cols="12"
        md="6"
      >
        <v-card class="impact-score-examples__card" elevation="0" rounded="xl" border>
          <div class="impact-score-examples__card-header">
            <div>
              <p class="impact-score-examples__card-eyebrow">{{ example.eyebrow }}</p>
              <h3 class="impact-score-examples__card-title">{{ example.title }}</h3>
              <p class="impact-score-examples__card-text">{{ example.description }}</p>
            </div>
            <div class="impact-score-examples__score">
              <ImpactScore :score="example.score" :max="5" size="medium" />
            </div>
          </div>

          <div class="impact-score-examples__breakdown">
            <div
              v-for="item in example.breakdown"
              :key="item.label"
              class="impact-score-examples__pill"
            >
              <v-icon :icon="item.icon" size="20" color="accent-primary-highlight" />
              <div class="impact-score-examples__pill-content">
                <span class="impact-score-examples__pill-label">{{ item.label }}</span>
                <span class="impact-score-examples__pill-value">{{ item.value }}</span>
              </div>
            </div>
          </div>

          <p class="impact-score-examples__footer">{{ example.conclusion }}</p>
        </v-card>
      </v-col>
    </v-row>
  </section>
</template>

<script setup lang="ts">
import ImpactScore from '~/components/shared/ui/ImpactScore.vue'

const { t } = useI18n()

const examples = computed(() => [
  {
    key: 'television',
    eyebrow: t('impactScorePage.examples.television.eyebrow'),
    title: t('impactScorePage.examples.television.title'),
    description: t('impactScorePage.examples.television.description'),
    score: 4.2,
    breakdown: [
      {
        label: t('impactScorePage.examples.television.breakdown.energy'),
        value: '40%',
        icon: 'mdi-flash',
      },
      {
        label: t('impactScorePage.examples.television.breakdown.materials'),
        value: '25%',
        icon: 'mdi-water-alert-outline',
      },
      {
        label: t('impactScorePage.examples.television.breakdown.repairability'),
        value: '20%',
        icon: 'mdi-tools',
      },
      {
        label: t('impactScorePage.examples.television.breakdown.transport'),
        value: '15%',
        icon: 'mdi-truck-fast-outline',
      },
    ],
    conclusion: t('impactScorePage.examples.television.conclusion'),
  },
  {
    key: 'washingMachine',
    eyebrow: t('impactScorePage.examples.washingMachine.eyebrow'),
    title: t('impactScorePage.examples.washingMachine.title'),
    description: t('impactScorePage.examples.washingMachine.description'),
    score: 3.6,
    breakdown: [
      {
        label: t('impactScorePage.examples.washingMachine.breakdown.energy'),
        value: '35%',
        icon: 'mdi-flash',
      },
      {
        label: t('impactScorePage.examples.washingMachine.breakdown.water'),
        value: '25%',
        icon: 'mdi-water',
      },
      {
        label: t('impactScorePage.examples.washingMachine.breakdown.durability'),
        value: '25%',
        icon: 'mdi-shield-half-full',
      },
      {
        label: t('impactScorePage.examples.washingMachine.breakdown.logistics'),
        value: '15%',
        icon: 'mdi-ferry',
      },
    ],
    conclusion: t('impactScorePage.examples.washingMachine.conclusion'),
  },
])
</script>

<style scoped>
.impact-score-examples {
  display: grid;
  gap: 1.5rem;
}

.impact-score-examples__header {
  display: grid;
  gap: 0.75rem;
}

.impact-score-examples__eyebrow {
  width: fit-content;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  font-weight: 700;
}

.impact-score-examples__title {
  margin: 0;
  font-size: clamp(1.6rem, 1.4vw + 1rem, 2rem);
  color: rgb(var(--v-theme-text-neutral-strong));
}

.impact-score-examples__subtitle {
  margin: 0;
  color: rgb(var(--v-theme-text-neutral-secondary));
}

.impact-score-examples__card {
  height: 100%;
  padding: 1.4rem;
  background: rgb(var(--v-theme-surface-default));
  border-color: rgba(var(--v-theme-border-primary-strong), 0.26);
  display: grid;
  gap: 1rem;
}

.impact-score-examples__card-header {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 1rem;
  align-items: center;
}

.impact-score-examples__card-eyebrow {
  margin: 0;
  text-transform: uppercase;
  letter-spacing: 0.12em;
  font-size: 0.85rem;
  color: rgb(var(--v-theme-text-neutral-secondary));
  font-weight: 700;
}

.impact-score-examples__card-title {
  margin: 0;
  font-size: 1.2rem;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.impact-score-examples__card-text {
  margin: 0.35rem 0 0;
  color: rgb(var(--v-theme-text-neutral-secondary));
}

.impact-score-examples__score {
  display: grid;
  gap: 0.3rem;
  justify-items: end;
}

.impact-score-examples__breakdown {
  display: grid;
  gap: 0.75rem;
}

.impact-score-examples__pill {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 0.6rem;
  align-items: center;
  padding: 0.85rem 1rem;
  border-radius: 14px;
  background: rgba(var(--v-theme-surface-primary-080), 0.75);
}

.impact-score-examples__pill-content {
  display: flex;
  justify-content: space-between;
  gap: 0.75rem;
  width: 100%;
  align-items: center;
}

.impact-score-examples__pill-label {
  font-weight: 700;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.impact-score-examples__pill-value {
  font-weight: 600;
  color: rgb(var(--v-theme-accent-primary-highlight));
}

.impact-score-examples__footer {
  margin: 0;
  color: rgb(var(--v-theme-text-neutral-secondary));
  line-height: 1.5;
}

@media (max-width: 960px) {
  .impact-score-examples__card-header {
    grid-template-columns: 1fr;
    justify-items: start;
  }

  .impact-score-examples__score {
    justify-items: start;
  }
}
</style>
