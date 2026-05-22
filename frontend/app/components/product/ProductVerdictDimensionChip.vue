<template>
  <div
    class="verdict-dimension-chip d-flex align-center px-4 py-3 rounded-xl cursor-pointer transition-all elevation-1"
    :class="[
      `verdict-dimension-chip--${color}`,
      { 'verdict-dimension-chip--hovered': hovered },
    ]"
    role="button"
    tabindex="0"
    @mouseenter="hovered = true"
    @mouseleave="hovered = false"
    @click="$emit('click')"
    @keydown.enter="$emit('click')"
    @keydown.space.prevent="$emit('click')"
  >
    <div
      class="verdict-dimension-chip__icon-wrapper d-flex align-center justify-center mr-3 rounded-circle"
    >
      <v-icon :icon="icon" size="20" class="verdict-dimension-chip__icon" />
    </div>
    <div class="verdict-dimension-chip__content d-flex flex-column">
      <span
        class="verdict-dimension-chip__title text-caption font-weight-medium text-uppercase tracking-wider"
      >
        {{ title }}
      </span>
      <span
        class="verdict-dimension-chip__value text-subtitle-2 font-weight-bold"
      >
        {{ value }}
      </span>
    </div>

    <v-tooltip
      v-if="tooltip"
      activator="parent"
      location="top"
      class="verdict-dimension-chip__tooltip"
    >
      {{ tooltip }}
    </v-tooltip>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

defineProps<{
  icon: string
  color: 'success' | 'warning' | 'error' | 'grey' | string
  title: string
  value: string
  tooltip?: string
}>()

defineEmits<{
  (e: 'click'): void
}>()

const hovered = ref(false)
</script>

<style scoped>
.verdict-dimension-chip {
  min-width: 180px;
  background: rgba(var(--v-theme-surface-glass), 0.7);
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.15);
  backdrop-filter: blur(12px);
  user-select: none;
  outline: none;
  transform: translateY(0);
}

.verdict-dimension-chip:focus-visible {
  box-shadow: 0 0 0 2px rgb(var(--v-theme-accent-primary-highlight));
}

.verdict-dimension-chip--hovered {
  transform: translateY(-2px);
  background: rgba(var(--v-theme-surface-glass), 0.95);
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.1);
}

.verdict-dimension-chip__icon-wrapper {
  width: 36px;
  height: 36px;
  transition: all 0.3s ease;
}

/* Color thematic styles using variables */
.verdict-dimension-chip--success {
  border-color: rgba(var(--v-theme-success), 0.3);
}
.verdict-dimension-chip--success .verdict-dimension-chip__icon-wrapper {
  background: rgba(var(--v-theme-success), 0.15);
}
.verdict-dimension-chip--success .verdict-dimension-chip__icon {
  color: rgb(var(--v-theme-success));
}
.verdict-dimension-chip--success .verdict-dimension-chip__value {
  color: rgb(var(--v-theme-success));
}
.verdict-dimension-chip--success.verdict-dimension-chip--hovered {
  box-shadow: 0 8px 20px rgba(var(--v-theme-success), 0.15);
}

.verdict-dimension-chip--warning {
  border-color: rgba(var(--v-theme-warning), 0.3);
}
.verdict-dimension-chip--warning .verdict-dimension-chip__icon-wrapper {
  background: rgba(var(--v-theme-warning), 0.15);
}
.verdict-dimension-chip--warning .verdict-dimension-chip__icon {
  color: rgb(var(--v-theme-warning));
}
.verdict-dimension-chip--warning .verdict-dimension-chip__value {
  color: rgb(var(--v-theme-warning));
}
.verdict-dimension-chip--warning.verdict-dimension-chip--hovered {
  box-shadow: 0 8px 20px rgba(var(--v-theme-warning), 0.15);
}

.verdict-dimension-chip--error {
  border-color: rgba(var(--v-theme-error), 0.3);
}
.verdict-dimension-chip--error .verdict-dimension-chip__icon-wrapper {
  background: rgba(var(--v-theme-error), 0.15);
}
.verdict-dimension-chip--error .verdict-dimension-chip__icon {
  color: rgb(var(--v-theme-error));
}
.verdict-dimension-chip--error .verdict-dimension-chip__value {
  color: rgb(var(--v-theme-error));
}
.verdict-dimension-chip--error.verdict-dimension-chip--hovered {
  box-shadow: 0 8px 20px rgba(var(--v-theme-error), 0.15);
}

.verdict-dimension-chip--grey {
  border-color: rgba(var(--v-theme-text-neutral-soft), 0.3);
}
.verdict-dimension-chip--grey .verdict-dimension-chip__icon-wrapper {
  background: rgba(var(--v-theme-text-neutral-soft), 0.1);
}
.verdict-dimension-chip--grey .verdict-dimension-chip__icon {
  color: rgb(var(--v-theme-text-neutral-secondary));
}
.verdict-dimension-chip--grey .verdict-dimension-chip__value {
  color: rgb(var(--v-theme-text-neutral-secondary));
}

.verdict-dimension-chip__title {
  color: rgb(var(--v-theme-text-neutral-secondary));
  letter-spacing: 0.05em;
}

.verdict-dimension-chip__value {
  white-space: nowrap;
}
</style>
