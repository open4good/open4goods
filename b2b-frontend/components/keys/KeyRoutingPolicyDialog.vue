<template>
  <v-dialog :model-value="open" max-width="760" @update:model-value="emit('close')">
    <v-card>
      <v-card-title>{{ t('keys.routing_policy.title') }}</v-card-title>
      <v-card-text class="d-flex flex-column ga-4">
        <v-btn-toggle v-model="nativeRoutingMode" mandatory density="comfortable" divided>
          <v-btn value="INFERA_GRID">{{ t('keys.billing.native_provider.infera_grid') }}</v-btn>
          <v-btn value="ORIGINAL_PROVIDER">{{ t('keys.billing.native_provider.original_provider') }}</v-btn>
        </v-btn-toggle>

        <div class="key-routing-grid">
          <v-select
            v-model="nativeProviderKind"
            :items="providerItems"
            :label="t('keys.billing.native_provider.kind')"
            variant="outlined"
            density="comfortable"
          />
          <v-text-field
            v-model="nativeProviderBaseUrl"
            :label="t('keys.billing.native_provider.base_url')"
            variant="outlined"
            density="comfortable"
          />
          <v-text-field
            v-model="nativeProviderApiKey"
            :label="t('keys.routing_policy.api_key_replace')"
            type="password"
            autocomplete="off"
            variant="outlined"
            density="comfortable"
          />
          <v-text-field
            v-model="nativeProviderModel"
            :label="t('keys.billing.native_provider.model')"
            variant="outlined"
            density="comfortable"
          />
        </div>

        <v-textarea
          v-model="nativeProviderHeadersJson"
          :label="t('keys.routing_policy.headers')"
          rows="2"
          auto-grow
          variant="outlined"
          density="comfortable"
        />
        <v-textarea
          v-model="nativeProviderDefaultsJson"
          :label="t('keys.routing_policy.defaults')"
          rows="2"
          auto-grow
          variant="outlined"
          density="comfortable"
        />
        <v-textarea
          v-model="nativeProviderExtraBodyJson"
          :label="t('keys.routing_policy.extra_body')"
          rows="2"
          auto-grow
          variant="outlined"
          density="comfortable"
        />
        <v-textarea
          v-model="nativeProviderCustomPropertiesJson"
          :label="t('keys.routing_policy.custom_properties')"
          rows="2"
          auto-grow
          variant="outlined"
          density="comfortable"
        />

        <v-divider />

        <v-switch
          v-model="trafficExperimentEnabled"
          :label="t('keys.billing.traffic_experiment.title')"
          color="primary"
          hide-details
        />
        <v-slider
          v-model="trafficExperimentProviderSharePct"
          :disabled="!trafficExperimentEnabled"
          :label="t('keys.billing.traffic_experiment.share')"
          min="0"
          max="100"
          step="1"
          thumb-label
        />
        <v-textarea
          v-model="trafficExperimentRulesJson"
          :disabled="!trafficExperimentEnabled"
          :label="t('keys.billing.traffic_experiment.rules')"
          rows="3"
          auto-grow
          variant="outlined"
          density="comfortable"
        />

        <v-alert type="warning" variant="tonal" density="comfortable">
          {{ t('keys.routing_policy.secret_warning') }}
        </v-alert>
        <v-alert v-if="jsonError" type="error" variant="tonal" density="comfortable">
          {{ t('keys.routing_policy.invalid_json') }}
        </v-alert>
      </v-card-text>
      <v-card-actions>
        <v-spacer />
        <v-btn variant="text" @click="emit('close')">{{ t('keys.actions.cancel') }}</v-btn>
        <v-btn color="primary" :disabled="Boolean(jsonError)" @click="onSave">{{ t('keys.routing_policy.save') }}</v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script setup lang="ts">
import type { KeyListItem, KeyRoutingPolicyUpdateRequest } from '~/domains/keys/keys'

const props = defineProps<{
  open: boolean
  keyItem: KeyListItem | null
}>()

const emit = defineEmits<{
  close: []
  save: [KeyRoutingPolicyUpdateRequest]
}>()

const { t } = useI18n()

