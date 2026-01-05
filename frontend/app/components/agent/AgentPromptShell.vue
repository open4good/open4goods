<template>
  <v-card elevation="2" class="mx-auto" max-width="800">
    <v-card-title class="headline d-flex align-center flex-wrap gap-2">
      <slot name="title" :title="title">
        <v-icon icon="mdi-creation" class="mr-2" color="primary"></v-icon>
        {{ title }}
      </slot>
    </v-card-title>

    <v-card-subtitle
      v-if="hasMeta"
      class="d-flex flex-wrap align-center gap-2 px-6"
    >
      <div
        v-if="hasTags"
        class="d-flex flex-wrap align-center gap-2"
        data-test="agent-tags"
      >
        <v-chip
          v-for="tag in tags"
          :key="tag"
          size="x-small"
          variant="outlined"
          color="secondary"
        >
          {{ tag }}
        </v-chip>
      </div>

      <v-chip
        v-if="usageLabel"
        size="x-small"
        color="secondary"
        variant="tonal"
        data-test="agent-usage"
      >
        {{ usageLabel }}
      </v-chip>
    </v-card-subtitle>

    <v-card-text>
      <v-alert
        type="info"
        variant="tonal"
        class="mb-4"
        closable
        icon="mdi-information"
      >
        {{ descriptionText }}
      </v-alert>

      <v-alert
        v-if="!isAuthorized"
        type="warning"
        variant="tonal"
        class="mb-4"
        icon="mdi-shield-alert"
        data-test="agent-locked"
      >
        {{ accessMessage }}
      </v-alert>

      <v-select
        v-if="promptTemplates.length > 0"
        v-model="selectedPromptTemplateId"
        :items="promptTemplates"
        item-title="title"
        item-value="id"
        :label="$t('agents.promptInput.templateLabel')"
        variant="outlined"
        density="comfortable"
        class="mb-4"
        :rules="[v => !!v || $t('agents.promptInput.templateRequired')]"
        :disabled="!isAuthorized"
        data-test="agent-template-select"
      />

      <v-textarea
        v-if="selectedPromptTemplate"
        v-model="promptTemplateContent"
        :label="$t('agents.promptInput.templateContent')"
        rows="4"
        auto-grow
        variant="outlined"
        :readonly="!allowTemplateEditing || !isAuthorized"
        :disabled="!allowTemplateEditing || !isAuthorized"
        class="mb-6"
        data-test="agent-template-content"
      />

      <div v-if="templateVariables.length > 0" class="mb-6">
        <v-text-field
          v-for="variable in templateVariables"
          :key="variable.id"
          v-model="templateVariableValues[variable.id]"
          :label="variable.label"
          :placeholder="variable.defaultValue || undefined"
          variant="outlined"
          density="comfortable"
          class="mb-3"
          :rules="variable.required ? [v => !!v || $t('agents.promptInput.required')] : []"
          :disabled="!isAuthorized"
          :data-test="`agent-template-variable-${variable.id}`"
        />
      </div>

      <v-textarea
        v-else
        v-model="prompt"
        :label="$t('agents.promptInput.label')"
        :placeholder="$t('agents.promptInput.placeholder')"
        rows="6"
        auto-grow
        variant="outlined"
        :rules="[v => !!v || $t('agents.promptInput.required')]"
        :disabled="!isAuthorized"
        data-test="agent-prompt"
      ></v-textarea>

      <div v-if="attributes && attributes.length > 0" class="mt-4">
        <h3 class="text-subtitle-1 mb-2 font-weight-bold">
          {{ $t('agents.promptInput.details') }}
        </h3>
        <AgentAttributeRenderer
          v-for="attr in attributes"
          :key="attr.id"
          v-model="attributeValues[attr.id]"
          :attribute="attr"
          :disabled="!isAuthorized"
        />
      </div>

      <v-checkbox
        v-if="canToggleVisibility"
        v-model="isPrivate"
        color="secondary"
        hide-details
        class="mt-2"
        :disabled="!isAuthorized"
      >
        <template #label>
          <div>
            <strong>{{ $t('agents.promptInput.private') }}</strong>
            <div class="text-caption">
              {{ $t('agents.promptInput.privateDescription') }}
            </div>
          </div>
        </template>
      </v-checkbox>

      <div class="d-flex justify-center mt-4">
        <ClientOnly>
          <VueHcaptcha
            v-if="siteKey"
            :sitekey="siteKey"
            @verify="onCaptchaVerify"
            @expired="onCaptchaExpired"
          />
        </ClientOnly>
      </div>

      <div
        v-if="fallbackMailto"
        class="mt-6 pa-4 bg-grey-lighten-4 rounded h-100"
      >
        <div class="d-flex align-center">
          <v-icon icon="mdi-email-outline" class="mr-2"></v-icon>
          <span class="text-body-2 font-weight-bold">
            {{ $t('agents.promptInput.email') }}
          </span>
        </div>
        <p class="text-caption mt-1 mb-2">
          {{ $t('agents.promptInput.emailDescription') }}
        </p>
        <v-btn
          variant="outlined"
          size="small"
          color="primary"
          :disabled="!isAuthorized"
          @click="emitFallbackContact"
        >
          {{ $t('agents.promptInput.openEmail') }}
        </v-btn>
      </div>
    </v-card-text>
    <v-divider></v-divider>
    <v-card-actions>
      <v-btn variant="text" :disabled="loading" @click="$emit('cancel')">
        {{ $t('agents.promptInput.back') }}
      </v-btn>
      <v-spacer></v-spacer>
      <v-btn
        color="primary"
        variant="flat"
        size="large"
        :loading="loading"
        :disabled="submitDisabled"
        data-test="agent-submit"
        @click="submit"
      >
        {{ $t('agents.promptInput.submit') }}
        <v-icon icon="mdi-send" end></v-icon>
      </v-btn>
    </v-card-actions>
  </v-card>
