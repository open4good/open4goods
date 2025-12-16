<template>
  <aside
    class="guide-sticky-sidebar"
    :aria-label="sidebarAriaLabel"
    data-test="guide-sticky-sidebar"
  >
    <div class="guide-sticky-sidebar__inner">
      <NuxtLink
        v-if="categoryPath"
        :to="categoryPath"
        class="guide-sticky-sidebar__back"
        data-test="guide-sidebar-back"
      >
        <v-btn
          block
          color="primary"
          prepend-icon="mdi-arrow-left"
          size="small"
          variant="tonal"
        >
          {{ backToCategoryLabel }}
        </v-btn>
      </NuxtLink>

      <div
        v-if="showGuides"
        class="guide-sticky-sidebar__panel"
        data-test="guide-sidebar-guides"
      >
        <h2 class="guide-sticky-sidebar__title">
          {{ guidesTitle }}
        </h2>
        <StickySectionNavigation
          :sections="guideSections"
          :aria-label="guidesAriaLabel"
          :sticky="false"
          @navigate="onGuideNavigate"
        />
      </div>

      <div
        v-if="showPosts"
        class="guide-sticky-sidebar__panel"
        data-test="guide-sidebar-posts"
      >
        <h2 class="guide-sticky-sidebar__title">
          {{ postsTitle }}
        </h2>
        <StickySectionNavigation
          :sections="postSections"
          :aria-label="postsAriaLabel"
          :sticky="false"
          @navigate="onPostNavigate"
        />
      </div>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import StickySectionNavigation from '~/components/shared/ui/StickySectionNavigation.vue'

interface NavigationEntry {
  title: string
  to: string
}

const MAX_LABEL_LENGTH = 60

const props = defineProps<{
  categoryName: string
  categoryPath?: string | null
  guides?: NavigationEntry[]
  posts?: NavigationEntry[]
}>()

const router = useRouter()
const { t } = useI18n()

const truncateLabel = (value: string) => {
  const trimmed = value.trim()

  if (!trimmed.length) {
    return ''
  }

  if (trimmed.length <= MAX_LABEL_LENGTH) {
    return trimmed
  }

  return `${trimmed.slice(0, MAX_LABEL_LENGTH - 1).trimEnd()}…`
}

const normalizedGuides = computed(() =>
  (props.guides ?? [])
    .filter(item => item.title?.trim().length && item.to?.trim().length)
    .map(item => ({
      id: item.to,
      href: item.to,
      label: truncateLabel(item.title),
    }))
)

const normalizedPosts = computed(() =>
  (props.posts ?? [])
    .filter(item => item.title?.trim().length && item.to?.trim().length)
    .map(item => ({
      id: item.to,
      href: item.to,
      label: truncateLabel(item.title),
    }))
)

const guideSections = computed(() =>
  normalizedGuides.value.map(item => ({
    id: item.id,
    label: item.label,
    icon: 'mdi-compass-outline',
  }))
)

const postSections = computed(() =>
  normalizedPosts.value.map(item => ({
    id: item.id,
    label: item.label,
    icon: 'mdi-post-outline',
  }))
)

const guideLookup = computed(
  () => new Map(normalizedGuides.value.map(item => [item.id, item.href]))
)
const postLookup = computed(
  () => new Map(normalizedPosts.value.map(item => [item.id, item.href]))
)

const showGuides = computed(() => guideSections.value.length > 0)
const showPosts = computed(() => postSections.value.length > 0)

const sidebarAriaLabel = computed(() =>
  t('category.documentation.sidebarAria', { category: props.categoryName })
)

const guidesTitle = computed(() => t('category.documentation.guidesTitle'))
const postsTitle = computed(() => t('category.documentation.postsTitle'))
const guidesAriaLabel = computed(() =>
  t('category.documentation.guidesAria', { category: props.categoryName })
)
const postsAriaLabel = computed(() =>
  t('category.documentation.postsAria', { category: props.categoryName })
)
const backToCategoryLabel = computed(() =>
  t('category.documentation.backToCategory', { category: props.categoryName })
)

const onGuideNavigate = (id: string) => {
  const target = guideLookup.value.get(id)

  if (target) {
    router.push(target)
  }
}

const onPostNavigate = (id: string) => {
  const target = postLookup.value.get(id)

  if (target) {
    router.push(target)
  }
}
</script>

<style scoped lang="sass">
.guide-sticky-sidebar
  position: relative
  display: block
  color: rgb(var(--v-theme-text-neutral-secondary))

  &__inner
    position: sticky
    top: clamp(80px, 12vw, 104px)
    display: flex
    flex-direction: column
    gap: 1.5rem

  &__back
    text-decoration: none

  &__panel
    display: flex
    flex-direction: column
    gap: 0.75rem
    padding: 1rem
    border-radius: 20px
    background: rgba(var(--v-theme-surface-glass), 0.9)
    box-shadow: 0 10px 30px rgba(15, 23, 42, 0.08)
    backdrop-filter: blur(10px)

  &__title
    margin: 0
    font-size: 0.95rem
    font-weight: 600
    color: rgb(var(--v-theme-text-neutral-strong))

@media (max-width: 960px)
  .guide-sticky-sidebar
    margin-bottom: 1.5rem

    &__inner
      position: static
      gap: 1rem

    &__panel
      padding: 0.75rem
      border-radius: 16px
      box-shadow: none
      background: rgba(var(--v-theme-surface-glass), 0.85)
</style>
