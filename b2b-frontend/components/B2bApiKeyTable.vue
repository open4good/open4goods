<template>
  <B2bDataTable
    :headers="headers"
    :items="keys"
    :items-length="itemsLength"
    :loading="loading"
    no-data-text="No API keys yet."
    @options="emit('options', $event)"
  >
    <template #[`item.name`]="{ item }">
      <div class="d-flex flex-column">
        <span class="font-weight-medium">{{ item.name }}</span>
        <span class="text-caption text-medium-emphasis">{{ item.prefix }}</span>
      </div>
    </template>

    <template #[`item.status`]="{ item }">
      <B2bStatusChip :status="String(item.status)" />
    </template>

    <template #[`item.actions`]="{ item }">
      <div class="d-flex justify-end ga-1">
        <v-btn icon="mdi-refresh" size="small" variant="text" @click="emit('rotate', String(item.id))" />
        <v-btn icon="mdi-key-remove" size="small" variant="text" color="error" @click="emit('revoke', String(item.id))" />
      </div>
    </template>
  </B2bDataTable>
</template>

<script setup lang="ts">
import B2bDataTable from '~/components/B2bDataTable.vue'
import B2bStatusChip from '~/components/B2bStatusChip.vue'

defineProps<{
  keys: Array<Record<string, unknown>>
  itemsLength: number
  loading?: boolean
}>()

const emit = defineEmits<{
  options: [Record<string, unknown>]
  rotate: [string]
  revoke: [string]
}>()

const headers = [
  { title: 'Key', key: 'name', minWidth: 220 },
  { title: 'Status', key: 'status', width: 140 },
  { title: 'Created', key: 'createdAt', minWidth: 160 },
  { title: 'Last used', key: 'lastUsedAt', minWidth: 160 },
  { title: 'Actions', key: 'actions', sortable: false, align: 'end' as const, width: 120 }
]
</script>
