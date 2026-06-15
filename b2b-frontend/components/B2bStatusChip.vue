<template>
  <v-chip :color="resolved.color" :prepend-icon="resolved.icon" size="small" variant="tonal">
    {{ label || resolved.label }}
  </v-chip>
</template>

<script setup lang="ts">
const props = defineProps<{
  status: string
  label?: string
}>()

const statusMap: Record<string, { label: string; color: string; icon: string }> = {
  active: { label: 'Active', color: 'success', icon: 'mdi-check-circle-outline' },
  billable: { label: 'Billable', color: 'success', icon: 'mdi-credit-card-check-outline' },
  paid: { label: 'Paid', color: 'success', icon: 'mdi-receipt-text-check-outline' },
  pending: { label: 'Pending', color: 'warning', icon: 'mdi-clock-outline' },
  rotated: { label: 'Rotated', color: 'info', icon: 'mdi-refresh' },
  revoked: { label: 'Revoked', color: 'error', icon: 'mdi-key-remove' },
  suspended: { label: 'Suspended', color: 'error', icon: 'mdi-pause-circle-outline' },
  non_billable: { label: 'No data, no pay', color: 'info', icon: 'mdi-credit-card-off-outline' },
  failed: { label: 'Failed', color: 'error', icon: 'mdi-alert-circle-outline' }
}

const resolved = computed(() => statusMap[props.status] ?? {
  label: props.status,
  color: 'default',
  icon: 'mdi-circle-outline'
})
</script>
