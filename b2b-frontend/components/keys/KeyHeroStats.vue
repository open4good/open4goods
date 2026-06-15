<template>
  <v-row class="inf-gap-sm">
    <v-col v-for="metric in metrics" :key="metric.key" cols="12" sm="6" lg="3">
      <AdminMetricCard
        :label="metric.label"
        :value="metric.value"
        :caption="metric.caption"
        :icon="metric.icon"
        :color="metric.color"
      />
    </v-col>
  </v-row>
</template>

<script setup lang="ts">
import AdminMetricCard from '~/components/admin/AdminMetricCard.vue'
import type { KeySummary } from '~/domains/keys/keys'

const props = defineProps<{
  summary: KeySummary
}>()

const { t, n } = useI18n()

const metrics = computed(() => [
  {
    key: 'total',
    label: t('keys.hero.total_keys'),
    value: n(props.summary.totalKeys),
    caption: t('keys.hero.total_caption'),
    icon: 'mdi-key-chain',
    color: 'primary'
  },
  {
    key: 'active',
    label: t('keys.hero.active_keys'),
    value: n(props.summary.activeKeys),
    caption: t('keys.hero.active_caption'),
    icon: 'mdi-shield-key-outline',
    color: 'success'
  },
  {
    key: 'captured',
    label: t('keys.hero.captured'),
    value: n(props.summary.capturedRequests),
    caption: t('keys.hero.captured_caption'),
    icon: 'mdi-radar',
    color: 'info'
  },
  {
    key: 'tokens',
    label: t('keys.hero.tokens'),
    value: n(props.summary.totalTokens),
    caption: t('keys.hero.tokens_caption'),
    icon: 'mdi-counter',
    color: 'warning'
  }
])
</script>
