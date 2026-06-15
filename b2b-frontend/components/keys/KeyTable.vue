<template>
  <InfDataTable
    :headers="headers"
    :items="rows as unknown as Record<string, unknown>[]"
    :items-length="total"
    :items-per-page="itemsPerPage"
    :loading="loading"
    :loading-text="t('keys.table.loading')"
    :no-data-text="t('keys.table.empty')"
    @options="emit('options', $event)"
  >
    <template #[`item.apiKeyId`]="{ item }">
      <div class="d-flex flex-column">
        <span class="font-weight-medium text-truncate inf-key-id">{{ item.name || item.apiKeyId }}</span>
        <span class="text-caption text-medium-emphasis">{{ item.keyPrefix || item.apiKeyId }}</span>
      </div>
    </template>
    <template v-if="showClient" #[`item.clientId`]="{ item }">
      <span class="text-body-2">{{ item.clientId }}</span>
    </template>
    <template #[`item.tier`]="{ item }">
      <v-chip color="primary" variant="tonal" size="small">{{ item.tier }}</v-chip>
    </template>
    <template #[`item.enabled`]="{ item }">
      <v-chip :color="item.enabled ? 'success' : 'error'" variant="tonal" size="small">
        {{ item.enabled ? t('keys.status.active') : t('keys.status.revoked') }}
      </v-chip>
    </template>
    <template #[`item.allowedModels`]="{ item }">
      <div class="d-flex ga-1 flex-wrap">
        <v-chip v-for="model in item.allowedModels.slice(0, 3)" :key="model" size="x-small" variant="tonal">{{ model }}</v-chip>
        <v-chip v-if="item.allowedModels.length > 3" size="x-small">
          {{ t('keys.table.more_models', { count: item.allowedModels.length - 3 }) }}
        </v-chip>
      </div>
    </template>
    <template #[`item.requests`]="{ item }">
      <span class="font-weight-medium">{{ n(Number(item.requests || 0)) }}</span>
    </template>
    <template #[`item.totalTokens`]="{ item }">
      <span>{{ n(Number(item.totalTokens || 0)) }}</span>
    </template>
    <template #[`item.totalCost`]="{ item }">
      <span class="font-weight-medium">
        {{ item.lifetimeSpendMinor ? formatMinor(item.lifetimeSpendMinor, item.spendLimitCurrency) : '—' }}
      </span>
    </template>
    <template #[`item.spend`]="{ item }">
      <div class="d-flex flex-column align-end">
        <span v-if="item.monthlySpendMinor || item.monthlySpendLimitMinor" class="font-weight-medium">
          {{ formatMinor(item.monthlySpendMinor, item.spendLimitCurrency) }}
        </span>
        <span class="text-caption text-medium-emphasis">
          {{ item.monthlySpendLimitMinor ? `/ ${formatMinor(item.monthlySpendLimitMinor, item.spendLimitCurrency)}` : t('keys.table.no_limit') }}
        </span>
        <v-chip v-if="item.spendAlertThreshold" :color="item.spendAlertThreshold >= 100 ? 'error' : 'warning'" size="x-small" variant="tonal" class="mt-1">
          {{ t('keys.table.spend_alert', { threshold: item.spendAlertThreshold }) }}
        </v-chip>
      </div>
    </template>
    <template v-if="hasActions" #[`item.actions`]="{ item }">
      <div class="d-flex justify-end ga-1">
        <v-tooltip v-if="showEdit" :text="t('keys.actions.edit')">
          <template #activator="{ props: tooltipProps }">
            <v-btn v-bind="tooltipProps" icon="mdi-pencil-outline" size="small" variant="text" color="primary" @click="emit('edit', item.apiKeyId)" />
          </template>
        </v-tooltip>
        <v-tooltip v-if="showAnalytics" :text="t('keys.actions.analytics')">
          <template #activator="{ props: tooltipProps }">
            <v-btn v-bind="tooltipProps" icon="mdi-chart-line" size="small" variant="text" @click="emit('analytics', item.apiKeyId)" />
          </template>
        </v-tooltip>
        <v-tooltip v-if="showLogs" :text="t('audit.actions.view_logs')">
          <template #activator="{ props: tooltipProps }">
            <v-btn v-bind="tooltipProps" icon="mdi-history" size="small" variant="text" @click="emit('logs', item.apiKeyId)" />
          </template>
        </v-tooltip>
        <v-tooltip v-if="showAuditPolicy" :text="t('audit.actions.edit_policy')">
          <template #activator="{ props: tooltipProps }">
            <v-btn v-bind="tooltipProps" icon="mdi-tune" size="small" variant="text" @click="emit('policy', item.apiKeyId)" />
          </template>
        </v-tooltip>
        <v-tooltip v-if="showRouting" :text="t('keys.actions.edit_routing')">
          <template #activator="{ props: tooltipProps }">
            <v-btn v-bind="tooltipProps" icon="mdi-routes" size="small" variant="text" @click="emit('routing', item.apiKeyId)" />
          </template>
        </v-tooltip>
        <v-tooltip v-if="showRotate" :text="t('keys.actions.rotate')">
          <template #activator="{ props: tooltipProps }">
            <v-btn v-bind="tooltipProps" icon="mdi-refresh" size="small" variant="text" @click="emit('rotate', item.apiKeyId)" />
          </template>
        </v-tooltip>
        <v-tooltip v-if="showRevoke" :text="t('keys.actions.revoke')">
          <template #activator="{ props: tooltipProps }">
            <v-btn v-bind="tooltipProps" icon="mdi-key-remove" size="small" variant="text" color="error" @click="emit('revoke', item.apiKeyId)" />
          </template>
        </v-tooltip>
        <v-tooltip v-if="showDelete" :text="t('keys.actions.delete')">
          <template #activator="{ props: tooltipProps }">
            <v-btn v-bind="tooltipProps" icon="mdi-delete-outline" size="small" variant="text" color="error" @click="emit('delete', item.apiKeyId)" />
          </template>
        </v-tooltip>
      </div>
    </template>
  </InfDataTable>
