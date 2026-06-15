<template>
  <v-chip :color="chipColor" size="small" variant="tonal">
    {{ label }}
  </v-chip>
</template>

<script setup lang="ts">
const props = defineProps<{
  status: string
}>()

const { t } = useI18n()

const chipColor = computed(() => {
  if (props.status === 'UP') {
    return 'success'
  }

  if (props.status === 'DOWN' || props.status === 'OUT_OF_SERVICE') {
    return 'error'
  }

  if (props.status === 'DEGRADED') {
    return 'warning'
  }

  return 'info'
})

const label = computed(() => t(`health.status_values.${props.status}`))
</script>
