<template>
  <v-sheet class="b2b-code-block" rounded="lg">
    <div class="d-flex align-center justify-space-between ga-3 px-3 py-2 b2b-code-block__toolbar">
      <span class="text-caption text-medium-emphasis">{{ language }}</span>
      <v-btn
        size="small"
        variant="text"
        prepend-icon="mdi-content-copy"
        :text="copied ? copiedLabel : copyLabel"
        @click="copy"
      />
    </div>
    <pre class="ma-0 pa-4"><code>{{ code }}</code></pre>
  </v-sheet>
</template>

<script setup lang="ts">
const props = withDefaults(defineProps<{
  code: string
  language?: string
  copyLabel?: string
  copiedLabel?: string
}>(), {
  language: 'text',
  copyLabel: 'Copy',
  copiedLabel: 'Copied'
})

const copied = ref(false)

async function copy() {
  await navigator.clipboard?.writeText(props.code)
  copied.value = true
  window.setTimeout(() => {
    copied.value = false
  }, 1600)
}
</script>

<style scoped>
.b2b-code-block {
  overflow: hidden;
  border: 1px solid rgba(var(--v-border-color), var(--v-border-opacity));
  background: #0f172a;
  color: #e2e8f0;
}

.b2b-code-block__toolbar {
  border-bottom: 1px solid rgba(226, 232, 240, 0.16);
  background: rgba(15, 23, 42, 0.96);
}

pre {
  overflow-x: auto;
  font-size: 0.875rem;
  line-height: 1.55;
}
</style>
