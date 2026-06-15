<template>
  <v-dialog :model-value="modelValue" max-width="560" @update:model-value="emit('update:modelValue', $event)">
    <v-card rounded="lg">
      <v-card-title class="d-flex align-center justify-space-between ga-3">
        <span>{{ title }}</span>
        <v-btn icon="mdi-close" variant="text" @click="emit('update:modelValue', false)" />
      </v-card-title>
      <v-card-text>
        <slot />

        <v-alert v-if="secret" type="warning" variant="tonal" class="mt-4">
          <div class="font-weight-bold mb-2">Save this API key now.</div>
          <B2bCodeBlock :code="secret" language="api key" />
        </v-alert>
      </v-card-text>
      <v-card-actions class="px-6 pb-5">
        <v-spacer />
        <v-btn variant="text" @click="emit('cancel')">{{ cancelLabel }}</v-btn>
        <v-btn color="primary" :disabled="confirmDisabled" @click="emit('confirm')">{{ confirmLabel }}</v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script setup lang="ts">
import B2bCodeBlock from '~/components/B2bCodeBlock.vue'

withDefaults(defineProps<{
  modelValue: boolean
  title: string
  secret?: string
  confirmLabel?: string
  cancelLabel?: string
  confirmDisabled?: boolean
}>(), {
  secret: undefined,
  confirmLabel: 'Confirm',
  cancelLabel: 'Cancel',
  confirmDisabled: false
})

const emit = defineEmits<{
  'update:modelValue': [boolean]
  confirm: []
  cancel: []
}>()
</script>
