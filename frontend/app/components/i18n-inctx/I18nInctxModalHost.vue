<template>
  <VDialog
    v-model="dialogVisible"
    max-width="540"
    transition="dialog-bottom-transition"
    :scrim="true"
  >
    <VCard>
      <VCardTitle class="d-flex align-center justify-space-between">
        <span class="text-subtitle-1 font-weight-medium">In-context translation</span>
        <VBtn icon="mdi-close" variant="text" @click="handleClose" />
      </VCardTitle>

      <VCardText>
        <div class="text-caption text-medium-emphasis mb-1">Key</div>
        <VSheet class="pa-2 rounded-sm bg-surface-variant" border>
          <code class="text-body-2">{{ state.value.key }}</code>
        </VSheet>

        <div v-if="state.value.locale" class="text-caption text-medium-emphasis mt-4 mb-1">Locale</div>
        <VChip v-if="state.value.locale" size="small" color="primary" variant="outlined">
          {{ state.value.locale }}
        </VChip>

        <div class="text-caption text-medium-emphasis mt-4 mb-1">Current value</div>
        <VTextarea
          :model-value="state.value.translation"
          :rows="4"
          variant="outlined"
          readonly
          auto-grow
          class="i18n-inctx__textarea"
        />

        <VAlert
          v-if="state.value.copyStatus === 'copied'"
          type="success"
          variant="tonal"
          density="compact"
          class="mt-4"
        >
          Translation key copied to clipboard.
        </VAlert>
        <VAlert
          v-else-if="state.value.copyStatus === 'error'"
          type="warning"
          variant="tonal"
          density="compact"
          class="mt-4"
        >
          Unable to copy the key automatically. Use the copy button below.
        </VAlert>
      </VCardText>

      <VCardActions class="justify-space-between">
        <VBtn variant="tonal" color="primary" @click="handleCopy">
          <VIcon icon="mdi-content-copy" start />
          Copy key
        </VBtn>
        <VBtn color="primary" variant="flat" :disabled="!state.value.githubUrl" @click="handleOpenGithub">
          <VIcon icon="mdi-open-in-new" start />
          Open JSON on GitHub
        </VBtn>
      </VCardActions>
    </VCard>
  </VDialog>
</template>

<script setup lang="ts">
import { computed } from 'vue'

import { copyTranslationKey } from '~/lib/i18n-inctx/github-editor'
import { useI18nInContextEditorState } from '~/composables/i18n-inctx/useI18nInContextEditorState'

const { state, close, reset, setCopyStatus } = useI18nInContextEditorState()

const dialogVisible = computed({
  get: () => state.value.isOpen,
  set: (value: boolean) => {
    if (!value) {
      close()
      reset()
    }
  },
})

async function handleCopy() {
  const success = await copyTranslationKey(state.value.key)
  setCopyStatus(success ? 'copied' : 'error')
}

function handleOpenGithub() {
  if (typeof window === 'undefined' || !state.value.githubUrl) {
    return
  }

  window.open(state.value.githubUrl, '_blank', 'noopener')
}

function handleClose() {
  close()
  reset()
}
</script>

<style scoped>
.i18n-inctx__textarea {
  font-family: var(--v-font-family-monospace, 'Roboto Mono', monospace);
}
</style>
