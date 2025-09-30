<script setup lang="ts">
import { computed, watchEffect } from 'vue'
import '~/assets/css/text-content.css'
import { useFullPage } from '~/composables/cms/useFullPage'
import { useAuth } from '~/composables/useAuth'
import { matchLocalizedWikiRouteByPath } from '~~/shared/utils/localized-routes'

const route = useRoute()
const { t } = useI18n()
const config = useRuntimeConfig()

const matchedRoute = computed(() => matchLocalizedWikiRouteByPath(route.path))

if (!matchedRoute.value) {
  throw createError({ statusCode: 404, statusMessage: 'CMS page not found' })
}

const pageId = computed(() => matchedRoute.value?.pageId ?? null)

const fullPageState = await useFullPage(pageId)

const {
  width,
  pageTitle,
  metaTitle,
  metaDescription,
  editLink,
  pending,
  error,
  refresh,
  page,
  data,
} = fullPageState

const normaliseHtmlContent = (value?: string | null) => {
  if (typeof value !== 'string') {
    return undefined
  }

  return value.trim() === '' ? undefined : value
}

const htmlContentStateKey = `cms-full-page:html:${pageId.value ?? 'empty'}`

const htmlContentState = useState<string | undefined>(
  htmlContentStateKey,
  () =>
    normaliseHtmlContent(data.value?.htmlContent) ??
    normaliseHtmlContent(page.value?.htmlContent),
)

watchEffect(() => {
  const fromData = normaliseHtmlContent(data.value?.htmlContent)
  const fallback = normaliseHtmlContent(page.value?.htmlContent)

  if (fromData !== undefined) {
    htmlContentState.value = fromData
    return
  }

  if (fallback !== undefined) {
    htmlContentState.value = fallback
    return
  }

  if (!pending.value) {
    htmlContentState.value = undefined
  }
})

const htmlContent = computed(() => htmlContentState.value)

const { isLoggedIn, hasRole } = useAuth()
const allowedRoles = computed(() => (config.public.editRoles as string[]) || [])
const canEdit = computed(() => {
  const link = editLink.value
  return isLoggedIn.value && !!link && allowedRoles.value.some(role => hasRole(role))
})

const containerClass = computed(() => [
  'cms-page__container',
  `cms-page__container--${width.value}`,
])

const containerMaxWidth = computed(() => {
  switch (width.value) {
    case 'container':
      return 'lg'
    case 'container-semi-fluid':
      return 'xl'
    default:
      return undefined
  }
})

const isFluidContainer = computed(() => width.value === 'container-fluid')

useSeoMeta({
  title: () => metaTitle.value || pageTitle.value,
  description: () => metaDescription.value || undefined,
  ogTitle: () => metaTitle.value || pageTitle.value,
  ogDescription: () => metaDescription.value || undefined,
})
</script>

<template>
  <div class="cms-page">
    <section class="cms-page__hero" role="banner">
      <v-container class="cms-page__hero-container" fluid>
        <div class="cms-page__hero-content">
          <p class="cms-page__hero-eyebrow">
            {{ t('cms.page.heroEyebrow') }}
          </p>
          <h1 class="cms-page__hero-title">{{ pageTitle }}</h1>
          <p v-if="metaDescription" class="cms-page__hero-description">
            {{ metaDescription }}
          </p>
          <div class="cms-page__hero-actions">
            <v-btn
              v-if="canEdit"
              :href="editLink || undefined"
              target="_blank"
              rel="noopener noreferrer"
              prepend-icon="mdi-open-in-new"
              variant="outlined"
              color="white"
              class="cms-page__edit-button"
            >
              {{ t('cms.page.edit') }}
            </v-btn>
          </div>
        </div>
      </v-container>
    </section>

    <v-progress-linear
      v-if="pending"
      color="primary"
      indeterminate
      class="cms-page__loader"
      :aria-label="t('cms.page.loading')"
      role="status"
    />

    <v-container
      v-if="!pending"
      :class="containerClass"
      :max-width="containerMaxWidth"
      :fluid="isFluidContainer"
    >
      <v-sheet class="cms-page__content" elevation="0" rounded="xl">
        <div v-if="error" class="cms-page__error" role="alert">
          <v-alert type="error" variant="tonal" border="start" prominent>
            {{ t('cms.page.error') }}
          </v-alert>
          <v-btn color="primary" variant="tonal" @click="refresh">
            {{ t('common.actions.retry') }}
          </v-btn>
        </div>

        <div v-else class="cms-page__html" role="article" aria-label="CMS content">
          <!-- eslint-disable-next-line vue/no-v-html -->
          <div class="xwiki-sandbox" v-html="htmlContent" />
        </div>
      </v-sheet>
    </v-container>
  </div>
