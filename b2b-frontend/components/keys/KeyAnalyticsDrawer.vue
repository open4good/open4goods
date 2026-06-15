<template>
  <v-navigation-drawer :model-value="open" location="right" width="720" temporary @update:model-value="emit('close')">
    <v-toolbar density="comfortable">
      <v-toolbar-title>{{ t('keys.analytics.title') }}</v-toolbar-title>
      <v-spacer />
      <v-btn icon="mdi-close" variant="text" @click="emit('close')" />
    </v-toolbar>

    <v-container v-if="analytics" fluid>
      <v-row>
        <v-col cols="12" md="4">
          <v-card>
            <v-card-title>{{ t('keys.fields.requests') }}</v-card-title>
            <v-card-text class="text-h5">{{ analytics.summary.requests }}</v-card-text>
          </v-card>
        </v-col>
        <v-col cols="12" md="4">
          <v-card>
            <v-card-title>{{ t('keys.fields.captured') }}</v-card-title>
            <v-card-text class="text-h5">{{ analytics.summary.captured }}</v-card-text>
          </v-card>
        </v-col>
        <v-col cols="12" md="4">
          <v-card>
            <v-card-title>{{ t('keys.fields.total_tokens') }}</v-card-title>
            <v-card-text class="text-h5">{{ n(analytics.summary.totalTokens) }}</v-card-text>
          </v-card>
        </v-col>
      </v-row>

      <v-card class="mt-4">
        <v-card-title>{{ t('keys.analytics.timeseries') }}</v-card-title>
        <v-card-text>
          <v-sparkline
            :model-value="analytics.timeseries.map(item => item.totalTokens)"
            auto-draw
            smooth
            color="primary"
            line-width="2"
          />
        </v-card-text>
      </v-card>

      <v-card class="mt-4">
        <v-card-title>{{ t('keys.analytics.by_model') }}</v-card-title>
        <v-card-text>
          <v-list density="compact">
            <v-list-item v-for="item in analytics.byModel" :key="item.key">
              <template #title>{{ item.key }}</template>
              <template #subtitle>{{ t('keys.fields.requests') }}: {{ item.requests }}</template>
              <template #append>{{ n(item.totalTokens) }}</template>
            </v-list-item>
          </v-list>
        </v-card-text>
      </v-card>

      <v-card class="mt-4">
        <v-card-title>{{ t('keys.analytics.ledger') }}</v-card-title>
        <v-card-text>
          <v-table density="compact">
            <thead>
              <tr>
                <th>{{ t('keys.fields.request_id') }}</th>
                <th>{{ t('keys.fields.model') }}</th>
                <th>{{ t('keys.fields.status') }}</th>
                <th>{{ t('keys.fields.total_tokens') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in analytics.ledger" :key="row.requestId">
                <td>{{ row.requestId }}</td>
                <td>{{ row.modelId || '-' }}</td>
                <td>{{ row.billingStatus }}</td>
                <td>{{ n(row.totalTokens) }}</td>
              </tr>
            </tbody>
          </v-table>
        </v-card-text>
      </v-card>
    </v-container>
  </v-navigation-drawer>
</template>

<script setup lang="ts">
import type { KeyAnalytics } from '~/domains/keys/keys'

defineProps<{
  open: boolean
  analytics: KeyAnalytics | null
}>()

const emit = defineEmits<{ close: [] }>()

const { t, n } = useI18n()
</script>
