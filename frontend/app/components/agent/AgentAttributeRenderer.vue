<template>
  <div class="agent-attribute-renderer mb-4">
    <!-- TEXT Input -->
    <v-text-field
      v-if="attribute.type === 'TEXT'"
      v-model="internalValue"
      :label="attribute.label || attribute.id"
      variant="outlined"
      hide-details="auto"
      :disabled="disabled"
    ></v-text-field>

    <!-- LIST (Select multiple) -->
    <v-select
      v-else-if="attribute.type === 'LIST'"
      v-model="internalValue"
      :items="attribute.options"
      :label="attribute.label || attribute.id"
      multiple
      chips
      variant="outlined"
      hide-details="auto"
      :disabled="disabled"
    ></v-select>

    <!-- COMBO (Select single) -->
    <v-select
      v-else-if="attribute.type === 'COMBO'"
      v-model="internalValue"
      :items="attribute.options"
      :label="attribute.label || attribute.id"
      variant="outlined"
      hide-details="auto"
      :disabled="disabled"
    ></v-select>

    <!-- CHECKBOX -->
    <v-checkbox
      v-else-if="attribute.type === 'CHECKBOX'"
      v-model="internalValue"
      :label="attribute.label || attribute.id"
      hide-details="auto"
      density="compact"
      :disabled="disabled"
    ></v-checkbox>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import type { AgentAttributeDto } from '~/shared/api-client'

const props = withDefaults(
  defineProps<{
    attribute: AgentAttributeDto
    modelValue: unknown
    disabled?: boolean
  }>(),
  {
    disabled: false,
  }
)

const emit = defineEmits(['update:modelValue'])

const internalValue = computed({
  get: () => props.modelValue,
  set: val => emit('update:modelValue', val),
})

// Initialize default values for certain types if undefined
onMounted(() => {
  if (props.modelValue === undefined) {
    if (props.attribute.type === 'CHECKBOX') {
      emit('update:modelValue', false)
    } else if (props.attribute.type === 'LIST') {
      emit('update:modelValue', [])
    }
  }
})
</script>
