<template>
  <v-dialog :model-value="open" max-width="560" @update:model-value="emit('close')">
    <v-card>
      <v-card-title>{{ t('audit.policy.title') }}</v-card-title>
      <v-card-text class="d-flex flex-column ga-4">
        <v-switch
          v-model="auditTrailEnabled"
          :label="t('audit.policy.enabled')"
          color="primary"
          hide-details
        />

        <v-select
          v-model="operationLogLevel"
          :items="levelItems"
          :label="t('audit.policy.level')"
          :disabled="!auditTrailEnabled"
          variant="outlined"
          density="comfortable"
        />

        <v-text-field
          v-model="operationLogRecentLimit"
          :label="t('audit.policy.recent_limit')"
          :disabled="!auditTrailEnabled"
          type="number"
          min="1"
          max="500"
          variant="outlined"
          density="comfortable"
        />

        <v-alert type="info" variant="tonal" density="comfortable">
          {{ t('audit.policy.hint') }}
        </v-alert>
      </v-card-text>
      <v-card-actions>
        <v-spacer />
        <v-btn variant="text" @click="emit('close')">{{ t('keys.actions.cancel') }}</v-btn>
        <v-btn color="primary" @click="onSave">{{ t('audit.policy.save') }}</v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script setup lang="ts">
import type { AuditLogLevel } from '~/domains/audit/audit'
import type { KeyAuditPolicyUpdateRequest, KeyListItem } from '~/domains/keys/keys'

const props = defineProps<{
  open: boolean
  keyItem: KeyListItem | null
}>()

const emit = defineEmits<{
  close: []
  save: [KeyAuditPolicyUpdateRequest]
}>()

const { t } = useI18n()

const auditTrailEnabled = ref(false)
const operationLogLevel = ref<AuditLogLevel>('INFO')
const operationLogRecentLimit = ref('50')

const levelItems = computed(() => [
  { title: t('audit.levels.info'), value: 'INFO' },
  { title: t('audit.levels.debug'), value: 'DEBUG' }
])

watch(() => props.keyItem, (value) => {
  auditTrailEnabled.value = Boolean(value?.auditTrailEnabled)
  operationLogLevel.value = (value?.operationLogLevel || 'INFO') as AuditLogLevel
  operationLogRecentLimit.value = String(value?.operationLogRecentLimit ?? 50)
}, { immediate: true })

function onSave() {
  emit('save', {
    auditTrailEnabled: auditTrailEnabled.value,
    operationLogLevel: operationLogLevel.value,
    operationLogRecentLimit: Math.max(1, Math.min(500, Number(operationLogRecentLimit.value || '50')))
  })
}
</script>
