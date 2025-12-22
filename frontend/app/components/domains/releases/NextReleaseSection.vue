<template>
  <v-sheet
    v-if="isEnabled"
    class="next-release-section"
    :color="backgroundColor"
    rounded="xl"
    elevation="0"
  >
    <div class="next-release-section__content">
      <div class="next-release-section__header">
        <v-chip
          color="primary"
          size="small"
          variant="flat"
          density="comfortable"
          class="next-release-section__eyebrow"
          prepend-icon="mdi-rocket-launch"
        >
          {{ t('releases.nextRelease.eyebrow') }}
        </v-chip>
        <h2 class="next-release-section__title">
          {{ t('releases.nextRelease.title') }}
        </h2>
      </div>

      <div class="next-release-section__body">
        <div class="next-release-section__meta">
          <div
            v-if="releaseVersion"
            class="next-release-section__meta-item"
            role="status"
          >
            <v-icon
              icon="mdi-tag-outline"
              size="small"
              class="next-release-section__meta-icon"
            />
            <span class="next-release-section__meta-text">{{
              releaseVersion
            }}</span>
          </div>
          <div
            v-if="targetDate"
            class="next-release-section__meta-item"
            role="status"
          >
            <v-icon
              icon="mdi-calendar-clock"
              size="small"
              class="next-release-section__meta-icon"
            />
            <span class="next-release-section__meta-text">{{ targetDate }}</span>
          </div>
        </div>

        <p class="next-release-section__description">
          {{ t('releases.nextRelease.description') }}
        </p>

        <div
          v-if="xwikiContentId"
          class="next-release-section__xwiki-content"
          aria-label="Additional release information"
        >
          <TextContent :bloc-id="xwikiContentId" />
        </div>

        <div
          v-if="hasExternalLinks"
          class="next-release-section__actions"
          role="group"
          aria-label="External links"
        >
          <ExternalLinkButton
            v-for="(link, index) in externalLinks"
            :key="`link-${index}`"
            :link="link"
            variant="tonal"
            size="default"
          />
        </div>
      </div>
    </div>
  </v-sheet>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import ExternalLinkButton from '~/components/shared/widgets/ExternalLinkButton.vue'
import TextContent from '~/components/domains/content/TextContent.vue'
import type { ExternalLink } from '~~/types/next-release'

withDefaults(
  defineProps<{
    xwikiContentId?: string
    backgroundColor?: string
  }>(),
  {
    xwikiContentId: undefined,
    backgroundColor: 'surface-primary-050',
  }
)

const { t } = useI18n()

const isEnabled = computed(() => {
  const enabled = t('releases.nextRelease.enabled')
  return enabled === 'true' || enabled === true
})

const releaseVersion = computed(() => {
  const version = String(t('releases.nextRelease.releaseVersion'))
  return version && version !== 'releases.nextRelease.releaseVersion'
    ? version
    : ''
})

const targetDate = computed(() => {
  const date = String(t('releases.nextRelease.targetDate'))
  return date && date !== 'releases.nextRelease.targetDate' ? date : ''
})

const linkedInUrl = computed(() => {
  const url = String(t('releases.nextRelease.linkedIn.url'))
  return url && url !== 'releases.nextRelease.linkedIn.url' ? url.trim() : ''
})

const linkedInLabel = computed(() => {
  const label = String(t('releases.nextRelease.linkedIn.label'))
  return label && label !== 'releases.nextRelease.linkedIn.label' ? label : ''
})

const externalLinks = computed<ExternalLink[]>(() => {
  const links: ExternalLink[] = []

  if (linkedInUrl.value && linkedInLabel.value) {
    links.push({
      url: linkedInUrl.value,
      label: linkedInLabel.value,
      type: 'linkedin',
      icon: 'mdi-linkedin',
    })
  }

  return links
})

const hasExternalLinks = computed(() => externalLinks.value.length > 0)
</script>

<style scoped lang="sass">
.next-release-section
  position: relative
  overflow: hidden
  margin-bottom: 48px

  &::before
    content: ''
    position: absolute
    top: 0
    left: 0
    right: 0
    height: 4px
    background: linear-gradient(90deg, rgb(var(--v-theme-accent-primary-highlight)) 0%, rgb(var(--v-theme-accent-supporting)) 100%)
    border-radius: 12px 12px 0 0

.next-release-section__content
  padding: clamp(1.75rem, 3vw, 2.5rem)
  display: flex
  flex-direction: column
  gap: 1.5rem

.next-release-section__header
  display: flex
  flex-direction: column
  gap: 0.75rem
  align-items: flex-start

.next-release-section__eyebrow
  font-weight: 700
  letter-spacing: 0.05em

.next-release-section__title
  margin: 0
  font-size: clamp(1.5rem, 3vw, 2rem)
  font-weight: 700
  line-height: 1.2
  color: rgb(var(--v-theme-text-neutral-strong))

.next-release-section__body
  display: flex
  flex-direction: column
  gap: 1.25rem

.next-release-section__meta
  display: flex
  flex-wrap: wrap
  gap: 1rem
  align-items: center

.next-release-section__meta-item
  display: flex
  align-items: center
  gap: 0.5rem
  padding: 0.5rem 0.75rem
  background: rgba(var(--v-theme-primary), 0.08)
  border-radius: 8px

.next-release-section__meta-icon
  color: rgb(var(--v-theme-primary))

.next-release-section__meta-text
  font-size: 0.9rem
  font-weight: 600
  color: rgb(var(--v-theme-text-neutral-strong))

.next-release-section__description
  margin: 0
  font-size: 1.05rem
  line-height: 1.6
  color: rgb(var(--v-theme-text-neutral-secondary))

.next-release-section__xwiki-content
  padding: 1rem 0
  border-top: 1px solid rgba(var(--v-theme-on-surface), 0.08)

.next-release-section__actions
  display: flex
  flex-wrap: wrap
  gap: 0.75rem
  padding-top: 0.5rem

@media (min-width: 600px)
  .next-release-section__header
    flex-direction: row
    align-items: center
    gap: 1rem

  .next-release-section__eyebrow
    order: 0

@media (max-width: 599px)
  .next-release-section__content
    padding: clamp(1.25rem, 4vw, 1.75rem)

  .next-release-section__meta
    flex-direction: column
    align-items: flex-start
    gap: 0.75rem

  .next-release-section__meta-item
    width: 100%
</style>
