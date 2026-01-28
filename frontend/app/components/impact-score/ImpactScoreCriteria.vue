<template>
  <section class="impact-score-criteria">
    <header class="impact-score-criteria__header">
      <v-chip
        class="impact-score-criteria__eyebrow"
        variant="tonal"
        color="accent-primary-highlight"
      >
        {{ t('impactScorePage.criteria.eyebrow') }}
      </v-chip>
      <h2 class="impact-score-criteria__title">
        {{ t('impactScorePage.criteria.title') }}
      </h2>
      <p class="impact-score-criteria__subtitle">
        {{ t('impactScorePage.criteria.subtitle') }}
      </p>
    </header>

    <v-row class="impact-score-criteria__grid" :gutter="16">
      <v-col
        v-for="criterion in criteria"
        :key="criterion.key"
        cols="12"
        sm="6"
        lg="4"
      >
        <v-card
          class="impact-score-criteria__card"
          elevation="0"
          rounded="xl"
          border
        >
          <div class="impact-score-criteria__card-top">
            <v-icon
              :icon="criterion.icon"
              size="28"
              color="accent-supporting"
            />
            <v-chip size="small" variant="flat" color="surface-primary-120">
              {{ criterion.weight }}
            </v-chip>
          </div>
          <h3 class="impact-score-criteria__card-title">
            {{ criterion.title }}
          </h3>
          <p class="impact-score-criteria__card-description">
            {{ criterion.description }}
          </p>
        </v-card>
      </v-col>
    </v-row>
  </section>
</template>

<script setup lang="ts">
const { t } = useI18n()

const criteria = computed(() => [
  {
    key: 'circularity',
    icon: 'mdi-recycle-variant',
    title: t('impactScorePage.criteria.items.circularity.title'),
    description: t('impactScorePage.criteria.items.circularity.description'),
    weight: t('impactScorePage.criteria.items.circularity.weight'),
  },
  {
    key: 'energy',
    icon: 'mdi-flash',
    title: t('impactScorePage.criteria.items.energy.title'),
    description: t('impactScorePage.criteria.items.energy.description'),
    weight: t('impactScorePage.criteria.items.energy.weight'),
  },
  {
    key: 'materials',
    icon: 'mdi-water-alert-outline',
    title: t('impactScorePage.criteria.items.materials.title'),
    description: t('impactScorePage.criteria.items.materials.description'),
    weight: t('impactScorePage.criteria.items.materials.weight'),
  },
  {
    key: 'logistics',
    icon: 'mdi-truck-outline',
    title: t('impactScorePage.criteria.items.logistics.title'),
    description: t('impactScorePage.criteria.items.logistics.description'),
    weight: t('impactScorePage.criteria.items.logistics.weight'),
  },
  {
    key: 'transparency',
    icon: 'mdi-shield-check',
    title: t('impactScorePage.criteria.items.transparency.title'),
    description: t('impactScorePage.criteria.items.transparency.description'),
    weight: t('impactScorePage.criteria.items.transparency.weight'),
  },
])
</script>

<style scoped>
.impact-score-criteria {
  display: grid;
  gap: 1.5rem;
}

.impact-score-criteria__header {
  display: grid;
  gap: 0.75rem;
}

.impact-score-criteria__eyebrow {
  width: fit-content;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  font-weight: 700;
}

.impact-score-criteria__title {
  margin: 0;
  font-size: clamp(1.6rem, 1.4vw + 1rem, 2rem);
  color: rgb(var(--v-theme-text-neutral-strong));
}

.impact-score-criteria__subtitle {
  margin: 0;
  color: rgb(var(--v-theme-text-neutral-secondary));
}

.impact-score-criteria__grid {
  margin-top: 0.5rem;
}

.impact-score-criteria__card {
  height: 100%;
  padding: 1.4rem;
  background: rgb(var(--v-theme-surface-glass));
  border-color: rgba(var(--v-theme-border-primary-strong), 0.28);
}

.impact-score-criteria__card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  margin-bottom: 0.9rem;
}

.impact-score-criteria__card-title {
  margin: 0;
  font-size: 1.1rem;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.impact-score-criteria__card-description {
  margin: 0.5rem 0 0;
  color: rgb(var(--v-theme-text-neutral-secondary));
  line-height: 1.55;
}
</style>
