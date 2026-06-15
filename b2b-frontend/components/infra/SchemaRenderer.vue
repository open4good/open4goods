<template>
  <v-card>
    <v-card-title>{{ t(title) }}</v-card-title>
    <v-card-text>
      <InfForm
        v-model="formModel"
        :schema="schema"
        :validator="validator"
        :submit-label="submitLabel"
        @submit="onSubmit"
      />
    </v-card-text>
  </v-card>
</template>

<script setup lang="ts">
import type { ZodSchema } from 'zod'
import type { UiSchemaEntity } from '~/ui-schema/health.gen'

const props = withDefaults(defineProps<{
  schema: UiSchemaEntity
  title: string
  initialModel?: Record<string, unknown>
  submitLabel?: string
  validator?: ZodSchema
}>(), {
  initialModel: () => ({}),
  submitLabel: 'common.submit', validator: undefined
})

const emit = defineEmits<{
  submit: [Record<string, unknown>]
}>()

const { t } = useI18n()
const formModel = ref<Record<string, unknown>>({ ...props.initialModel })

function onSubmit(value: Record<string, unknown>) {
  emit('submit', value)
}
</script>
