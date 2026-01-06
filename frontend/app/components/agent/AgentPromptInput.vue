<template>
  <AgentPromptShell
    :title="templateName"
    :description="description"
    :prompt-templates="promptTemplates"
    :allow-template-editing="allowTemplateEditing"
    :selected-prompt-template-id="selectedPromptTemplateId"
    :attributes="attributes"
    :can-toggle-visibility="canToggleVisibility"
    :default-public="defaultPublic"
    :loading="loading"
    :fallback-mailto="fallbackMailto"
    :tags="tags"
    :allowed-roles="allowedRoles"
    :is-authorized="isAuthorized"
    :usage-limits="usageLimits"
    @submit="payload => emit('submit', payload)"
    @fallback-contact="payload => emit('fallback-contact', payload)"
    @cancel="emit('cancel')"
  >
    <template #title="{ title }">
      <v-icon icon="mdi-creation" class="mr-2" color="primary"></v-icon>
      {{ title }}
    </template>
  </AgentPromptShell>
</template>

<script setup lang="ts">
import AgentPromptShell, {
  type AgentPromptShellUsageLimits,
} from './AgentPromptShell.vue'

const _props = defineProps<{
  templateName: string
  description?: string
  promptTemplates: { id: string; title: string; content: string }[]
  allowTemplateEditing: boolean
  selectedPromptTemplateId?: string
  attributes?: import('~/shared/api-client').AgentAttributeDto[]
  canToggleVisibility?: boolean
  defaultPublic?: boolean
  loading?: boolean
  fallbackMailto?: string | null
  tags?: string[]
  allowedRoles?: string[]
  isAuthorized?: boolean
  usageLimits?: AgentPromptShellUsageLimits
}>()

const emit = defineEmits<{
  (
    e: 'submit',
    payload: {
      prompt: string
      promptVariantId: string
      isPrivate: boolean
      attributeValues: Record<string, unknown>
      captchaToken?: string
    }
  ): void
  (
    e: 'fallback-contact',
    payload: {
      prompt: string
      attributeValues: Record<string, unknown>
      captchaToken?: string
    }
  ): void
  (e: 'cancel'): void
}>()
</script>
