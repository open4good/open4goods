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
          :class="{ 'agent-card--disabled': template.isAuthorized === false }"
          data-test="agent-template-card"
          @click="onSelect(template)"
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
            <v-card-title class="d-flex align-center gap-2">
              {{ template.name }}
              <v-chip
                v-if="template.isAuthorized === false"
                size="x-small"
                color="warning"
                variant="tonal"
                data-test="agent-template-locked"
              >
                {{ $t('agents.selector.restricted') }}
              </v-chip>
            </v-card-title>
            <v-card-subtitle
              v-if="template.allowedRoles?.length"
              class="text-caption"
            >
              {{
                $t('agents.selector.roles', {
                  roles: template.allowedRoles.join(', '),
                })
              }}
            </v-card-subtitle>
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
              >
                {{ tag }}
              </v-chip>
            </div>
          </v-card-text>

          <v-divider></v-divider>
          <v-card-actions>
            <v-btn
              variant="text"
              color="primary"
              :disabled="template.isAuthorized === false"
            >
              {{ $t('agents.selector.select') }}
            </v-btn>
            <v-spacer></v-spacer>
            <v-icon
              v-if="template.mailTemplate"
              icon="mdi-email-outline"
              :title="$t('agents.selector.mailTitle')"
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
import type { AgentTemplateDto } from '~~/shared/api-client/services/agents.services'

type AgentTemplateWithAccess = AgentTemplateDto & { isAuthorized?: boolean }

const _props = defineProps<{
  templates: AgentTemplateWithAccess[]
}>()

const emit = defineEmits<{
  (e: 'select' | 'blocked', template: AgentTemplateWithAccess): void
}>()

function onSelect(template: AgentTemplateWithAccess) {
  if (template.isAuthorized === false) {
    emit('blocked', template)
    return
  }
  emit('select', template)
}
</script>

<style scoped>
.agent-card--disabled {
  opacity: 0.6;
  cursor: not-allowed;
  border: 1px dashed rgba(0, 0, 0, 0.12);
}
</style>