</template>

<script setup lang="ts">
import InfDataTable from '~/components/infra/InfDataTable.vue'
import type { KeyListItem } from '~/domains/keys/keys'

const props = withDefaults(defineProps<{
  rows: KeyListItem[]
  total: number
  loading?: boolean
  itemsPerPage?: number
  showClient?: boolean
  showEdit?: boolean
  showAnalytics?: boolean
  showLogs?: boolean
  showAuditPolicy?: boolean
  showRouting?: boolean
  showRotate?: boolean
  showRevoke?: boolean
  showDelete?: boolean
}>(), {
  itemsPerPage: 10,
  showClient: true,
  showEdit: false,
  showAnalytics: true,
  showLogs: true,
  showAuditPolicy: true,
  showRouting: true,
  showRotate: true,
  showRevoke: true,
  showDelete: true
})

const emit = defineEmits<{
  options: [Record<string, unknown>]
  edit: [string]
  analytics: [string]
  logs: [string]
  policy: [string]
  routing: [string]
  rotate: [string]
  revoke: [string]
  delete: [string]
}>()

const { t, n } = useI18n()

const hasActions = computed(() =>
  props.showEdit || props.showAnalytics || props.showLogs || props.showAuditPolicy || props.showRouting || props.showRotate || props.showRevoke || props.showDelete
)

const headers = computed(() => {
  const base = [
    { title: t('keys.fields.api_key_id'), key: 'apiKeyId', minWidth: 220 },
    ...(props.showClient ? [{ title: t('keys.fields.client_id'), key: 'clientId', minWidth: 160 }] : []),
    { title: t('keys.fields.tier'), key: 'tier', width: 120 },
    { title: t('keys.fields.allowed_models'), key: 'allowedModels', sortable: false, minWidth: 220 },
    { title: t('keys.fields.enabled'), key: 'enabled', width: 120 },
    { title: t('keys.fields.requests'), key: 'requests', align: 'end' as const, width: 120 },
    { title: t('keys.fields.total_tokens'), key: 'totalTokens', align: 'end' as const, width: 150 },
    { title: t('keys.fields.total_cost'), key: 'totalCost', sortable: false, align: 'end' as const, width: 130 },
    { title: t('keys.fields.monthly_spend'), key: 'spend', sortable: false, align: 'end' as const, width: 170 }
  ]
  return hasActions.value
    ? [...base, { title: t('keys.fields.actions'), key: 'actions', sortable: false, align: 'end' as const, width: 300 }]
    : base
})

function formatMinor(value: number | null | undefined, currency: string | null | undefined) {
  return n(Number(value || 0) / 100, { style: 'currency', currency: currency || 'EUR' })
}
</script>

<style scoped>
.inf-key-id {
  max-width: 220px;
}
</style>
