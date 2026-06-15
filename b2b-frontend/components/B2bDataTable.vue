<template>
  <div class="b2b-data-table">
    <div v-if="$slots.filters" class="pa-3 b2b-data-table__filters">
      <slot name="filters" />
    </div>

    <v-data-table-server
      :headers="headers"
      :items="items"
      :items-length="itemsLength"
      :items-per-page="itemsPerPage"
      :loading="loading"
      :loading-text="loadingText"
      :no-data-text="noDataText"
      :items-per-page-options="itemsPerPageOptions"
      density="comfortable"
      fixed-header
      hover
      class="bg-transparent"
      @update:options="emit('options', $event)"
    >
      <template v-for="name in slotNames" :key="name" #[name]="slotProps">
        <slot :name="name" v-bind="slotProps" />
      </template>
    </v-data-table-server>
  </div>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  headers: Array<{ title: string; key: string; align?: 'start' | 'center' | 'end'; sortable?: boolean; width?: string | number; minWidth?: string | number }>
  items: Record<string, unknown>[]
  itemsLength: number
  itemsPerPage?: number
  loading?: boolean
  loadingText?: string
  noDataText?: string
}>(), {
  itemsPerPage: 10,
  loadingText: 'Loading data...',
  noDataText: 'No data found.'
})

const emit = defineEmits<{
  options: [Record<string, unknown>]
}>()

const slots = useSlots()
const slotNames = computed(() => Object.keys(slots).filter((name) => name !== 'filters'))

const itemsPerPageOptions = [
  { title: '10', value: 10 },
  { title: '25', value: 25 },
  { title: '50', value: 50 }
]
</script>

<style scoped>
.b2b-data-table {
  overflow: hidden;
  border: 1px solid rgba(var(--v-border-color), var(--v-border-opacity));
  border-radius: 8px;
}

.b2b-data-table__filters {
  border-bottom: 1px solid rgba(var(--v-border-color), var(--v-border-opacity));
}
</style>
