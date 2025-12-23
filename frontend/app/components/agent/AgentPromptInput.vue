<template>
  <v-card elevation="2" class="mx-auto" max-width="800">
    <v-card-title class="headline">
      <v-icon icon="mdi-creation" class="mr-2" color="primary"></v-icon>
      {{ templateName }}
    </v-card-title>
  <v-card-text>
    <v-alert
      type="info"
      variant="tonal"
      class="mb-4"
      closable
      icon="mdi-information"
    >
      {{ $t('agents.promptInput.description') }}
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
    />

    <v-textarea
      v-if="selectedPromptTemplate"
      v-model="promptTemplateContent"
      :label="$t('agents.promptInput.templateContent')"
      rows="4"
      auto-grow
      variant="outlined"
      :readonly="!allowTemplateEditing"
      :disabled="!allowTemplateEditing"
      class="mb-6"
    />

    <v-textarea
      v-model="prompt"
      :label="$t('agents.promptInput.label')"
      :placeholder="$t('agents.promptInput.placeholder')"
      rows="6"
        auto-grow
        variant="outlined"
        :rules="[v => !!v || $t('agents.promptInput.required')]"
      ></v-textarea>

      <!-- Custom Attributes -->
      <div v-if="attributes && attributes.length > 0" class="mt-4">
        <h3 class="text-subtitle-1 mb-2 font-weight-bold">
          {{ $t('agents.promptInput.details') }}
        </h3>
        <AgentAttributeRenderer
          v-for="attr in attributes"
          :key="attr.id"
          v-model="attributeValues[attr.id]"
          :attribute="attr"
        />
      </div>

      <v-checkbox
        v-if="canToggleVisibility"
        v-model="isPrivate"
        color="secondary"
        hide-details
        class="mt-2"
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

      <!-- Captcha -->
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
          <span class="text-body-2 font-weight-bold">{{
            $t('agents.promptInput.email')
          }}</span>
        </div>
        <p class="text-caption mt-1 mb-2">
          {{ $t('agents.promptInput.emailDescription') }}
        </p>
        <v-btn
          :href="fallbackMailto"
          target="_blank"
          variant="outlined"
          size="small"
          color="primary"
          >{{ $t('agents.promptInput.openEmail') }}</v-btn
        >
      </div>
    </v-card-text>
    <v-divider></v-divider>
    <v-card-actions>
      <v-btn variant="text" @click="$emit('cancel')">Back</v-btn>
      <v-spacer></v-spacer>
      <v-btn
        color="primary"
        variant="flat"
        size="large"
        :loading="loading"
        :disabled="!isValid"
        @click="submit"
      >
        Submit Request
        <v-icon icon="mdi-send" end></v-icon>
      </v-btn>
    </v-card-actions>
  </v-card>
</template>

<script setup lang="ts">
import { ref, computed, defineAsyncComponent, watch } from 'vue'
import type { AgentAttributeDto } from '~/shared/api-client'
import AgentAttributeRenderer from './AgentAttributeRenderer.vue'

const VueHcaptcha = defineAsyncComponent(
  () => import('@hcaptcha/vue3-hcaptcha')
)

const props = defineProps<{
  templateName: string
  promptTemplates: { id: string; title: string; content: string }[]
  allowTemplateEditing: boolean
  selectedPromptTemplateId?: string
  attributes?: AgentAttributeDto[]
  canToggleVisibility?: boolean
  defaultPublic?: boolean
  loading?: boolean
  fallbackMailto?: string | null
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
  (e: 'cancel'): void
}>()

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

const prompt = ref('')
const isPrivate = ref(props.defaultPublic === false)
const attributeValues = ref<Record<string, unknown>>({})
const captchaToken = ref<string | null>(null)

const onCaptchaVerify = (token: string) => {
  captchaToken.value = token
}

const onCaptchaExpired = () => {
  captchaToken.value = null
}

const isValid = computed(() => {
  const hasPrompt = !!prompt.value.trim()
  const hasTemplate = !!selectedPromptTemplateId.value
  const hasCaptcha = siteKey.value ? !!captchaToken.value : true
  return hasPrompt && hasTemplate && hasCaptcha
})

function submit() {
  if (!isValid.value) return
  emit('submit', {
    prompt: prompt.value,
    promptVariantId: selectedPromptTemplateId.value,
    isPrivate: isPrivate.value,
    attributeValues: attributeValues.value,
    captchaToken: captchaToken.value || undefined,
  })
}
</script>
