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
        <div class="product-summary-navigation__item-header">
          <button
            type="button"
            class="product-summary-navigation__link"
            :class="{
              'product-summary-navigation__link--active':
                isSectionActive(section),
            }"
            :aria-current="isSectionActive(section) ? 'true' : undefined"
            :aria-controls="section.id"
            @click="handleSectionClick(section)"
          >
            <v-icon
              v-if="section.icon"
              :icon="section.icon"
              size="18"
              class="product-summary-navigation__icon"
            />
            <span class="product-summary-navigation__label">{{
              section.label
            }}</span>
          </button>
          <button
            v-if="hasSubsections(section)"
            type="button"
            class="product-summary-navigation__toggle"
            :aria-expanded="isSubmenuOpen(section)"
            :aria-controls="`submenu-${section.id}`"
            @click="toggleSubmenu(section.id)"
          >
            <v-icon
              :icon="
                isSubmenuOpen(section) ? 'mdi-chevron-up' : 'mdi-chevron-down'
              "
              size="18"
            />
          </button>
        </div>
        <v-expand-transition>
          <ul
            v-if="hasSubsections(section)"
            v-show="isSubmenuOpen(section)"
            :id="`submenu-${section.id}`"
            class="product-summary-navigation__submenu"
          >
            <li
              v-for="subsection in section.subsections"
              :key="subsection.id"
              class="product-summary-navigation__submenu-item"
            >
              <button
                type="button"
                class="product-summary-navigation__submenu-link"
                :class="{
                  'product-summary-navigation__submenu-link--active':
                    subsection.id === activeSection,
                }"
                :aria-current="
                  subsection.id === activeSection ? 'true' : undefined
                "
                :aria-controls="subsection.id"
                @click="onNavigate(subsection.id)"
              >
                <v-icon
                  v-if="subsection.icon"
                  :icon="subsection.icon"
                  size="16"
                  class="product-summary-navigation__submenu-icon"
                />
                <span class="product-summary-navigation__submenu-label">{{
                  subsection.label
                }}</span>
              </button>
            </li>
          </ul>
        </v-expand-transition>
      </li>
    </ul>

    <section
      v-if="adminSections.length"
      class="product-summary-navigation__admin-panel"
      :aria-label="adminTitle"
    >
      <p class="product-summary-navigation__admin-eyebrow">{{ adminTitle }}</p>
      <p v-if="adminHelper" class="product-summary-navigation__admin-helper">
        {{ adminHelper }}
      </p>
      <ul class="product-summary-navigation__admin-list">
        <li
          v-for="admin in adminSections"
          :key="admin.id"
          class="product-summary-navigation__admin-item"
        >
          <button
            type="button"
            class="product-summary-navigation__admin-link"
            :class="{
              'product-summary-navigation__admin-link--active':
                admin.id === activeSection,
            }"
            :aria-current="admin.id === activeSection ? 'true' : undefined"
            :aria-controls="admin.id"
            @click="onNavigate(admin.id)"
          >
            <v-icon
              v-if="admin.icon"
              :icon="admin.icon"
              size="18"
              class="product-summary-navigation__admin-icon"
            />
            <span class="product-summary-navigation__admin-label">{{
              admin.label
            }}</span>
          </button>
        </li>
      </ul>
    </section>
  </nav>
</template>

<script setup lang="ts">
import type { PropType } from 'vue'
import { ref, watch } from 'vue'

const _props = defineProps({
  sections: {
    type: Array as PropType<
      Array<{
        id: string
        label: string
        icon?: string
        subsections?: Array<{ id: string; label: string; icon?: string }>
      }>
    >,
    default: () => [],
  },
  adminSections: {
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
    default: 'Product summary navigation',
  },
  adminTitle: {
    type: String,
    default: '',
  },
  adminHelper: {
    type: String,
    default: '',
  },
})

const emit = defineEmits<{ navigate: [string] }>()

const openSectionId = ref<string | null>(null)

const onNavigate = (sectionId: string) => {
  emit('navigate', sectionId)
}

const hasSubsections = (section: { subsections?: Array<{ id: string }> }) =>
  (section.subsections?.length ?? 0) > 0

const isSectionActive = (section: {
  id: string
  subsections?: Array<{ id: string }>
}) =>
  section.id === _props.activeSection ||
  Boolean(section.subsections?.some(sub => sub.id === _props.activeSection))

const isSubmenuOpen = (section: { id: string }) =>
  openSectionId.value === section.id || isSectionActive(section)

const toggleSubmenu = (sectionId: string) => {
  openSectionId.value = openSectionId.value === sectionId ? null : sectionId
}

const handleSectionClick = (section: { id: string }) => {
  if (hasSubsections(section)) {
    // If clicking the section header, ensure it opens
    openSectionId.value = section.id
  }
  onNavigate(section.id)
}

// Watch activeSection to latch the open state
// This prevents the menu from collapsing when scrolling through gaps or "up" out of a section
// It ensures the *last active* section remains open until a new one is engaged

watch(
  () => _props.activeSection,
  newActiveId => {
    if (!newActiveId) return

    // Find which section this ID belongs to
    const section = _props.sections.find(
      s =>
        s.id === newActiveId ||
        s.subsections?.some(sub => sub.id === newActiveId)
    )

    if (section && hasSubsections(section)) {
      openSectionId.value = section.id
    }
  },
  { immediate: true }
)
</script>

