<template>
  <section class="impact-score-docs">
    <div class="impact-score-docs__header">
      <h2 class="text-h4 font-weight-bold">
        {{ t('impactScorePage.docs.title') }}
      </h2>
      <p class="text-body-1 text-medium-emphasis">
        {{ t('impactScorePage.docs.description') }}
      </p>
    </div>

    <DocCardsBrowser
      :prefilter-tags="['impact-score']"
      group-by="directory"
      sort="weight"
      :search-enabled="true"
      :tag-filter-enabled="true"
    />
  </section>
</template>

<script setup lang="ts">
import DocCardsBrowser from '~/components/docs/DocCardsBrowser.vue'

const { t } = useI18n()
</script>

<style scoped>
.impact-score-docs {
  display: grid;
  gap: 1.5rem;
}

.impact-score-docs__header {
  display: grid;
  gap: 0.5rem;
}
</style>
