<template>
  <div class="impact-score-not-rated" role="status">
    <div class="impact-score-not-rated__title">
      {{ t('components.impactScore.v2.notRated') }}
    </div>
    <p class="impact-score-not-rated__reason">{{ reason }}</p>
    <NuxtLink
      v-if="methodologyHref"
      :to="methodologyHref"
      class="impact-score-not-rated__link"
    >
      {{ t('components.impactScore.v2.howCalculated') }}
    </NuxtLink>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { PropType } from 'vue'
import { useI18n } from 'vue-i18n'
import type { ProductImpactScoreStatusDto } from '~~/shared/api-client'
import { resolveNotRatedReasonKey } from '~/utils/_impact-score-v2'

const props = defineProps({
  status: {
    type: Object as PropType<ProductImpactScoreStatusDto>,
    required: true,
  },
  methodologyHref: {
    type: String,
    default: '',
  },
})

const { t } = useI18n()

// Neutral grey, no gauge, never a "low" level: the reason is written out.
const reason = computed(() =>
  resolveNotRatedReasonKey(props.status) === 'coverage'
    ? t('components.impactScore.v2.notRatedCoverage', {
        count: props.status.realCriteriaCount ?? 0,
        total: props.status.totalCriteriaCount ?? 0,
      })
    : t('components.impactScore.v2.notRatedRecalculating')
)
</script>

<style scoped>
.impact-score-not-rated {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 14px 16px;
  border-radius: 22px;
  border: 1px solid rgba(var(--v-theme-on-surface), 0.2);
  background: rgba(var(--v-theme-on-surface), 0.05);
  color: rgba(var(--v-theme-on-surface), 0.75);
  text-align: center;
}

.impact-score-not-rated__title {
  font-size: 1.25rem;
  font-weight: 700;
}

.impact-score-not-rated__reason {
  margin: 0;
  font-size: 0.8rem;
}

.impact-score-not-rated__link {
  font-size: 0.75rem;
  color: rgb(var(--v-theme-primary));
  text-decoration: underline;
}
</style>
