<template>
  <div class="cms-page" :class="{ 'cms-page--wide': isWideLayout }">
    <section class="cms-page__hero" role="banner">
      <v-container class="cms-page__hero-container" fluid>
        <v-sheet class="cms-page__hero-wrapper" elevation="0">
          <div v-if="hasHeroImage" class="cms-page__hero-media" aria-hidden="true">
            <v-img :src="heroImage" alt="" class="cms-page__hero-image" cover>
              <template #placeholder>
                <v-skeleton-loader type="image" class="cms-page__hero-image-placeholder" />
              </template>
            </v-img>
          </div>

          <div class="cms-page__hero-content">
            <CategoryNavigationBreadcrumbs
              v-if="hasHeroBreadcrumbs"
              v-bind="heroBreadcrumbProps"
              class="cms-page__hero-breadcrumbs"
            />

            <div class="cms-page__hero-copy">
              <h1 class="cms-page__hero-title">{{ effectivePageTitle }}</h1>
              <p v-if="effectiveMetaDescription" class="cms-page__hero-description">
                {{ effectiveMetaDescription }}
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
          </div>
        </v-sheet>
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
      <div
        class="cms-page__layout"
        :class="{
          'cms-page__layout--with-sidebar': hasSidebar,
          'cms-page__layout--wide': isWideLayout,
        }"
      >
        <aside v-if="hasSidebar" class="cms-page__sidebar">
          <slot name="sidebar" />
        </aside>

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
      </div>
    </v-container>
  </div>
</template>

<script setup lang="ts">
import { computed, useSlots } from 'vue'
import { useI18n } from 'vue-i18n'
import '~/assets/css/text-content.css'
import CategoryNavigationBreadcrumbs from '~/components/category/navigation/CategoryNavigationBreadcrumbs.vue'
import { useFullPage } from '~/composables/cms/useFullPage'
import { useAuth } from '~/composables/useAuth'

type BreadcrumbInput = {
  title?: string | null
  link?: string | null
}

const props = defineProps<{
  pageId: string | null
  fallbackTitle?: string | null
  fallbackDescription?: string | null
  breadcrumbs?: BreadcrumbInput[] | null
  heroImage?: string | null
  layoutVariant?: 'default' | 'wide'
}>()

const { t } = useI18n()
const config = useRuntimeConfig()
const slots = useSlots()

const resolvedPageId = computed(() => props.pageId?.trim() ?? null)

if (!resolvedPageId.value) {
  throw createError({ statusCode: 404, statusMessage: 'CMS page not found' })
}

const {
  width,
  pageTitle,
  metaTitle,
  metaDescription,
  htmlContent,
  editLink,
  pending,
  error,
  refresh,
} = await useFullPage(resolvedPageId)

const effectivePageTitle = computed(() => pageTitle.value || props.fallbackTitle || '')
const effectiveMetaDescription = computed(
  () => metaDescription.value || props.fallbackDescription || '',
)

const rawBreadcrumbs = computed(() => props.breadcrumbs ?? [])

const heroBreadcrumbs = computed(() => {
  const items = rawBreadcrumbs.value
    .map((item) => ({
      title: item?.title?.trim() ?? '',
      link: item?.link?.trim() ?? '',
    }))
    .filter((item) => item.title.length)

  return items.map((item, index) => ({
    title: item.title,
    link: index === items.length - 1 ? undefined : item.link || undefined,
  }))
})

const hasHeroBreadcrumbs = computed(() => heroBreadcrumbs.value.length > 0)

const heroBreadcrumbProps = computed(() => ({
  items: heroBreadcrumbs.value,
  ariaLabel: t('category.hero.breadcrumbAriaLabel'),
}))

const hasSidebar = computed(() => Boolean(slots.sidebar))

const isWideLayout = computed(() => (props.layoutVariant ?? 'default') === 'wide')
const heroImage = computed(() => props.heroImage?.trim() ?? '')
const hasHeroImage = computed(() => heroImage.value.length > 0)

const canonicalUrl = useCanonicalUrl()

const { isLoggedIn, hasRole } = useAuth()
const allowedRoles = computed(() => (config.public.editRoles as string[]) || [])
const canEdit = computed(() => {
  const link = editLink.value
  return isLoggedIn.value && !!link && allowedRoles.value.some(role => hasRole(role))
})

const containerClass = computed(() => [
  'cms-page__container',
  `cms-page__container--${width.value}`,
  { 'cms-page__container--wide': isWideLayout.value },
])

const containerMaxWidth = computed(() => {
  if (isWideLayout.value) {
    return undefined
  }

  switch (width.value) {
    case 'container':
      return 'lg'
    case 'container-semi-fluid':
      return 'xl'
    default:
      return undefined
  }
})

const isFluidContainer = computed(
  () => isWideLayout.value || width.value === 'container-fluid',
)

