<template>
  <div class="nudge-step-subset">
    <div class="nudge-step-subset__header">
      <div class="nudge-step-subset__title-block">
        <v-avatar
          class="nudge-step-subset__group-avatar"
          size="44"
          color="rgba(var(--v-theme-surface-primary-080), 0.9)"
        >
          <v-icon :icon="groupIcon" size="24" />
        </v-avatar>
        <div class="nudge-step-subset__titles">
          <p class="nudge-step-subset__title">{{ group.title }}</p>
          <p v-if="group.description" class="nudge-step-subset__description">
            {{ group.description }}
          </p>
        </div>
      </div>

      <v-chip
        v-if="categoryLabel"
        class="nudge-step-subset__category-chip"
        color="primary"
        variant="tonal"
        size="small"
        @click="emit('return-to-category')"
      >
        <v-icon start :icon="categoryIcon || 'mdi-tag-outline'" />
        {{ categoryLabel }}
      </v-chip>
    </div>

    <v-row dense>
      <v-col v-for="subset in subsets" :key="subset.id" cols="12" sm="6">
        <v-tooltip
          location="top"
          :text="subset.description"
          :disabled="!subset.description"
        >
          <template #activator="{ props: activatorProps }">
            <v-card
              v-bind="activatorProps"
              class="nudge-step-subset__card nudge-toggle-card"
              :class="{
                'nudge-toggle-card--selected': isSelected(subset.id),
              }"
              rounded="lg"
              role="button"
              :aria-pressed="isSelected(subset.id).toString()"
              :aria-label="subset.title"
              tabindex="0"
              @click="toggle(subset.id ?? '')"
              @keydown.enter.prevent="toggle(subset.id ?? '')"
              @keydown.space.prevent="toggle(subset.id ?? '')"
            >
              <div class="nudge-toggle-card__body">
                <div class="nudge-toggle-card__icon-rail">
                  <div class="nudge-toggle-card__icon-shell">
                    <v-icon :icon="getSubsetIcon(subset)" size="24" />
                  </div>
                </div>

                <div class="nudge-toggle-card__content">
                  <p class="nudge-toggle-card__title">{{ subset.title }}</p>
                  <p v-if="subset.caption" class="nudge-toggle-card__caption">
                    {{ subset.caption }}
                  </p>
                </div>

                <div class="nudge-toggle-card__indicator" aria-hidden="true">
                  <v-icon v-if="isSelected(subset.id)" icon="mdi-check" size="20" color="white" />
                </div>
              </div>
            </v-card>
          </template>
        </v-tooltip>
      </v-col>
    </v-row>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type {
  NudgeToolSubsetGroupDto,
  VerticalSubsetDto,
} from '~~/shared/api-client'

const props = defineProps<{
  group: NudgeToolSubsetGroupDto
  subsets: VerticalSubsetDto[]
  modelValue: string[]
  stepNumber: number
  categoryIcon?: string | null
  categoryLabel?: string | null
}>()

const emit = defineEmits<{
  (event: 'update:modelValue', value: string[]): void
  (event: 'continue' | 'return-to-category'): void
}>()

const isSelected = (subsetId?: string | null) =>
  subsetId ? props.modelValue.includes(subsetId) : false

const getSubsetIcon = (subset: VerticalSubsetDto) =>
  subset.mdiIcon ?? 'mdi-sprout'

const groupIcon = computed(() => props.group.mdiIcon ?? 'mdi-format-list-bulleted')

const toggle = (subsetId: string) => {
  const next = new Set(props.modelValue)

  if (next.has(subsetId)) {
    next.delete(subsetId)
  } else {
    next.add(subsetId)
  }

  emit('update:modelValue', Array.from(next))
}
</script>

<style scoped lang="scss">
.nudge-step-subset {
  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    flex-wrap: wrap;
    gap: 12px;
    margin-bottom: 16px;
  }

  &__title-block {
    display: inline-flex;
    align-items: center;
    gap: 10px;
  }

  &__group-avatar {
    border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.45);
  }

  &__titles {
    display: flex;
    flex-direction: column;
    gap: 4px;
  }

  &__title {
    font-size: 1.1rem;
    font-weight: 700;
    margin: 0;
  }

  &__description {
    margin: 0;
    color: rgb(var(--v-theme-text-neutral-secondary));
  }

  &__category-chip {
    font-weight: 700;
  }

  &__card {
    .nudge-toggle-card {
      display: flex;
      flex-direction: column;
      height: 100%;
      border-radius: 16px;
      padding: 0;
      overflow: hidden;
      background: rgb(var(--v-theme-surface-primary-050)) !important;
      border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.4);
      box-shadow: none;
      transition: transform 140ms ease, border-color 160ms ease,
        box-shadow 160ms ease, background-color 160ms ease;
      cursor: pointer;

      &__body {
        display: grid;
        grid-template-columns: 0.32fr 1fr auto;
        align-items: stretch;
        height: 100%;
      }

      &__icon-rail {
        display: flex;
        align-items: center;
        justify-content: center;
        background: rgba(var(--v-theme-accent-supporting), 0.08);
        padding: 14px;
        min-height: 100%;
        transition: background 160ms ease;
      }

      &__icon-shell {
        width: 44px;
        height: 44px;
        border-radius: 14px;
        display: grid;
        place-items: center;
        background: rgba(var(--v-theme-accent-supporting), 0.12);
        color: rgb(var(--v-theme-accent-supporting));
        box-shadow: inset 0 0 0 1px rgba(var(--v-theme-border-primary-strong), 0.2);
        transition: background 160ms ease, color 160ms ease, box-shadow 160ms ease;
      }

      &__content {
        display: flex;
        flex-direction: column;
        gap: 4px;
        padding: 14px 18px;
        justify-content: center;
        min-height: 100%;
      }

      &__title {
        margin: 0;
        font-weight: 700;
        line-height: 1.4;
      }

      &__caption {
        margin: 0;
        color: rgb(var(--v-theme-text-neutral-secondary));
        font-size: 0.95rem;
        line-height: 1.4;
      }

      &__indicator {
        width: 52px;
        display: grid;
        place-items: center;
        background: rgba(var(--v-theme-accent-supporting), 0.12);
        border-left: 1px solid rgba(var(--v-theme-border-primary-strong), 0.4);
        transition: background 160ms ease, border-color 160ms ease;
      }

      &:hover,
      &:focus-visible {
        transform: translateY(-2px);
        border-color: rgba(var(--v-theme-accent-supporting), 0.6);
        box-shadow: 0 10px 20px rgba(var(--v-theme-shadow-primary-600), 0.08);
        outline: none;
      }

      &:focus-visible {
        box-shadow: 0 0 0 3px rgba(var(--v-theme-accent-supporting), 0.25);
      }
    }
  }

  .nudge-toggle-card--selected {
    background: rgba(var(--v-theme-accent-supporting), 0.12) !important;
    border-color: rgb(var(--v-theme-accent-supporting));

    .nudge-toggle-card__icon-rail {
      background: rgba(var(--v-theme-accent-supporting), 0.18);
    }

    .nudge-toggle-card__icon-shell {
      background: rgba(var(--v-theme-accent-supporting), 0.2);
      color: rgb(var(--v-theme-surface-default));
      box-shadow: inset 0 0 0 1px rgba(var(--v-theme-accent-supporting), 0.5);
    }

    .nudge-toggle-card__indicator {
      background: rgb(var(--v-theme-accent-supporting));
      border-color: rgb(var(--v-theme-accent-supporting));
      color: rgb(var(--v-theme-surface-default));
    }
  }
}
</style>
