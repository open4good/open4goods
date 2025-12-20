<template>
  <v-sheet class="release-accordion" color="transparent">
    <v-progress-linear
      v-if="loading"
      indeterminate
      color="primary"
      class="mb-4"
      :aria-label="t('releases.loading')"
      role="progressbar"
    />

    <v-alert
      v-if="error"
      type="error"
      variant="tonal"
      border="start"
      prominent
      class="mb-4"
      role="alert"
    >
      <div class="d-flex align-center gap-2">
        <span>{{ t('releases.errors.loadFailed') }}</span>
        <v-btn
          size="small"
          variant="tonal"
          color="primary"
          @click="$emit('retry')"
        >
          {{ t('common.actions.retry') }}
        </v-btn>
      </div>
    </v-alert>

    <v-alert
      v-else-if="!loading && releases.length === 0"
      type="info"
      variant="tonal"
      border="start"
      class="mb-4"
      role="status"
    >
      {{ t('releases.empty') }}
    </v-alert>

    <v-expansion-panels
      v-model="expanded"
      elevation="0"
      variant="accordion"
      class="release-accordion__panels"
    >
      <v-expansion-panel
        v-for="(release, index) in releases"
        :key="release.slug"
        :value="release.slug"
        class="release-accordion__panel"
      >
        <v-expansion-panel-title class="release-accordion__title" hide-actions>
          <template #default="{ expanded: isExpanded, toggle }">
            <div class="release-accordion__title-content">
              <div class="release-accordion__meta">
                <div class="release-accordion__date-row">
                  <span class="release-accordion__eyebrow">
                    {{ formatPublishedDate(release.publishedAt) }}
                  </span>
                </div>
                <span class="release-accordion__name">{{ release.name }}</span>
              </div>

              <div class="release-accordion__actions">
                <span
                  v-if="index === 0"
                  class="release-accordion__latest-badge"
                  role="status"
                >
                  <span class="release-accordion__badge-dot" aria-hidden="true" />
                  <span class="release-accordion__badge-label">
                    {{ t('releases.latest') }}
                  </span>
                </span>

                <v-btn
                  variant="text"
                  color="primary"
                  density="comfortable"
                  class="release-accordion__toggle"
                  :aria-label="
                    isExpanded
                      ? t('releases.actions.collapse')
                      : t('releases.actions.expand')
                  "
                  :icon="isExpanded ? 'mdi-chevron-up' : 'mdi-chevron-down'"
                  @click.stop="toggle"
                />
              </div>
            </div>
          </template>
        </v-expansion-panel-title>
        <v-expansion-panel-text>
          <!-- eslint-disable vue/no-v-html -->
          <div
            class="release-accordion__content"
            v-html="release.contentHtml"
          />
          <!-- eslint-enable vue/no-v-html -->
        </v-expansion-panel-text>
      </v-expansion-panel>
    </v-expansion-panels>
  </v-sheet>
</template>

<script setup lang="ts">
import type { ReleaseNote } from '~~/types/releases'

const props = defineProps<{
  releases: ReleaseNote[]
  loading?: boolean
  error?: unknown
}>()

defineEmits<{
  (event: 'retry'): void
}>()

const { t, locale } = useI18n()

const expanded = ref<string[]>([])

watch(
  () => props.releases,
  list => {
    if (list.length > 0) {
      expanded.value = [list[0]?.slug]
    }
  },
  { immediate: true }
)

const formatPublishedDate = (isoDate: string): string => {
  try {
    const parsed = new Date(isoDate)

    return new Intl.DateTimeFormat(locale.value, {
      dateStyle: 'medium',
    }).format(parsed)
  } catch {
    return isoDate
  }
}
</script>

<style scoped lang="sass">
.release-accordion
  &__panels
    background: transparent
    border: 1px solid rgba(var(--v-theme-on-surface), 0.08)
    border-radius: 18px
    overflow: hidden

  &__panel
    &::after
      display: none

  &__title
    padding: 16px 20px

  &__title-content
    display: flex
    justify-content: space-between
    align-items: flex-start
    gap: 16px
    flex-wrap: wrap

  &__actions
    display: flex
    align-items: center
    gap: 10px
    flex-shrink: 0

  &__meta
    display: flex
    flex-direction: column
    gap: 6px
    min-width: 0

  &__date-row
    display: flex
    align-items: center
    gap: 8px

  &__eyebrow
    text-transform: uppercase
    letter-spacing: 0.08em
    font-size: 0.75rem
    color: rgba(var(--v-theme-on-surface), 0.6)

  &__name
    font-weight: 700
    font-size: 1.05rem
    color: rgb(var(--v-theme-on-surface))
    word-break: break-word

  &__latest-badge
    display: inline-flex
    align-items: center
    gap: 8px
    padding: 6px 12px
    border-radius: 999px
    border: 1px solid #d0d7de
    background: linear-gradient(180deg, #f6f8fa, #eaeef2)
    box-shadow: inset 0 1px 0 #ffffff, 0 1px 0 rgba(0, 0, 0, 0.04)
    color: #24292f
    font-weight: 700
    font-size: 0.85rem

  &__badge-dot
    width: 8px
    height: 8px
    border-radius: 50%
    background: #2da44e
    box-shadow: 0 0 0 2px rgba(45, 164, 78, 0.2)

  &__badge-label
    white-space: nowrap

  &__toggle
    color: rgba(var(--v-theme-on-surface), 0.72)

  &__content
    padding: 10px 4px 20px

    :deep(h1),
    :deep(h2),
    :deep(h3)
      margin: 16px 0 10px
      font-weight: 700

    :deep(p)
      margin: 10px 0
      color: rgba(var(--v-theme-on-surface), 0.8)

    :deep(ul)
      padding-left: 24px
      margin: 10px 0

    :deep(li)
      margin: 6px 0
</style>
