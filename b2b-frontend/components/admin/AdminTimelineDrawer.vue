<template>
  <v-navigation-drawer
    :model-value="open"
    location="right"
    width="440"
    temporary
    @update:model-value="emit('close')"
  >
    <v-toolbar density="comfortable">
      <v-toolbar-title>{{ title }}</v-toolbar-title>
      <v-spacer />
      <v-btn icon="mdi-close" variant="text" :aria-label="closeLabel" @click="emit('close')" />
    </v-toolbar>

    <v-list v-if="events.length" lines="three" class="py-2">
      <v-list-item v-for="event in events" :key="event.eventId" class="px-4">
        <template #prepend>
          <v-avatar color="primary" variant="tonal" size="34">
            <v-icon icon="mdi-history" size="18" />
          </v-avatar>
        </template>
        <template #title>{{ event.action }}</template>
        <template #subtitle>
          <span class="d-block">{{ event.summary }}</span>
          <span class="text-caption text-medium-emphasis">{{ event.happenedAt }} · {{ event.actor }}</span>
        </template>
      </v-list-item>
    </v-list>

    <v-empty-state
      v-else
      icon="mdi-timeline-clock-outline"
      :title="emptyTitle"
    />
  </v-navigation-drawer>
</template>

<script setup lang="ts">
import type { AdminAuditItem } from '~/domains/admin/users'

defineProps<{
  open: boolean
  title: string
  emptyTitle: string
  closeLabel: string
  events: AdminAuditItem[]
}>()

const emit = defineEmits<{ close: [] }>()
</script>
