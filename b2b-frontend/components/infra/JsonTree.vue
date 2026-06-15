<template>
  <div class="json-tree">
    <template v-if="kind === 'primitive'">
      <span class="json-tree__primitive" :class="`json-tree__primitive--${primitiveKind}`">
        {{ primitiveValue }}
      </span>
    </template>

    <v-sheet
      v-else-if="kind === 'array'"
      class="json-tree__container json-tree__container--array"
      border
      rounded="lg"
    >
      <div class="json-tree__header">
        <div class="d-flex align-center ga-2">
          <v-icon size="16" icon="mdi-code-brackets" />
          <span class="text-body-2 font-weight-medium">{{ t('nodes.json.array') }}</span>
        </div>
        <v-chip size="x-small" variant="tonal">{{ arrayValue.length }}</v-chip>
      </div>

      <div v-if="arrayValue.length === 0" class="json-tree__empty">
        {{ t('nodes.json.empty_array') }}
      </div>

      <div v-else class="json-tree__body json-tree__body--array">
        <div v-for="(item, index) in arrayValue" :key="index" class="json-tree__item">
          <div class="json-tree__key">[{{ index }}]</div>
          <JsonTree :value="item" :depth="depth + 1" />
        </div>
      </div>
    </v-sheet>

    <v-sheet
      v-else
      class="json-tree__container json-tree__container--object"
      border
      rounded="lg"
    >
      <div class="json-tree__header">
        <div class="d-flex align-center ga-2">
          <v-icon size="16" icon="mdi-code-braces" />
          <span class="text-body-2 font-weight-medium">{{ t('nodes.json.object') }}</span>
        </div>
        <v-chip size="x-small" variant="tonal">{{ objectEntries.length }}</v-chip>
      </div>

      <div v-if="objectEntries.length === 0" class="json-tree__empty">
        {{ t('nodes.json.empty_object') }}
      </div>

      <div v-else class="json-tree__body">
        <div v-for="entry in objectEntries" :key="entry.key" class="json-tree__row">
          <div class="json-tree__key">{{ entry.key }}</div>
          <JsonTree :value="entry.value" :depth="depth + 1" />
        </div>
      </div>
    </v-sheet>
  </div>
</template>

<script setup lang="ts">
defineOptions({
  name: 'JsonTree'
})

const props = withDefaults(defineProps<{
  value: unknown
  depth?: number
}>(), {
  depth: 0
})

const { t } = useI18n()

const kind = computed(() => {
  if (props.value === null || props.value === undefined) {
    return 'primitive'
  }
  if (Array.isArray(props.value)) {
    return 'array'
  }
  if (typeof props.value === 'object') {
    return 'object'
  }
  return 'primitive'
})

const primitiveKind = computed(() => {
  if (props.value === null) return 'null'
  return typeof props.value
})

const primitiveValue = computed(() => {
  if (props.value === null) {
    return 'null'
  }
  if (props.value === undefined) {
    return 'undefined'
  }
  if (typeof props.value === 'string') {
    return props.value
  }
  if (typeof props.value === 'number' || typeof props.value === 'boolean' || typeof props.value === 'bigint') {
    return String(props.value)
  }
  return JSON.stringify(props.value)
})

const arrayValue = computed(() => (Array.isArray(props.value) ? props.value : []))

const objectEntries = computed(() => {
  if (!props.value || typeof props.value !== 'object' || Array.isArray(props.value)) {
    return []
  }
  return Object.entries(props.value as Record<string, unknown>)
    .sort(([left], [right]) => left.localeCompare(right))
    .map(([key, value]) => ({ key, value }))
})
</script>

<style scoped>
.json-tree {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.json-tree__primitive {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 0.8rem;
  line-height: 1;
  word-break: break-word;
}

.json-tree__primitive--string {
  color: rgb(var(--v-theme-primary));
  background: rgba(var(--v-theme-primary), 0.1);
}

.json-tree__primitive--number {
  color: rgb(var(--v-theme-info));
  background: rgba(var(--v-theme-info), 0.12);
}

.json-tree__primitive--boolean {
  color: rgb(var(--v-theme-success));
  background: rgba(var(--v-theme-success), 0.12);
}

.json-tree__primitive--bigint {
  color: rgb(var(--v-theme-info));
  background: rgba(var(--v-theme-info), 0.12);
}

.json-tree__primitive--undefined,
.json-tree__primitive--null,
.json-tree__primitive--object {
  color: rgb(var(--v-theme-on-surface-variant));
  background: rgba(var(--v-theme-surface-variant), 0.72);
}

.json-tree__container {
  padding: 12px;
  background: rgba(var(--v-theme-surface), 0.96);
}

.json-tree__container--array {
  border-color: rgba(var(--v-theme-primary), 0.16);
}

.json-tree__container--object {
  border-color: rgba(var(--v-theme-on-surface), 0.12);
}

.json-tree__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.json-tree__body {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.json-tree__body--array {
  gap: 12px;
}

.json-tree__item,
.json-tree__row {
  display: grid;
  grid-template-columns: minmax(64px, 180px) minmax(0, 1fr);
  gap: 12px;
  align-items: start;
}

.json-tree__key {
  padding-top: 6px;
  font-size: 0.78rem;
  font-weight: 700;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  color: rgb(var(--v-theme-on-surface-variant));
  overflow-wrap: anywhere;
}

.json-tree__empty {
  padding: 8px 0 2px;
  font-size: 0.875rem;
  color: rgb(var(--v-theme-on-surface-variant));
}

@media (max-width: 960px) {
  .json-tree__item,
  .json-tree__row {
    grid-template-columns: 1fr;
    gap: 6px;
  }

  .json-tree__key {
    padding-top: 0;
  }
}
</style>
