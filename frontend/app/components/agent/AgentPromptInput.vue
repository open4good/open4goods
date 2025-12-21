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
        Describe your feature request or question below. Our AI agents will
        process it and create a GitHub issue for tracking.
      </v-alert>

      <v-textarea
        v-model="prompt"
        label="Your Request"
        placeholder="e.g., I want a feature to filter products by ..."
        rows="6"
        auto-grow
        variant="outlined"
        :rules="[v => !!v || 'Request cannot be empty']"
      ></v-textarea>

      <v-checkbox
        v-if="canToggleVisibility"
        v-model="isPrivate"
        color="secondary"
        hide-details
      >
        <template #label>
          <div>
            <strong>Make my prompt private</strong>
            <div class="text-caption">
              The issue will be public, but your prompt text will be hidden.
            </div>
          </div>
        </template>
      </v-checkbox>

      <div
        v-if="fallbackMailto"
        class="mt-6 pa-4 bg-grey-lighten-4 rounded h-100"
      >
        <div class="d-flex align-center">
          <v-icon icon="mdi-email-outline" class="mr-2"></v-icon>
          <span class="text-body-2 font-weight-bold">Prefer email?</span>
        </div>
        <p class="text-caption mt-1 mb-2">
          You can use your default email client instead.
        </p>
        <v-btn
          :href="fallbackMailto"
          target="_blank"
          variant="outlined"
          size="small"
          color="primary"
          >Open Email Client</v-btn
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
        :disabled="!prompt.trim()"
        @click="submit"
      >
        Submit Request
        <v-icon icon="mdi-send" end></v-icon>
      </v-btn>
    </v-card-actions>
  </v-card>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const props = defineProps<{
  templateName: string
  canToggleVisibility?: boolean
  defaultPublic?: boolean
  loading?: boolean
  fallbackMailto?: string | null
}>()

const emit = defineEmits<{
  (e: 'submit', payload: { prompt: string; isPrivate: boolean }): void
  (e: 'cancel'): void
}>()

const prompt = ref('')
const isPrivate = ref(props.defaultPublic === false)

function submit() {
  emit('submit', { prompt: prompt.value, isPrivate: isPrivate.value })
}
</script>
