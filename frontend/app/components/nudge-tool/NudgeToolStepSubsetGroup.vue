<template>
  <div class="nudge-step-subset">
    <div class="nudge-step-subset__header">
      <div>
        <div class="nudge-step-subset__eyebrow" v-if="group.mdiIcon || group.title">
          <v-icon v-if="group.mdiIcon" :icon="group.mdiIcon" size="18" class="me-1" />
          <span>{{ group.title }}</span>
        </div>
        <p class="nudge-step-subset__description">{{ group.description }}</p>
      </div>

      <v-btn color="primary" @click="emit('continue')">
        {{ group.ctaLabel || $t('nudge-tool.actions.continue') }}
      </v-btn>
    </div>

    <v-row dense>
      <v-col
        v-for="subset in subsets"
        :key="subset.id"
        cols="12"
        sm="6"
      >
        <v-card
          class="nudge-step-subset__card"
          :elevation="modelValue.includes(subset.id ?? '') ? 6 : 2"
          :variant="modelValue.includes(subset.id ?? '') ? 'elevated' : 'tonal'"
          rounded="xl"
          role="button"
          :aria-pressed="modelValue.includes(subset.id ?? '')"
          @click="toggle(subset.id ?? '')"
        >
          <div class="nudge-step-subset__media" v-if="subset.image">
            <v-img :src="subset.image" height="120" cover />
          </div>
          <div class="nudge-step-subset__body">
            <p class="nudge-step-subset__title">{{ subset.title }}</p>
            <p class="nudge-step-subset__caption">{{ subset.caption }}</p>
            <p class="nudge-step-subset__description">{{ subset.description }}</p>
          </div>
        </v-card>
      </v-col>
    </v-row>
  </div>
</template>

<script setup lang="ts">
import type { NudgeToolSubsetGroupDto, VerticalSubsetDto } from '~~/shared/api-client'

const props = defineProps<{
  group: NudgeToolSubsetGroupDto
  subsets: VerticalSubsetDto[]
  modelValue: string[]
}>()

const emit = defineEmits<{ (event: 'update:modelValue', value: string[]): void; (event: 'continue'): void }>()

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

  &__eyebrow {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    font-weight: 700;
  }

  &__description {
    margin: 4px 0 0;
    color: rgb(var(--v-theme-text-neutral-secondary));
  }

  &__card {
    display: flex;
    flex-direction: column;
    gap: 8px;
    height: 100%;
  }

  &__media {
    border-radius: 12px;
    overflow: hidden;
  }

  &__body {
    padding: 12px 16px;
    display: flex;
    flex-direction: column;
    gap: 4px;
  }

  &__title {
    margin: 0;
    font-weight: 700;
  }

  &__caption {
    margin: 0;
    color: rgb(var(--v-theme-text-neutral-secondary));
  }
}
</style>
