<template>
  <section class="impact-score-principles">
    <header class="impact-score-principles__header">
      <v-chip class="impact-score-principles__eyebrow" color="accent-primary-highlight" variant="tonal">
        {{ t('impactScorePage.principles.eyebrow') }}
      </v-chip>
      <h2 class="impact-score-principles__title">{{ t('impactScorePage.principles.title') }}</h2>
      <p class="impact-score-principles__subtitle">{{ t('impactScorePage.principles.subtitle') }}</p>
    </header>

    <v-row :gutter="16">
      <v-col
        v-for="principle in principles"
        :key="principle.key"
        cols="12"
        sm="6"
        lg="3"
      >
        <v-sheet class="impact-score-principles__card" rounded="xl" border elevation="0">
          <v-icon :icon="principle.icon" size="30" color="accent-primary-highlight" />
          <h3 class="impact-score-principles__card-title">{{ principle.title }}</h3>
          <p class="impact-score-principles__card-text">{{ principle.description }}</p>
        </v-sheet>
      </v-col>
    </v-row>
  </section>
</template>

<script setup lang="ts">
const { t } = useI18n()

const principles = computed(() => [
  {
    key: 'transparency',
    icon: 'mdi-open-source-initiative',
    title: t('impactScorePage.principles.items.transparency.title'),
    description: t('impactScorePage.principles.items.transparency.description'),
  },
  {
    key: 'category',
    icon: 'mdi-layers-outline',
    title: t('impactScorePage.principles.items.category.title'),
    description: t('impactScorePage.principles.items.category.description'),
  },
  {
    key: 'data',
    icon: 'mdi-database-check',
    title: t('impactScorePage.principles.items.data.title'),
    description: t('impactScorePage.principles.items.data.description'),
  },
  {
    key: 'pedagogy',
    icon: 'mdi-human-male-board',
    title: t('impactScorePage.principles.items.pedagogy.title'),
    description: t('impactScorePage.principles.items.pedagogy.description'),
  },
])
</script>

<style scoped>
.impact-score-principles {
  display: grid;
  gap: 1.4rem;
}

.impact-score-principles__header {
  display: grid;
  gap: 0.75rem;
}

.impact-score-principles__eyebrow {
  width: fit-content;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  font-weight: 700;
}

.impact-score-principles__title {
  margin: 0;
  font-size: clamp(1.6rem, 1.4vw + 1rem, 2rem);
  color: rgb(var(--v-theme-text-neutral-strong));
}

.impact-score-principles__subtitle {
  margin: 0;
  color: rgb(var(--v-theme-text-neutral-secondary));
}

.impact-score-principles__card {
  height: 100%;
  padding: 1.3rem;
  background: rgb(var(--v-theme-surface-default));
  border-color: rgba(var(--v-theme-border-primary-strong), 0.26);
  display: grid;
  gap: 0.65rem;
  align-content: start;
  box-shadow: 0 16px 50px rgba(15, 23, 42, 0.07);
}

.impact-score-principles__card-title {
  margin: 0;
  font-size: 1.05rem;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.impact-score-principles__card-text {
  margin: 0;
  color: rgb(var(--v-theme-text-neutral-secondary));
  line-height: 1.55;
}
</style>
