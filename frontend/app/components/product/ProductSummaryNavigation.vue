<template>
  <nav
    class="product-summary-navigation"
    :class="`product-summary-navigation--${orientation}`"
    :aria-label="ariaLabel"
  >
    <ul class="product-summary-navigation__list">
      <li
        v-for="section in sections"
        :key="section.id"
        class="product-summary-navigation__item"
      >
        <button
          type="button"
          class="product-summary-navigation__link"
          :class="{ 'product-summary-navigation__link--active': section.id === activeSection }"
          :aria-current="section.id === activeSection ? 'true' : undefined"
          :aria-controls="section.id"
          @click="onNavigate(section.id)"
        >
          <v-icon
            v-if="section.icon"
            :icon="section.icon"
            size="18"
            class="product-summary-navigation__icon"
          />
          <span class="product-summary-navigation__label">{{ section.label }}</span>
        </button>
      </li>
    </ul>
  </nav>
</template>

<script setup lang="ts">
import type { PropType } from 'vue'

const _props = defineProps({
  sections: {
    type: Array as PropType<Array<{ id: string; label: string; icon?: string }>>,
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
    default: 'Product summary navigation',
  },
})

const emit = defineEmits<{ navigate: [string] }>()

const onNavigate = (sectionId: string) => {
  emit('navigate', sectionId)
}
</script>

<style scoped>
.product-summary-navigation {
  position: sticky;
  top: 96px;
  z-index: 10;
  padding: 1.5rem 1rem;
  border-radius: 16px;
  background: rgba(var(--v-theme-surface-glass));
  box-shadow: 0 14px 40px rgba(15, 23, 42, 0.06);
  backdrop-filter: blur(8px);
}

.product-summary-navigation--horizontal {
  top: 0;
  background: rgba(var(--v-theme-surface-glass), 0.9);
  border-radius: 0;
  padding: 0.75rem 1rem;
  box-shadow: none;
  width: 100%;
  border-bottom: 1px solid rgba(var(--v-theme-border-primary-strong), 0.3);
}

.product-summary-navigation__list {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  padding: 0;
  margin: 0;
}

.product-summary-navigation--horizontal .product-summary-navigation__list {
  flex-direction: row;
  justify-content: space-between;
  gap: 0.75rem;
  overflow-x: auto;
  scrollbar-width: thin;
}

.product-summary-navigation__item {
  display: contents;
}

.product-summary-navigation__link {
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

.product-summary-navigation__link:hover,
.product-summary-navigation__link:focus-visible {
  border-color: rgba(var(--v-theme-border-primary-strong), 0.6);
  color: rgb(var(--v-theme-text-neutral-strong));
  background: rgba(var(--v-theme-surface-primary-080), 0.6);
}

.product-summary-navigation__link--active {
  background: rgba(var(--v-theme-surface-primary-100), 0.85);
  color: rgb(var(--v-theme-text-neutral-strong));
  border-color: rgba(var(--v-theme-border-primary-strong), 0.9);
  box-shadow: inset 0 0 0 1px rgba(var(--v-theme-accent-primary-highlight), 0.2);
}

.product-summary-navigation__icon {
  color: rgb(var(--v-theme-accent-primary-highlight));
}

.product-summary-navigation__label {
  white-space: nowrap;
}

.product-summary-navigation--horizontal .product-summary-navigation__link {
  justify-content: center;
}

@media (max-width: 960px) {
  .product-summary-navigation {
    border-radius: 0;
    box-shadow: none;
    padding: 0.75rem 0.5rem;
  }

  .product-summary-navigation__link {
    font-size: 0.85rem;
    padding: 0.5rem 0.75rem;
  }
}
</style>
