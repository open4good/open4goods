<template>
  <v-form @submit.prevent="submit">
    <v-row>
      <v-col v-for="field in schema.fields" :key="field.key" cols="12" md="6">
        <v-switch
          v-if="field.component === 'switch'"
          :model-value="Boolean(localModel[field.key])"
          :label="t(field.label)"
          color="primary"
          @update:model-value="updateValue(field.key, $event)"
        />

        <v-text-field
          v-else-if="field.component === 'number'"
          :model-value="localModel[field.key]"
          :label="t(field.label)"
          color="primary"
          type="number"
          :error-messages="errors[field.key]"
          @update:model-value="updateValue(field.key, Number($event))"
        />

        <v-textarea
          v-else-if="field.component === 'json'"
          :model-value="stringifyValue(localModel[field.key])"
          :label="t(field.label)"
          auto-grow
          color="primary"
          :error-messages="errors[field.key]"
          @update:model-value="updateJson(field.key, $event)"
        />

        <v-text-field
          v-else
          :model-value="localModel[field.key]"
          :label="t(field.label)"
          color="primary"
          :error-messages="errors[field.key]"
          @update:model-value="updateValue(field.key, $event)"
        />
      </v-col>
    </v-row>

    <v-btn color="primary" type="submit" variant="flat">
      {{ t(submitLabel) }}
    </v-btn>
  </v-form>
</template>

<script setup lang="ts">
import type { ZodSchema } from 'zod'
import type { UiSchemaEntity } from '~/ui-schema/health.gen'

const props = withDefaults(defineProps<{
  schema: UiSchemaEntity
  modelValue: Record<string, unknown>
  submitLabel?: string
  validator?: ZodSchema
}>(), {
  submitLabel: 'common.submit',
  validator: undefined
})

const emit = defineEmits<{
  'update:modelValue': [Record<string, unknown>]
  submit: [Record<string, unknown>]
}>()

const { t } = useI18n()

const localModel = ref<Record<string, unknown>>({ ...props.modelValue })
const errors = ref<Record<string, string[]>>({})

watch(
  () => props.modelValue,
  (value) => {
    localModel.value = { ...value }
  }
)

function updateValue(key: string, value: unknown) {
  localModel.value = {
    ...localModel.value,
    [key]: value
  }
  emit('update:modelValue', localModel.value)
}

function updateJson(key: string, value: string) {
  try {
    updateValue(key, value ? JSON.parse(value) : {})
    errors.value[key] = []
  } catch {
    errors.value[key] = [t('errors.validation_error')]
  }
}

function stringifyValue(value: unknown) {
  if (!value) {
    return ''
  }

  return JSON.stringify(value, null, 2)
}

function submit() {
  if (props.validator) {
    const validationResult = props.validator.safeParse(localModel.value)
    if (!validationResult.success) {
      const fieldErrors = validationResult.error.flatten().fieldErrors
      errors.value = Object.fromEntries(
        Object.entries(fieldErrors).map(([key, val]) => [key, ((val as string[] | undefined) ?? []).map((item) => item || '')])
      )
      return
    }
  }

  emit('submit', localModel.value)
}
</script>
