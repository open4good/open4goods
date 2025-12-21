<template>
  <v-container>
    <v-row>
      <v-col
        v-for="template in templates"
        :key="template.id"
        cols="12"
        md="6"
        lg="4"
      >
        <v-card
          hover
          class="h-100 d-flex flex-column"
          @click="$emit('select', template)"
        >
          <v-card-item>
            <template #prepend>
              <v-icon
                v-if="template.icon"
                :icon="template.icon"
                size="large"
                color="primary"
              ></v-icon>
              <v-icon
                v-else
                icon="mdi-robot"
                size="large"
                color="secondary"
              ></v-icon>
            </template>
            <v-card-title>{{ template.name }}</v-card-title>
          </v-card-item>

          <v-card-text class="flex-grow-1">
            <p class="mb-4">{{ template.description }}</p>
            <div
              v-if="template.tags && template.tags.length > 0"
              class="d-flex flex-wrap gap-2"
            >
              <v-chip
                v-for="tag in template.tags"
                :key="tag"
                size="x-small"
                variant="flat"
                color="surface-variant"
                >{{ tag }}</v-chip
              >
            </div>
          </v-card-text>

          <v-divider></v-divider>
          <v-card-actions>
            <v-btn variant="text" color="primary">Select</v-btn>
            <v-spacer></v-spacer>
            <v-icon
              v-if="template.mailTemplate"
              icon="mdi-email-outline"
              title="Includes Email Fallback"
              size="small"
              class="mr-2"
            ></v-icon>
          </v-card-actions>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>

<script setup lang="ts">
import type { AgentTemplateDto } from '@/types/agent'

defineProps<{
  templates: AgentTemplateDto[]
}>()

defineEmits<{
  (e: 'select', template: AgentTemplateDto): void
}>()
</script>