<style scoped>
.product-summary-navigation {
  position: sticky;
  top: var(--product-sticky-offset, 0px);
  z-index: 10;
  padding: 1.5rem 1rem;
  border-radius: 16px;
  background: rgba(var(--v-theme-surface-glass));
  box-shadow: 0 14px 40px rgba(15, 23, 42, 0.06);
  backdrop-filter: blur(8px);
}

.product-summary-navigation--horizontal {
  top: var(--product-sticky-offset, 0px);
  background: rgba(var(--v-theme-surface-glass), 0.9);
  border-radius: 0;
  padding: 0.75rem 1rem;
  box-shadow: none;
  width: 100%;
  max-width: 100vw;
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
  justify-content: flex-start; /* Changed from space-between to allow scrolling start */
  gap: 0.75rem;
  overflow-x: auto;
  scrollbar-width: thin;
  max-width: 100%; /* Ensure it fits within parent */
  padding-bottom: 4px; /* Space for scrollbar */
}

.product-summary-navigation__item {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.product-summary-navigation__item-header {
  display: flex;
  align-items: center;
  gap: 0.35rem;
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

.product-summary-navigation__toggle {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 10px;
  border: none;
  background: transparent;
  color: rgb(var(--v-theme-text-neutral-strong));
  transition: all 0.25s ease;
}

.product-summary-navigation__toggle:hover,
.product-summary-navigation__toggle:focus-visible {
  color: rgb(var(--v-theme-text-neutral-strong));
  background: rgba(var(--v-theme-surface-primary-080), 0.5);
}

.product-summary-navigation__submenu {
  list-style: none;
  padding: 0 0 0 1.75rem;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.product-summary-navigation__submenu-link {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  width: 100%;
  padding: 0.4rem 0.6rem;
  border-radius: 10px;
  border: 1px solid transparent;
  background: transparent;
  font-weight: 600;
  font-size: 0.85rem;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.85);
  transition: all 0.2s ease;
}

.product-summary-navigation__submenu-link:hover,
.product-summary-navigation__submenu-link:focus-visible {
  border-color: rgba(var(--v-theme-border-primary-strong), 0.5);
  color: rgb(var(--v-theme-text-neutral-strong));
  background: rgba(var(--v-theme-surface-primary-080), 0.55);
}

.product-summary-navigation__submenu-link--active {
  background: rgba(var(--v-theme-surface-primary-100), 0.7);
  color: rgb(var(--v-theme-text-neutral-strong));
  border-color: rgba(var(--v-theme-border-primary-strong), 0.85);
}

.product-summary-navigation__submenu-icon {
  color: rgb(var(--v-theme-accent-primary-highlight));
}

.product-summary-navigation__submenu-label {
  white-space: nowrap;
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

.product-summary-navigation__admin-panel {
  margin-top: 1.5rem;
  padding: 1rem 0.875rem 1.25rem;
  border-radius: 14px;
  border: 1px solid rgba(var(--v-theme-error), 0.35);
  background: rgba(var(--v-theme-error), 0.08);
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.product-summary-navigation__admin-eyebrow {
  margin: 0;
  font-size: 0.75rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: rgb(var(--v-theme-error));
}

.product-summary-navigation__admin-helper {
  margin: 0;
  font-size: 0.85rem;
  color: rgba(var(--v-theme-error), 0.9);
}

.product-summary-navigation__admin-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.product-summary-navigation__admin-item {
  display: contents;
}

.product-summary-navigation__admin-link {
  width: 100%;
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.6rem 0.75rem;
  border-radius: 12px;
  border: 1px solid rgba(var(--v-theme-error), 0.45);
  background: rgba(var(--v-theme-error), 0.12);
  color: rgb(var(--v-theme-error));
  font-weight: 600;
  font-size: 0.9rem;
  transition: all 0.25s ease;
}

.product-summary-navigation__admin-link:hover,
.product-summary-navigation__admin-link:focus-visible {
  background: rgba(var(--v-theme-error), 0.18);
  border-color: rgba(var(--v-theme-error), 0.6);
}

.product-summary-navigation__admin-link--active {
  background: rgba(var(--v-theme-error), 0.28);
  color: rgb(var(--v-theme-surface-default));
  border-color: rgba(var(--v-theme-error), 0.9);
  box-shadow: inset 0 0 0 1px rgba(var(--v-theme-error), 0.35);
}

.product-summary-navigation__admin-link--active
  .product-summary-navigation__admin-icon {
  color: rgb(var(--v-theme-surface-default));
}

.product-summary-navigation__admin-icon {
  color: rgba(var(--v-theme-error), 0.95);
}

.product-summary-navigation__admin-label {
  white-space: nowrap;
}

.product-summary-navigation--horizontal .product-summary-navigation__link {
  justify-content: center;
}

.product-summary-navigation--horizontal .product-summary-navigation__item {
  min-width: max-content;
}

.product-summary-navigation--horizontal .product-summary-navigation__submenu {
  padding-left: 0.5rem;
}

.product-summary-navigation--horizontal
  .product-summary-navigation__admin-panel {
  margin-top: 1rem;
}

.product-summary-navigation--horizontal
  .product-summary-navigation__admin-list {
  flex-direction: row;
  flex-wrap: wrap;
  gap: 0.75rem;
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

  .product-summary-navigation__submenu {
    padding-left: 1rem;
  }

  .product-summary-navigation__admin-link {
    font-size: 0.85rem;
  }
}
</style>