</template>

<script setup lang="ts">
import { computed, defineAsyncComponent, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import type { AgentAttributeDto } from '~/shared/api-client'
import AgentAttributeRenderer from './AgentAttributeRenderer.vue'

const VueHcaptcha = defineAsyncComponent(
  () => import('@hcaptcha/vue3-hcaptcha')
)

export interface AgentPromptShellUsageLimits {
  remaining?: number
  total?: number
}

const props = withDefaults(
  defineProps<{
    title: string
    description?: string
    promptTemplates?: { id: string; title: string; content: string }[]
    allowTemplateEditing: boolean
    selectedPromptTemplateId?: string
    attributes?: AgentAttributeDto[]
    canToggleVisibility?: boolean
    defaultPublic?: boolean
    loading?: boolean
    fallbackMailto?: string | null
    tags?: string[]
    allowedRoles?: string[]
    isAuthorized?: boolean
    usageLimits?: AgentPromptShellUsageLimits
  }>(),
  {
    description: undefined,
    promptTemplates: () => [],
    attributes: () => [],
    canToggleVisibility: false,
    defaultPublic: true,
    loading: false,
    fallbackMailto: null,
    tags: () => [],
    allowedRoles: () => [],
    isAuthorized: true,
    usageLimits: () => ({}),
    selectedPromptTemplateId: '',
  }
)

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

const { t } = useI18n()
const runtimeConfig = useRuntimeConfig()
const siteKey = computed(() => runtimeConfig.public.hcaptchaSiteKey ?? '')

const promptTemplates = computed(() => props.promptTemplates ?? [])
const selectedPromptTemplateId = ref(
  props.selectedPromptTemplateId || promptTemplates.value[0]?.id || ''
)
const selectedPromptTemplate = computed(() =>
  promptTemplates.value.find(tpl => tpl.id === selectedPromptTemplateId.value)
)
const promptTemplateContent = ref(selectedPromptTemplate.value?.content ?? '')

watch(
  () => props.selectedPromptTemplateId,
  newValue => {
    if (newValue) {
      selectedPromptTemplateId.value = newValue
    }
  }
)

watch(
  selectedPromptTemplate,
  tpl => {
    if (tpl) {
      promptTemplateContent.value = tpl.content
    }
  },
  { immediate: true }
)

watch(
  promptTemplates,
  newTemplates => {
    if (newTemplates.length > 0 && !selectedPromptTemplateId.value) {
      selectedPromptTemplateId.value = newTemplates[0].id
    }
  },
  { immediate: true }
)

type TemplateVariable = {
  id: string
  label: string
  defaultValue: string
  required: boolean
}

const prompt = ref('')
const templateVariableValues = ref<Record<string, string>>({})
const isPrivate = ref(props.defaultPublic === false)
const attributeValues = ref<Record<string, unknown>>({})
const captchaToken = ref<string | null>(null)

const descriptionText = computed(
  () => props.description ?? t('agents.promptInput.description')
)

const isAuthorized = computed(() => props.isAuthorized !== false)
const accessMessage = computed(() => {
  if (isAuthorized.value) return ''
  if (props.allowedRoles?.length) {
    return t('agents.promptInput.restrictedAccess', {
      roles: props.allowedRoles.join(', '),
    })
  }
  return t('agents.promptInput.locked')
})

const usageLabel = computed(() => {
  const remaining = props.usageLimits?.remaining
  const total = props.usageLimits?.total
  if (remaining == null && total == null) return ''
  if (remaining != null && total != null) {
    return t('agents.promptInput.usageLimits', { remaining, total })
  }
  if (remaining != null) {
    return t('agents.promptInput.usageRemaining', { remaining })
  }
  return ''
})

const hasTags = computed(() => (props.tags ?? []).length > 0)
const hasMeta = computed(() => hasTags.value || Boolean(usageLabel.value))

const onCaptchaVerify = (token: string) => {
  captchaToken.value = token
}

const onCaptchaExpired = () => {
  captchaToken.value = null
}

const templateVariables = computed(() => {
  if (!promptTemplateContent.value) {
    return [] as TemplateVariable[]
  }
  const variableRegex = /\{\{\s*([^{}]+?)\s*\}\}/g
  const seen = new Set<string>()
  const variables: TemplateVariable[] = []
  let match: RegExpExecArray | null
  while ((match = variableRegex.exec(promptTemplateContent.value))) {
    const raw = match[1].trim()
    if (!raw) continue
    const [namePart, ...defaultParts] = raw.split('=')
    const id = namePart.trim()
    if (!id || seen.has(id)) continue
    const defaultValue = defaultParts.join('=').trim()
    variables.push({
      id,
      label: id,
      defaultValue,
      required: defaultValue.length === 0,
    })
    seen.add(id)
  }
  return variables
})

watch(
  templateVariables,
  variables => {
    const nextValues: Record<string, string> = {}
    variables.forEach(variable => {
      const existing = templateVariableValues.value[variable.id]
      if (existing && existing.trim()) {
        nextValues[variable.id] = existing
      } else if (variable.defaultValue) {
        nextValues[variable.id] = variable.defaultValue
      } else {
        nextValues[variable.id] = ''
      }
    })
    templateVariableValues.value = nextValues
  },
  { immediate: true }
)

const renderedPrompt = computed(() => {
  if (templateVariables.value.length === 0) {
    return prompt.value
  }
  const template = promptTemplateContent.value ?? ''
  const variableRegex = /\{\{\s*([^{}]+?)\s*\}\}/g
  return template.replace(variableRegex, (_match, rawValue) => {
    const raw = String(rawValue).trim()
    const [namePart, ...defaultParts] = raw.split('=')
    const id = namePart.trim()
    const defaultValue = defaultParts.join('=').trim()
    const provided = templateVariableValues.value[id]
    return (provided && provided.trim()) || defaultValue || ''
  })
})

const isValid = computed(() => {
  const hasPrompt = !!renderedPrompt.value.trim()
  const hasTemplate = !!selectedPromptTemplateId.value
  const hasCaptcha = siteKey.value ? !!captchaToken.value : true
  return hasPrompt && hasTemplate && hasCaptcha && isAuthorized.value
})

const submitDisabled = computed(
  () => !isValid.value || props.loading || !isAuthorized.value
)

function submit() {
  if (!isValid.value) return
  emit('submit', {
    prompt: renderedPrompt.value,
    promptVariantId: selectedPromptTemplateId.value,
    isPrivate: isPrivate.value,
    attributeValues: attributeValues.value,
    captchaToken: captchaToken.value || undefined,
  })
}

function emitFallbackContact() {
  if (!renderedPrompt.value.trim()) {
    return
  }
  emit('fallback-contact', {
    prompt: renderedPrompt.value,
    attributeValues: attributeValues.value,
    captchaToken: siteKey.value ? captchaToken.value || undefined : undefined,
  })
}
</script>

<style scoped>
.gap-2 {
  gap: 0.5rem;
}
</style>
