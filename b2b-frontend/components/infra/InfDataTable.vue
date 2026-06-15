<template>
  <v-data-table-server
    :items="items"
    :headers="headers"
    :items-length="itemsLength"
    :items-per-page="itemsPerPage"
    :loading="loading"
    :loading-text="loadingText"
    :no-data-text="noDataText"
    :items-per-page-options="itemsPerPageOptions"
    density="comfortable"
    fixed-header
    hover
    class="bg-transparent inf-data-table"
    :row-props="rowProps"
    @update:options="onOptionsUpdate"
  >
    <template v-for="name in slotNames" :key="name" #[name]="slotProps">
      <slot :name="name" v-bind="slotProps" />
    </template>
  </v-data-table-server>
</template>

<script setup lang="ts">
const props = defineProps<{
  headers: Array<{ title: string; key: string; align?: 'start' | 'center' | 'end'; sortable?: boolean; width?: string | number; minWidth?: string | number }>
  items: Record<string, unknown>[]
  itemsLength: number
  itemsPerPage?: number
  loading?: boolean
  loadingText?: string
  noDataText?: string
  rowProps?: Record<string, unknown> | ((data: { item: Record<string, unknown>; index: number }) => Record<string, unknown>)
}>()

const emit = defineEmits<{
  options: [Record<string, unknown>]
}>()

const slots = useSlots()
const slotNames = computed(() => Object.keys(slots))

const itemsPerPage = computed(() => props.itemsPerPage ?? 10)
const itemsPerPageOptions = [
  { title: '10', value: 10 },
  { title: '25', value: 25 },
  { title: '50', value: 50 }
]

function onOptionsUpdate(options: Record<string, unknown>) {
  emit('options', options)
}
</script>

<style scoped>
.inf-data-table {
  border: 1px solid rgba(var(--v-border-color), var(--v-border-opacity));
  border-radius: 8px;
}
</style>