useSeoMeta({
  title: () => metaTitle.value || effectivePageTitle.value,
  description: () => effectiveMetaDescription.value || undefined,
  ogTitle: () => metaTitle.value || effectivePageTitle.value,
  ogDescription: () => effectiveMetaDescription.value || undefined,
  ogUrl: () => canonicalUrl.value || undefined,
})

useHead(() => ({
  link: canonicalUrl.value
    ? [
        {
          rel: 'canonical',
          href: canonicalUrl.value,
        },
      ]
    : [],
}))
</script>

<style scoped lang="sass">
.cms-page
  display: flex
  flex-direction: column
  gap: clamp(2rem, 4vw, 4rem)
  background: rgb(var(--v-theme-surface-default))

.cms-page__hero
  position: relative
  overflow: hidden
  padding-block: clamp(3rem, 6vw, 3.5rem)
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

.cms-page__hero-wrapper
  position: relative
  display: grid
  grid-template-columns: minmax(0, 1fr)
  gap: 1.5rem
  overflow: hidden
  min-height: clamp(180px, 28vw, 260px)
  background: inherit
  border-radius: clamp(18px, 3vw, 28px)
  color: inherit

.cms-page__hero-media
  display: none
  position: relative
  align-items: center
  justify-content: center
  border-radius: clamp(16px, 3vw, 24px)
  overflow: hidden

.cms-page__hero-image,
.cms-page__hero-image-placeholder
  width: 100%
  height: 100%

.cms-page__hero-content
  display: flex
  flex-direction: column
  gap: 1rem
  align-items: center

.cms-page__hero-breadcrumbs
  color: rgba(var(--v-theme-hero-overlay-strong), 0.9)

  :deep(.category-navigation-breadcrumbs)
    color: inherit

  :deep(.category-navigation-breadcrumbs__separator)
    color: rgba(var(--v-theme-hero-overlay-strong), 0.6)

  :deep(.category-navigation-breadcrumbs__current)
    color: rgba(var(--v-theme-hero-overlay-strong), 0.95)

  :deep(.category-navigation-breadcrumbs__link)
    color: inherit

  :deep(.category-navigation-breadcrumbs__link--interactive:hover),
  :deep(.category-navigation-breadcrumbs__link--interactive:focus-visible)
    color: rgba(var(--v-theme-hero-overlay-strong), 0.95)
    text-decoration: underline

.cms-page__hero-copy
  display: flex
  flex-direction: column
  gap: 0.75rem
  align-items: center

.cms-page__hero-title
  margin: 0
  font-size: clamp(2.25rem, 4vw, 3.25rem)
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
  padding-block: clamp(1.25rem, 2vw, 2rem)
  padding-inline: clamp(1rem, 4vw, 3rem)
  margin-inline: auto

.cms-page__container--container
  max-width: min(960px, 100%) !important

.cms-page__container--container-semi-fluid
  max-width: min(1560px, 100%) !important

.cms-page__container--wide
  max-width: min(1560px, 100%) !important

.cms-page--wide .cms-page__hero-container,
.cms-page--wide .cms-page__container
  max-width: min(1560px, 100%)
  margin-inline: auto

.cms-page__layout
  display: grid
  gap: 1.5rem

.cms-page__layout--with-sidebar
  grid-template-columns: minmax(0, 1fr)

.cms-page__sidebar
  order: -1

.cms-page__content
  padding: clamp(1.75rem, 3vw, 3rem)
  background: rgb(var(--v-theme-surface-glass))
  min-height: 320px
  display: flex
  flex-direction: column
  gap: 1.5rem

.cms-page__error
  display: flex
  flex-direction: column
  gap: 1rem
  align-items: flex-start

.cms-page__html
  display: flex
  flex-direction: column
  gap: 1.5rem

@media (min-width: 960px)
  .cms-page__hero-wrapper
    gap: clamp(1.25rem, 3vw, 3rem)

  .cms-page__hero-media
    display: flex

  .cms-page__layout
    gap: 2.5rem

  .cms-page__layout--with-sidebar
    grid-template-columns: clamp(220px, 26vw, 320px) minmax(0, 1fr)
    align-items: start

  .cms-page__layout--with-sidebar.cms-page__layout--wide
    grid-template-columns: minmax(260px, 320px) minmax(0, 1fr)
    column-gap: clamp(1.5rem, 3vw, 2rem)

  .cms-page__sidebar
    order: 0

@media (min-width: 1280px)
  .cms-page__layout--with-sidebar.cms-page__layout--wide
    grid-template-columns: minmax(280px, 320px) minmax(0, 1fr)
    column-gap: clamp(1.75rem, 3vw, 2.5rem)

@media (max-width: 959px)
  .cms-page__hero-wrapper
    border-radius: clamp(12px, 4vw, 20px)

  .cms-page__sidebar
    margin-bottom: 1.5rem
    order: 0
</style>
