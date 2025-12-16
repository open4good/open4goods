<template>
  <nav
    class="sticky-section-navigation"
    :class="[
      `sticky-section-navigation--${orientation}`,
      { 'sticky-section-navigation--sticky': sticky },
    ]"
    :aria-label="ariaLabel"
  >
    <ul class="sticky-section-navigation__list">
      <li
        v-for="section in sections"
        :key="section.id"
        class="sticky-section-navigation__item"
      >
        <button
          type="button"
          class="sticky-section-navigation__link"
          :class="{
            'sticky-section-navigation__link--active':
              section.id === activeSection,
          }"
          @click="onNavigate(section.id)"
        >
          <v-icon
            v-if="section.icon"
            :icon="section.icon"
            size="18"
            class="sticky-section-navigation__icon"
          />
          <span class="sticky-section-navigation__label">{{
            section.label
          }}</span>
        </button>
      </li>
    </ul>
  </nav>
</template>

<script setup lang="ts">
import type { PropType } from 'vue'

const _props = defineProps({
  sections: {
    type: Array as PropType<
      Array<{ id: string; label: string; icon?: string }>
    >,
    default: () => [],
  },
  activeSection: {
    type: String,
    default: '',
  },
  orientation: {
    type: String as PropType<'vertical' | 'horizontal'>,
    default: 'vertical',
  },
  ariaLabel: {
    type: String,
    default: 'Page navigation',
  },
  sticky: {
    type: Boolean,
    default: true,
  },
})

const emit = defineEmits<{ navigate: [string] }>()

const onNavigate = (sectionId: string) => {
  emit('navigate', sectionId)
}
</script>

<style scoped>
.sticky-section-navigation {
  position: relative;
  top: auto;
  z-index: 10;
  padding: 1.5rem 1rem;
  border-radius: 18px;
  background: rgba(var(--v-theme-surface-glass));
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.08);
  backdrop-filter: blur(10px);
}

.sticky-section-navigation--sticky {
  position: sticky;
  top: 96px;
}

.sticky-section-navigation--horizontal {
  top: 0;
  background: rgba(var(--v-theme-surface-glass), 0.92);
  border-radius: 0;
  padding: 0.75rem 1rem;
  box-shadow: none;
  width: 100%;
  border-bottom: 1px solid rgba(var(--v-theme-border-primary-strong), 0.35);
}

.sticky-section-navigation__list {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  padding: 0;
  margin: 0;
}

.sticky-section-navigation--horizontal .sticky-section-navigation__list {
  flex-direction: row;
  justify-content: flex-start;
  gap: 0.75rem;
  overflow-x: auto;
  scrollbar-width: thin;
}

.sticky-section-navigation__item {
  display: contents;
}

.sticky-section-navigation__link {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  width: 100%;
  padding: 0.625rem 0.75rem;
  border-radius: 12px;
  border: 1px solid transparent;
  background: transparent;
  font-weight: 600;
  font-size: 0.92rem;
  color: rgb(var(--v-theme-text-neutral-secondary));
  transition: all 0.25s ease;
}

.sticky-section-navigation__link:hover,
.sticky-section-navigation__link:focus-visible {
  border-color: rgba(var(--v-theme-border-primary-strong), 0.6);
  color: rgb(var(--v-theme-text-neutral-strong));
  background: rgba(var(--v-theme-surface-primary-080), 0.6);
}

.sticky-section-navigation__link--active {
  background: rgba(var(--v-theme-surface-primary-100), 0.9);
  color: rgb(var(--v-theme-text-neutral-strong));
  border-color: rgba(var(--v-theme-border-primary-strong), 0.95);
  box-shadow: inset 0 0 0 1px
    rgba(var(--v-theme-accent-primary-highlight), 0.22);
}

.sticky-section-navigation__icon {
  color: rgb(var(--v-theme-accent-primary-highlight));
}

.sticky-section-navigation__label {
  display: inline-block;
  white-space: normal;
  line-height: 1.35;
  text-align: left;
  word-break: break-word;
}

.sticky-section-navigation--horizontal .sticky-section-navigation__link {
  justify-content: center;
}

.sticky-section-navigation--horizontal .sticky-section-navigation__label {
  text-align: center;
}

.sticky-section-navigation--horizontal.sticky-section-navigation--sticky {
  top: 0;
}

@media (max-width: 960px) {
  .sticky-section-navigation {
    border-radius: 0;
    box-shadow: none;
    padding: 0.75rem 0.5rem;
  }

  .sticky-section-navigation__link {
    font-size: 0.85rem;
    padding: 0.5rem 0.75rem;
  }
}
</style>