</template>

<style lang="sass" scoped>
.cms-page
  display: flex
  flex-direction: column
  gap: clamp(2rem, 4vw, 4rem)
  background: rgb(var(--v-theme-surface-default))

.cms-page__hero
  position: relative
  overflow: hidden
  padding-block: clamp(3rem, 6vw, 5.5rem)
  background: linear-gradient(
    135deg,
    rgba(var(--v-theme-hero-gradient-start), 0.92) 0%,
    rgba(var(--v-theme-hero-gradient-mid), 0.88) 45%,
    rgba(var(--v-theme-hero-gradient-end), 0.9) 100%
  )
  color: rgb(var(--v-theme-hero-overlay-strong))

.cms-page__hero::after
  content: ''
  position: absolute
  inset: 0
  background: radial-gradient(
    circle at 20% 20%,
    rgba(var(--v-theme-hero-overlay-soft), 0.2) 0%,
    transparent 60%
  )
  pointer-events: none

.cms-page__hero-container
  position: relative
  z-index: 1

.cms-page__hero-content
  display: flex
  flex-direction: column
  gap: 1rem
  max-width: min(72ch, 100%)

.cms-page__hero-eyebrow
  font-size: 0.875rem
  letter-spacing: 0.08em
  text-transform: uppercase
  margin: 0
  color: rgba(var(--v-theme-hero-overlay-soft), 0.9)

.cms-page__hero-title
  margin: 0
  font-size: clamp(2.25rem, 4vw, 3.5rem)
  font-weight: 700
  line-height: 1.1

.cms-page__hero-description
  margin: 0
  font-size: clamp(1.05rem, 1.8vw, 1.25rem)
  color: rgba(var(--v-theme-hero-overlay-soft), 0.92)

.cms-page__hero-actions
  display: flex
  gap: 0.75rem
  flex-wrap: wrap
  margin-top: 0.5rem

.cms-page__edit-button
  color: rgb(var(--v-theme-hero-overlay-strong))
  border-color: rgba(var(--v-theme-hero-overlay-soft), 0.6)

.cms-page__loader
  margin-inline: auto
  width: min(900px, 90vw)

.cms-page__container
  padding-block: clamp(2rem, 4vw, 4rem)

.cms-page__container--container
  max-width: min(960px, 100%) !important

.cms-page__container--container-semi-fluid
  max-width: min(1100px, 92vw) !important

.cms-page__content
  padding: clamp(1.75rem, 3vw, 3rem)
  border-radius: 24px
  background: rgb(var(--v-theme-surface-default))
  box-shadow: 0 24px 48px rgba(var(--v-theme-shadow-primary-600), 0.12)
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.4)

.cms-page__error
  display: flex
  flex-direction: column
  gap: 1rem

.cms-page__html
  font-size: 1.05rem
  line-height: 1.65
  color: rgb(var(--v-theme-text-neutral-strong))

.cms-page__html :deep(h2)
  margin-top: 2.5rem
  font-size: clamp(1.6rem, 2.5vw, 2rem)
  font-weight: 700
  color: rgb(var(--v-theme-text-neutral-strong))

.cms-page__html :deep(p)
  margin-block: 1.1rem
  color: rgba(var(--v-theme-text-neutral-secondary), 0.98)

.cms-page__html :deep(a)
  color: rgb(var(--v-theme-accent-primary-highlight))
  text-decoration: underline
  text-decoration-thickness: 2px
  text-decoration-color: rgba(var(--v-theme-accent-primary-highlight), 0.4)

.cms-page__html :deep(ul)
  padding-inline-start: 1.5rem
  margin-block: 1.25rem

.cms-page__html :deep(li)
  margin-block: 0.5rem

@media (max-width: 600px)
  .cms-page__hero
    padding-block: 2.5rem

  .cms-page__content
    padding: clamp(1.5rem, 4vw, 2rem)

  .cms-page__html
    font-size: 1rem
</style>