const nativeRoutingMode = ref<KeyRoutingPolicyUpdateRequest['nativeRoutingMode']>('INFERA_GRID')
const nativeProviderKind = ref<KeyRoutingPolicyUpdateRequest['nativeProviderKind']>('OPENAI')
const nativeProviderBaseUrl = ref('')
const nativeProviderApiKey = ref('')
const nativeProviderModel = ref('')
const nativeProviderHeadersJson = ref('{}')
const nativeProviderDefaultsJson = ref('{}')
const nativeProviderExtraBodyJson = ref('{}')
const nativeProviderCustomPropertiesJson = ref('{}')
const trafficExperimentEnabled = ref(false)
const trafficExperimentProviderSharePct = ref(0)
const trafficExperimentRulesJson = ref('{}')

const providerItems = computed(() => [
  { title: 'OpenAI', value: 'OPENAI' },
  { title: 'Gemini', value: 'GEMINI' },
  { title: 'Claude', value: 'CLAUDE' },
  { title: 'Mistral', value: 'MISTRAL' },
  { title: t('keys.billing.native_provider.custom'), value: 'CUSTOM_OPENAI_COMPATIBLE' }
])

const jsonError = computed(() => [
  nativeProviderHeadersJson.value,
  nativeProviderDefaultsJson.value,
  nativeProviderExtraBodyJson.value,
  nativeProviderCustomPropertiesJson.value,
  trafficExperimentRulesJson.value
].some(value => !isJsonObject(value)))

watch(() => props.keyItem, (value) => {
  nativeRoutingMode.value = (value?.nativeRoutingMode || 'INFERA_GRID') as KeyRoutingPolicyUpdateRequest['nativeRoutingMode']
  nativeProviderKind.value = (value?.nativeProviderKind || 'OPENAI') as KeyRoutingPolicyUpdateRequest['nativeProviderKind']
  nativeProviderBaseUrl.value = value?.nativeProviderBaseUrl || ''
  nativeProviderApiKey.value = ''
  nativeProviderModel.value = value?.nativeProviderModel || ''
  nativeProviderHeadersJson.value = value?.nativeProviderHeadersJson || '{}'
  nativeProviderDefaultsJson.value = value?.nativeProviderDefaultsJson || '{}'
  nativeProviderExtraBodyJson.value = value?.nativeProviderExtraBodyJson || '{}'
  nativeProviderCustomPropertiesJson.value = value?.nativeProviderCustomPropertiesJson || '{}'
  trafficExperimentEnabled.value = Boolean(value?.trafficExperimentEnabled)
  trafficExperimentProviderSharePct.value = value?.trafficExperimentProviderSharePct ?? 0
  trafficExperimentRulesJson.value = value?.trafficExperimentRulesJson || '{}'
}, { immediate: true })

function isJsonObject(raw: string) {
  try {
    const parsed = JSON.parse(raw || '{}')
    return parsed !== null && !Array.isArray(parsed) && typeof parsed === 'object'
  } catch {
    return false
  }
}

function onSave() {
  emit('save', {
    nativeRoutingMode: nativeRoutingMode.value,
    nativeProviderKind: nativeProviderKind.value,
    nativeProviderBaseUrl: nativeProviderBaseUrl.value,
    nativeProviderApiKey: nativeProviderApiKey.value,
    nativeProviderModel: nativeProviderModel.value,
    nativeProviderHeadersJson: nativeProviderHeadersJson.value || '{}',
    nativeProviderDefaultsJson: nativeProviderDefaultsJson.value || '{}',
    nativeProviderExtraBodyJson: nativeProviderExtraBodyJson.value || '{}',
    nativeProviderCustomPropertiesJson: nativeProviderCustomPropertiesJson.value || '{}',
    trafficExperimentEnabled: trafficExperimentEnabled.value,
    trafficExperimentProviderSharePct: trafficExperimentEnabled.value ? trafficExperimentProviderSharePct.value : 0,
    trafficExperimentRulesJson: trafficExperimentRulesJson.value || '{}'
  })
}
</script>

<style scoped>
.key-routing-grid {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
}
</style>
