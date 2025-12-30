<template>
  <v-container class="py-16">
    <div class="text-center mb-8">
      <h2 class="text-h4 font-weight-bold mb-2">
        {{ $t('home.agent.title') }}
      </h2>
      <p class="text-body-1 text-medium-emphasis">
        {{ $t('home.agent.subtitle') }}
      </p>
    </div>

    <v-card
      v-if="template"
      max-width="800"
      class="mx-auto"
      elevation="0"
      border
    >
      <v-alert
        v-if="!isAuthorized"
        type="warning"
        variant="tonal"
        class="ma-4"
      >
        {{
          $t('agents.selector.notAuthorized', {
            roles: template.allowedRoles?.join(', ') || $t('agents.selector.noRoles'),
          })
        }}
      </v-alert>
      <AgentPromptInput
        :template-name="template.name"
        :prompt-templates="template.promptTemplates || []"
        :allow-template-editing="template.allowTemplateEditing"
        :attributes="template.attributes"
        :can-toggle-visibility="!template.publicPromptHistory"
        :default-public="template.publicPromptHistory"
        :loading="submitting"
        :tags="template.tags"
        :allowed-roles="template.allowedRoles"
        :is-authorized="isAuthorized"
        @submit="onSubmit"
      />
    </v-card>

    <!-- Success Dialog -->
    <v-dialog v-model="showSuccess" max-width="500">
      <v-card class="pa-4 text-center rounded-xl">
        <v-icon
          icon="mdi-check-circle"
          color="success"
          size="64"
          class="mb-4 mx-auto"
        ></v-icon>
        <h3 class="text-h5 mb-2">{{ $t('agents.submission.success') }}</h3>
        <p class="mb-6">
          {{
            $t('agents.submission.message', {
              id: submissionResult?.issueNumber,
            })
          }}
        </p>
        <v-btn color="primary" block @click="showSuccess = false">{{
          $t('common.close')
        }}</v-btn>
      </v-card>
    </v-dialog>
  </v-container>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useAgent } from '@/composables/useAgent'
import AgentPromptInput from '@/components/agent/AgentPromptInput.vue'
import type {
  AgentTemplateDto,
  AgentRequestDto,
  AgentRequestDtoTypeEnum,
  AgentRequestDtoPromptVisibilityEnum,
  AgentRequestResponseDto,
} from '~/shared/api-client'
import type { DomainLanguage } from '~~/shared/utils/domain-language'
import { useUserRoles } from '@/composables/auth/useUserRoles'
import { hasAdminAccess } from '~~/shared/utils/_roles'

const { locale } = useI18n()
const { listTemplates, submitRequest } = useAgent()
const currentLang = locale.value.split('-')[0] as DomainLanguage
const { roles } = useUserRoles()

const template = ref<AgentTemplateDto | null>(null)
const submitting = ref(false)
const showSuccess = ref(false)
const submissionResult = ref<AgentRequestResponseDto | null>(null)
const isAuthorized = computed(
  () =>
    !template.value?.allowedRoles?.length ||
    hasAdminAccess(roles.value, { allowedRoles: template.value?.allowedRoles })
)

onMounted(async () => {
  try {
    const templates = await listTemplates(currentLang)
    template.value = templates.find(t => t.id === 'question') || null
  } catch (e) {
    console.error('Failed to load agent template', e)
  }
})

async function onSubmit({
  prompt,
  promptVariantId,
  isPrivate,
  attributeValues,
  captchaToken,
}: {
  prompt: string
  promptVariantId?: string
  isPrivate: boolean
  attributeValues: Record<string, unknown>
  captchaToken?: string
}) {
  if (!template.value) return
  submitting.value = true
  try {
    const request: AgentRequestDto = {
      type: 'QUESTION' as unknown as AgentRequestDtoTypeEnum,
      promptUser: prompt,
      promptTemplateId: template.value.id,
      promptVariantId: promptVariantId ?? template.value.promptTemplates?.[0]?.id ?? '',
      promptVisibility: (isPrivate
        ? 'PRIVATE'
        : 'PUBLIC') as unknown as AgentRequestDtoPromptVisibilityEnum,
      attributeValues,
      captchaToken,
      tags: template.value.tags,
    }
    submissionResult.value = await submitRequest(request, currentLang)
    showSuccess.value = true
  } catch (e) {
    console.error('Submission failed', e)
  } finally {
    submitting.value = false
  }
}
</script>
