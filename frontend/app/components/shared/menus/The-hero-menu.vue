<template>
  <nav id="container-main-menu" class="d-none d-md-block" :aria-label="t('siteIdentity.menu.ariaLabel')">
    <!-- Desktop menu -->
    <div class="d-flex justify-end align-center ga-4">
      <div
        v-if="showMenuSearch"
        ref="menuSearchRef"
        class="main-menu-search"
        :class="{ 'main-menu-search--open': isSearchOpen }"
      >
        <v-btn
          v-if="!isSearchOpen"
          class="main-menu-search__activator"
          icon="mdi-magnify"
          variant="text"
          :aria-label="t('siteIdentity.menu.search.openLabel')"
          @click="openSearch"
        />

        <v-expand-x-transition>
          <div v-if="isSearchOpen" class="main-menu-search__field-wrapper">
            <SearchSuggestField
              v-model="searchQuery"
              class="main-menu-search__field"
              :label="t('siteIdentity.menu.search.label')"
              :placeholder="t('siteIdentity.menu.search.placeholder')"
              :aria-label="t('siteIdentity.menu.search.ariaLabel')"
              :min-chars="MIN_SEARCH_QUERY_LENGTH"
              @submit="handleSearchSubmit"
              @select-category="handleCategorySuggestion"
              @select-product="handleProductSuggestion"
              @update:model-value="updateSearchQuery"
              @clear="handleSearchClear"
            >
              <template #append-inner>
                <div class="main-menu-search__actions">
                  <v-btn
                    icon="mdi-arrow-right"
                    variant="flat"
                    color="primary"
                    size="small"
                    :aria-label="t('siteIdentity.menu.search.submitLabel')"
                    @click.prevent="handleSearchSubmit"
                  />
                  <v-btn
                    icon="mdi-close"
                    variant="text"
                    color="text-neutral-secondary"
                    size="small"
                    :aria-label="t('siteIdentity.menu.search.closeLabel')"
                    @click.prevent="closeSearch"
                  />
                </div>
              </template>
            </SearchSuggestField>
          </div>
        </v-expand-x-transition>
      </div>

      <v-list class="d-flex justify-end font-weight-bold" role="menubar">
        <template v-for="item in menuItems" :key="item.id">
          <v-list-item
            v-if="item.type === 'link'"
            class="main-menu-items"
            :class="{ active: isMenuItemActive(item) }"
            role="menuitem"
            @click="navigateToPage(item.path)"
          >
            <v-list-item-title>{{ item.label }}</v-list-item-title>
          </v-list-item>

          <div
            v-else-if="item.type === 'products'"
            class="main-menu-item main-menu-item--products"
          >
            <v-menu
              open-on-hover
              location="bottom"
              transition="fade-transition"
              :offset="[0, 12]"
            >
              <template #activator="{ props, isActive }">
                <v-list-item
                  v-bind="props"
                  class="main-menu-items main-menu-items--products"
                  :class="{ active: isMenuItemActive(item) || isActive }"
                  role="menuitem"
                >
                  <v-list-item-title>
                    <span class="main-menu-items__label">
                      <span>{{ item.label }}</span>
                      <v-icon
                        class="main-menu-items__toggle-icon"
                        :icon="isActive ? 'mdi-menu-up' : 'mdi-menu-down'"
                        aria-hidden="true"
                      />
                    </span>
                  </v-list-item-title>
                </v-list-item>
              </template>

              <v-card
                class="products-menu"
                color="surface-default"
                elevation="8"
              >
                <div class="products-menu__header">
                  <v-avatar
                    class="products-menu__avatar"
                    size="44"
                    color="surface-primary-080"
                  >
                    <v-icon icon="mdi-storefront-outline" color="primary" />
                  </v-avatar>
                  <div>
                    <p class="products-menu__title">
                      {{ item.copy.headerTitle }}
                    </p>
                    <p class="products-menu__subtitle">
                      {{ item.copy.headerSubtitle }}
                    </p>
                  </div>
                </div>

                <v-divider class="products-menu__divider" />

                <v-row class="products-menu__sections" align="stretch">
                  <v-col cols="12" sm="5" class="products-menu__section">
                    <p class="products-menu__section-title">
                      {{ item.copy.sections.popularTitle }}
                    </p>
                    <p class="products-menu__section-description">
                      {{ item.copy.sections.popularDescription }}
                    </p>

                    <div
                      v-if="item.loading"
                      class="products-menu__skeleton-group"
                    >
                      <v-skeleton-loader
                        v-for="skeletonIndex in 2"
                        :key="`products-menu-popular-skeleton-${skeletonIndex}`"
                        type="list-item-two-line"
                        class="products-menu__skeleton"
                      />
                    </div>

                    <div
                      v-else-if="item.popularCategories.length"
                      class="products-menu__popular-list"
                    >
                      <NuxtLink
                        v-for="category in item.popularCategories"
                        :key="category.id"
                        :to="category.href"
                        class="products-menu__popular-card"
                        role="menuitem"
                      >
                        <v-avatar
                          class="products-menu__popular-avatar"
                          size="36"
                        >
                          <v-img
                            v-if="category.image"
                            :src="category.image"
                            :alt="category.title"
                            cover
                          />
                          <v-icon v-else icon="mdi-image-outline" size="28" />
                        </v-avatar>
                        <p class="products-menu__popular-title">
                          {{ category.title }}
                        </p>
                        <v-icon
                          icon="mdi-arrow-top-right"
                          size="18"
                          class="products-menu__popular-icon"
                        />
                      </NuxtLink>
                    </div>

                    <p v-else class="products-menu__empty">
                      {{ item.copy.sections.popularEmpty }}
                    </p>

                    <div class="products-menu__nudge">
                      <div class="products-menu__nudge-copy">
                        <p class="products-menu__nudge-title">
                          {{ item.copy.nudgeCtaTitle }}
                        </p>
                        <p class="products-menu__nudge-description">
                          {{ item.copy.nudgeCtaDescription }}
                        </p>
                      </div>
                      <v-btn
                        color="primary"
                        variant="flat"
                        prepend-icon="mdi-robot-love"
                        size="small"
                        class="products-menu__nudge-button"
                        @click.stop="isNudgeWizardOpen = true"
                      >
                        {{ item.copy.nudgeCtaButton }}
                      </v-btn>
                    </div>
                  </v-col>

                  <v-col cols="12" sm="5" class="products-menu__section">
                    <p class="products-menu__section-title">
                      {{ item.copy.sections.taxonomyTitle }}
                    </p>
                    <p class="products-menu__section-description">
                      {{ item.copy.sections.taxonomyDescription }}
                    </p>

                    <div
                      v-if="item.taxonomyGroups.length"
                      class="products-menu__taxonomy"
                    >
                      <div
                        v-for="group in item.taxonomyGroups"
                        :key="group.id"
                        class="products-menu__taxonomy-group"
                      >
                        <p class="products-menu__taxonomy-label">
                          {{ group.title }}
                        </p>
                        <v-list
                          class="products-menu__taxonomy-list"
                          bg-color="transparent"
                          density="compact"
                          nav
                        >
                          <v-list-item
                            v-for="entry in group.entries"
                            :key="entry.id"
                            :to="entry.href"
                            class="products-menu__taxonomy-link"
                            role="menuitem"
                          >
                            <v-list-item-title>{{
                              entry.title
                            }}</v-list-item-title>
                            <template #append>
                              <v-icon icon="mdi-arrow-right" size="16" />
                            </template>
                          </v-list-item>
                        </v-list>
                        <NuxtLink
                          v-if="group.showMoreHref"
                          :to="group.showMoreHref"
                          class="products-menu__show-more"
                        >
                          {{ item.copy.sections.taxonomyShowMore }}
                        </NuxtLink>
                      </div>
                    </div>

                    <p v-else class="products-menu__empty">
                      {{ item.copy.sections.taxonomyEmpty }}
                    </p>
                  </v-col>
                </v-row>

                <v-divider
                  class="products-menu__divider products-menu__divider--cta"
                />

                <div class="products-menu__cta-wrapper">
                  <NuxtLink
                    :to="item.ctaHref"
                    class="products-menu__cta-action"
                  >
                    {{ item.copy.ctaLabel }}
                    <v-icon icon="mdi-arrow-right" size="18" />
                  </NuxtLink>
                </div>
              </v-card>
            </v-menu>
          </div>

          <div v-else class="main-menu-item main-menu-item--community">
            <v-menu
              open-on-hover
              location="bottom"
              transition="fade-transition"
              :offset="[0, 12]"
            >
              <template #activator="{ props, isActive }">
                <v-list-item
                  v-bind="props"
                  class="main-menu-items main-menu-items--community"
                  :class="{ active: isMenuItemActive(item) || isActive }"
                  role="menuitem"
                >
                  <v-list-item-title>
                    <span class="main-menu-items__label">
                      <span>{{ item.label }}</span>
                      <v-icon
                        class="main-menu-items__toggle-icon"
                        :icon="isActive ? 'mdi-menu-up' : 'mdi-menu-down'"
                        aria-hidden="true"
                      />
                    </span>
                  </v-list-item-title>
                </v-list-item>
              </template>

              <v-card
                class="community-menu"
                color="surface-default"
                elevation="8"
              >
                <div class="community-menu__header">
                  <v-avatar
                    class="community-menu__avatar"
                    size="44"
                    color="surface-primary-080"
                  >
                    <v-icon icon="mdi-account-group" color="primary" />
                  </v-avatar>
                  <div>
                    <p class="community-menu__title">{{ item.label }}</p>
                    <p class="community-menu__subtitle">
                      {{ t('siteIdentity.menu.community.tagline') }}
                    </p>
                  </div>
                </div>

                <v-divider class="community-menu__divider" />

                <v-row
                  class="community-menu__sections"
                  justify="space-between"
                  align="stretch"
                >
                  <v-col
                    v-for="section in item.sections"
                    :key="section.id"
                    cols="12"
                    sm="5"
                    class="community-menu__section"
                  >
                    <p class="community-menu__section-title">
                      {{ section.title }}
                    </p>
                    <p class="community-menu__section-description">
                      {{ section.description }}
                    </p>

                    <v-list
                      class="community-menu__section-list"
                      bg-color="transparent"
                      density="comfortable"
                      nav
                      lines="one"
                    >
                      <v-list-item
                        v-for="link in section.links"
                        :key="link.id"
                        class="community-menu__link"
                        :class="{
                          'community-menu__link--external': link.external,
                        }"
                        :to="link.to"
                        :href="link.href"
                        :target="link.external ? '_blank' : undefined"
                        :rel="link.external ? 'noopener noreferrer' : undefined"
                        role="menuitem"
                      >
                        <template #prepend>
                          <v-avatar size="34" class="community-menu__link-icon">
                            <v-icon
                              :icon="link.icon"
                              size="20"
                              color="accent-primary-highlight"
                            />
                          </v-avatar>
                        </template>

                        <v-list-item-title class="community-menu__link-label">
                          {{ link.label }}
                        </v-list-item-title>
                        <v-list-item-subtitle
                          v-if="link.description"
                          class="community-menu__link-description"
                        >
                          {{ link.description }}
                        </v-list-item-subtitle>

                        <template v-if="link.external" #append>
                          <v-icon
                            icon="mdi-open-in-new"
                            size="18"
                            class="community-menu__external-icon"
                          />
                        </template>
                      </v-list-item>
                    </v-list>
                  </v-col>
                </v-row>
              </v-card>
            </v-menu>
          </div>
        </template>
      </v-list>
      <ThemeToggle test-id="hero-theme-toggle" />
      <ZoomToggle />
      <v-menu
        v-if="isLoggedIn"
        v-model="isAccountMenuOpen"
        location="bottom"
        transition="fade-transition"
        min-width="260"
        offset="8"
      >
        <template #activator="{ props }">
          <v-btn
            v-bind="props"
            color="surface-primary-080"
            class="font-weight-bold account-menu-activator"
            rounded="pill"
            variant="flat"
            data-testid="hero-account-menu-activator"
          >
            <v-icon icon="mdi-account-circle" start />
            <span class="account-username text-truncate">{{
              displayName
            }}</span>
            <v-icon icon="mdi-menu-down" end />
          </v-btn>
        </template>

        <v-card class="account-menu" color="surface-default" elevation="4">
          <v-list density="comfortable">
            <v-list-item>
              <v-list-item-title class="font-weight-medium text-truncate">
                {{ displayName }}
              </v-list-item-title>
              <v-list-item-subtitle v-if="hasRoles" class="mt-2">
                <div class="d-flex flex-wrap ga-2">
                  <v-chip
                    v-for="role in accountRoles"
                    :key="role"
                    color="surface-primary-100"
                    size="small"
                    variant="flat"
                    class="role-chip"
                  >
                    {{ role }}
                  </v-chip>
                </div>
              </v-list-item-subtitle>
              <v-list-item-subtitle v-else class="text-neutral-soft">
                No assigned roles
              </v-list-item-subtitle>
            </v-list-item>

            <v-divider class="my-2" />

            <v-list-item
              density="comfortable"
              data-testid="hero-clear-cache"
              :disabled="isClearingCache"
              @click="handleClearCache"
            >
              <template #prepend>
                <v-icon icon="mdi-refresh" />
              </template>
              <v-list-item-title>
                {{ isClearingCache ? 'Clearing cache…' : 'Clear cache' }}
              </v-list-item-title>
            </v-list-item>

            <v-list-item
              density="comfortable"
              data-testid="hero-account-logout"
              @click="handleLogout"
            >
              <template #prepend>
                <v-icon icon="mdi-logout" />
              </template>
              <v-list-item-title>Logout</v-list-item-title>
            </v-list-item>
          </v-list>
        </v-card>
      </v-menu>
    </div>
  </nav>

  <v-dialog
    v-model="isNudgeWizardOpen"
    max-width="980"
    scrollable
    transition="dialog-bottom-transition"
  >
    <NudgeToolWizard @navigate="isNudgeWizardOpen = false" />
  </v-dialog>

  <!-- Mobile menu command -->
  <div class="d-flex justify-end d-md-none">
    <v-btn icon aria-label="Ouvrir le menu" @click="$emit('toggle-drawer')">
      <v-icon>mdi-menu</v-icon>
    </v-btn>
  </div>
</template>

<script setup lang="ts">
import { defineAsyncComponent } from 'vue'
import ThemeToggle from './ThemeToggle.vue'
import ZoomToggle from './ZoomToggle.vue'
import { useI18n } from 'vue-i18n'
import { useCommunityMenu } from './useCommunityMenu'
import type { CommunitySection } from './useCommunityMenu'
import {
  normalizeLocale,
  resolveLocalizedRoutePath,
} from '~~/shared/utils/localized-routes'
import { useCategoryNavigation } from '~/composables/categories/useCategoryNavigation'
import type {
  CategoryNavigationDto,
  CategoryNavigationDtoChildCategoriesInner,
} from '~~/shared/api-client'
import {
  MIN_SEARCH_QUERY_LENGTH,
  useMenuSearchControls,
} from '~/composables/menus/useMenuSearchControls'

const SearchSuggestField = defineAsyncComponent({
  loader: () => import('~/components/search/SearchSuggestField.vue'),
  suspensible: false,
})

const NudgeToolWizard = defineAsyncComponent({
  loader: () => import('~/components/nudge-tool/NudgeToolWizard.vue'),
  suspensible: false,
})

const route = useRoute()
const router = useRouter()
const nuxtApp = useNuxtApp()
const { isLoggedIn, logout, username, roles } = useAuth()
const { t, locale } = useI18n()
const currentLocale = computed(() => normalizeLocale(locale.value))
const {
  navigation: categoryNavigation,
  fetchNavigation,
  loading: navigationLoading,
} = useCategoryNavigation()

if (import.meta.server) {
  await fetchNavigation()
} else if (!categoryNavigation.value) {
  await fetchNavigation()
}

const MAX_PRODUCTS_MENU_POPULAR = 4
const MAX_TAXONOMY_GROUP_ITEMS = 3

const menuSearchRef = ref<HTMLElement | null>(null)
const categoriesRoutePath = computed(() =>
  resolveLocalizedRoutePath('categories', currentLocale.value)
)

const {
  searchQuery,
  isSearchOpen,
  showMenuSearch,
  openSearch: activateSearch,
  closeSearch,
  handleSearchClear,
  handleSearchSubmit,
  handleCategorySuggestion,
  handleProductSuggestion,
} = useMenuSearchControls()

const focusSearchInput = () => {
  const root = menuSearchRef.value

  if (!root) {
    return
  }

  const input = root.querySelector('input')

  if (input instanceof HTMLInputElement) {
    input.focus()
    input.select()
  }
}

const openSearch = async () => {
  if (isSearchOpen.value) {
    focusSearchInput()
    return
  }

  activateSearch()
  await nextTick()
  focusSearchInput()
}

const updateSearchQuery = (value: string) => {
  searchQuery.value = value
}

const normalizeVerticalHomeUrl = (
  raw: string | null | undefined
): string | null => {
  if (!raw) {
    return null
  }

  const trimmed = raw.trim()

  if (!trimmed) {
    return null
  }

  if (/^https?:\/\//iu.test(trimmed)) {
    return trimmed
  }

  return trimmed.startsWith('/') ? trimmed : `/${trimmed}`
}

type FetchLike = (
  input: string,
  init?: Record<string, unknown>
) => Promise<unknown>

const resolveFetch = (): FetchLike | undefined => {
  if (typeof nuxtApp.$fetch === 'function') {
    return nuxtApp.$fetch as FetchLike
  }

  const globalFetch = (globalThis as { $fetch?: unknown }).$fetch

  if (typeof globalFetch === 'function') {
    return globalFetch as FetchLike
  }

  return undefined
}

const isNudgeWizardOpen = ref(false)
const isAccountMenuOpen = ref(false)
const isClearingCache = ref(false)

const displayName = computed(() => {
  const label = username.value?.trim()
  return label && label.length > 0 ? label : 'Account'
})

const accountRoles = computed(() =>
  roles.value.map(role => role.trim()).filter(role => role.length > 0)
)
const hasRoles = computed(() => accountRoles.value.length > 0)

const handleLogout = async () => {
  if (!isLoggedIn.value) {
    return
  }

  try {
    await logout()
    await router.replace(route.fullPath || '/')
    isAccountMenuOpen.value = false
  } catch (error) {
    console.error('Logout failed', error)
  }
}

const isSuccessfulCacheResetResponse = (
  payload: unknown
): payload is { success: true } =>
  Boolean(
    payload &&
    typeof payload === 'object' &&
    'success' in payload &&
    (payload as { success?: boolean }).success === true
  )

const handleClearCache = async () => {
  if (isClearingCache.value) {
    return
  }

  isClearingCache.value = true

  try {
    const fetcher = resolveFetch()

    if (!fetcher) {
      console.error(
        'Failed to clear caches',
        new Error('No fetch helper available')
      )
      return
    }

    const response = await fetcher('/api/admin/cache/reset', { method: 'POST' })

    if (!isSuccessfulCacheResetResponse(response)) {
      console.error(
        'Failed to clear caches',
        new Error('Unexpected response payload')
      )
      return
    }

    isAccountMenuOpen.value = false

    if (typeof window !== 'undefined') {
      window.location.reload()
    }
  } catch (error) {
    console.error('Failed to clear caches', error)
  } finally {
    isClearingCache.value = false
  }
}

defineEmits<{
  'toggle-drawer': []
}>()

interface ProductsMenuTexts {
  headerTitle: string
  headerSubtitle: string
  fallbackCategoryTitle: string
  sections: {
    popularTitle: string
    popularDescription: string
    popularEmpty: string
    taxonomyTitle: string
    taxonomyDescription: string
    taxonomyEmpty: string
    taxonomyShowMore: string
  }
  ctaLabel: string
  nudgeCtaTitle: string
  nudgeCtaDescription: string
  nudgeCtaButton: string
}

const productsMenuTexts = computed(() => ({
  headerTitle: String(t('siteIdentity.menu.productsMenu.title')),
  headerSubtitle: String(t('siteIdentity.menu.productsMenu.subtitle')),
  fallbackCategoryTitle: String(
    t('siteIdentity.menu.productsMenu.untitledCategory')
  ),
  sections: {
    popularTitle: String(t('siteIdentity.menu.productsMenu.popular.title')),
    popularDescription: String(
      t('siteIdentity.menu.productsMenu.popular.description')
    ),
    popularEmpty: String(t('siteIdentity.menu.productsMenu.popular.empty')),
    taxonomyTitle: String(t('siteIdentity.menu.productsMenu.taxonomy.title')),
    taxonomyDescription: String(
      t('siteIdentity.menu.productsMenu.taxonomy.description')
    ),
    taxonomyEmpty: String(t('siteIdentity.menu.productsMenu.taxonomy.empty')),
    taxonomyShowMore: String(
      t('siteIdentity.menu.productsMenu.taxonomy.showMore')
    ),
  },
  ctaLabel: String(t('siteIdentity.menu.productsMenu.cta.label')),
  nudgeCtaTitle: String(t('siteIdentity.menu.productsMenu.nudge.title')),
  nudgeCtaDescription: String(
    t('siteIdentity.menu.productsMenu.nudge.description')
  ),
  nudgeCtaButton: String(t('siteIdentity.menu.productsMenu.nudge.button')),
}))

interface ProductsMenuPopularCategory {
  id: string
  title: string
  href: string
  image: string | null
}

interface ProductsMenuTaxonomyEntry {
  id: string
  title: string
  href: string
}

interface ProductsMenuTaxonomyGroup {
  id: string
  title: string
  entries: ProductsMenuTaxonomyEntry[]
  showMoreHref: string | null
}

const normalizeNavigationCategoryTitle = (
  category: CategoryNavigationDtoChildCategoriesInner | undefined,
  fallback: string
) =>
  category?.vertical?.verticalHomeTitle?.trim() ||
  category?.title?.trim() ||
  fallback

const flattenNavigationChildren = (
  categories: CategoryNavigationDtoChildCategoriesInner[] | undefined
): CategoryNavigationDtoChildCategoriesInner[] => {
  if (!categories?.length) {
    return []
  }

  return categories.flatMap(category => {
    const children = Array.isArray(category.children)
      ? (category.children as CategoryNavigationDtoChildCategoriesInner[])
      : []

    return [category, ...flattenNavigationChildren(children)]
  })
}

const navigationSource = computed<CategoryNavigationDto | null>(
  () => categoryNavigation.value ?? null
)

const productsMenuPopularCategories = computed<ProductsMenuPopularCategory[]>(
  () => {
    const fallbackTitle = productsMenuTexts.value.fallbackCategoryTitle
    const popularCategories = navigationSource.value?.popularCategories ?? []

    return popularCategories
      .map((category, index) => {
        const href = normalizeVerticalHomeUrl(
          category.vertical?.verticalHomeUrl
        )
        if (!href) {
          return null
        }

        const image =
          category.vertical?.imageSmall ??
          category.vertical?.imageMedium ??
          category.vertical?.imageLarge ??
          null

        return {
          id:
            category.vertical?.id ??
            category.slug ??
            `popular-category-${index}`,
          title: normalizeNavigationCategoryTitle(category, fallbackTitle),
          href,
          image,
          order: category.vertical?.order ?? Number.MAX_SAFE_INTEGER,
        }
      })
      .filter(
        (
          category
        ): category is ProductsMenuPopularCategory & { order: number } =>
          category !== null
      )
      .sort((a, b) => a.order - b.order || a.title.localeCompare(b.title))
      .slice(0, MAX_PRODUCTS_MENU_POPULAR)
      .map(({ order: _order, ...category }) => category)
  }
)

const descendantVerticals = computed(() => {
  const fromApi = navigationSource.value?.descendantVerticals ?? []
  if (fromApi.length) {
    return fromApi
  }

  return flattenNavigationChildren(navigationSource.value?.childCategories)
})

const productsMenuTaxonomyGroups = computed<ProductsMenuTaxonomyGroup[]>(() => {
  const parents = navigationSource.value?.childCategories ?? []
  const fallbackTitle = productsMenuTexts.value.fallbackCategoryTitle

  return parents
    .map((parent, parentIndex) => {
      const parentPath = parent.path ?? parent.slug ?? ''
      if (!parentPath) {
        return null
      }

      const candidateVerticals = descendantVerticals.value.filter(category => {
        const path = category.path ?? category.slug ?? ''
        if (!path) {
          return false
        }

        if (path === parentPath) {
          return false
        }

        return path.startsWith(`${parentPath}/`)
      })

      const entries = candidateVerticals
        .map((category, entryIndex) => {
          const href = normalizeVerticalHomeUrl(
            category.vertical?.verticalHomeUrl
          )
          if (!href) {
            return null
          }

          return {
            id:
              category.slug ??
              category.path ??
              `taxonomy-entry-${parentIndex}-${entryIndex}`,
            title: normalizeNavigationCategoryTitle(category, fallbackTitle),
            href,
            order: category.vertical?.order ?? Number.MAX_SAFE_INTEGER,
          }
        })
        .filter(
          (entry): entry is ProductsMenuTaxonomyEntry & { order: number } =>
            entry !== null
        )
        .sort((a, b) => a.order - b.order || a.title.localeCompare(b.title))
        .slice(0, MAX_TAXONOMY_GROUP_ITEMS)
        .map(({ order: _order, ...entry }) => entry)

      if (!entries.length) {
        return null
      }

      return {
        id: parent.slug ?? parent.path ?? `taxonomy-group-${parentIndex}`,
        title: parent.title ?? fallbackTitle,
        entries,
        showMoreHref: parent.path
          ? `/categories/${parent.path}`
          : categoriesRoutePath.value,
      }
    })
    .filter((group): group is ProductsMenuTaxonomyGroup => group !== null)
})

const productsMenuActivePaths = computed(() => {
  const normalizedPaths = new Set<string>([categoriesRoutePath.value])

  productsMenuPopularCategories.value.forEach(category => {
    if (category.href.startsWith('/')) {
      normalizedPaths.add(category.href)
    }
  })

  productsMenuTaxonomyGroups.value.forEach(group => {
    group.entries.forEach(entry => {
      if (entry.href.startsWith('/')) {
        normalizedPaths.add(entry.href)
      }
    })
  })

  return Array.from(normalizedPaths)
})

const isProductsMenuLoading = computed(
  () =>
    navigationLoading.value && productsMenuPopularCategories.value.length === 0
)

interface LinkMenuItemDefinition {
  id: string
  labelKey: string
  routeName: string
  type: 'link'
}

interface ProductsMenuItemDefinition {
  id: string
  labelKey: string
  routeName: string
  type: 'products'
}

interface CommunityMenuItemDefinition {
  id: string
  labelKey: string
  type: 'community'
}

type MenuItemDefinition =
  | LinkMenuItemDefinition
  | CommunityMenuItemDefinition
  | ProductsMenuItemDefinition

interface LinkMenuItem {
  id: string
  type: 'link'
  label: string
  path: string
}

interface ProductsMenuItem {
  id: string
  type: 'products'
  label: string
  popularCategories: ProductsMenuPopularCategory[]
  taxonomyGroups: ProductsMenuTaxonomyGroup[]
  copy: ProductsMenuTexts
  ctaHref: string
  loading: boolean
  activePaths: string[]
}

interface CommunityMenuItem {
  id: string
  type: 'community'
  label: string
  sections: CommunitySection[]
  activePaths: string[]
}

type MenuItem = LinkMenuItem | CommunityMenuItem | ProductsMenuItem
const { sections: communitySections, activePaths: communityActivePaths } =
  useCommunityMenu(t, currentLocale)

const baseMenuItems: MenuItemDefinition[] = [
  {
    id: 'impact-score',
    type: 'link',
    labelKey: 'siteIdentity.menu.items.impactScore',
    routeName: 'impact-score',
  },
  {
    id: 'products',
    type: 'products',
    labelKey: 'siteIdentity.menu.items.products',
    routeName: 'categories',
  },
  {
    id: 'blog',
    type: 'link',
    labelKey: 'siteIdentity.menu.items.blog',
    routeName: 'blog',
  },
  {
    id: 'community',
    type: 'community',
    labelKey: 'siteIdentity.menu.items.contact',
  },
]

const menuItems = computed<MenuItem[]>(() =>
  baseMenuItems.map(item => {
    const label = String(t(item.labelKey))

    if (item.type === 'link') {
      return {
        id: item.id,
        type: 'link' as const,
        label,
        path: resolveLocalizedRoutePath(item.routeName, currentLocale.value),
      }
    }

    if (item.type === 'products') {
      const copy = productsMenuTexts.value
      return {
        id: item.id,
        type: 'products' as const,
        label,
        popularCategories: productsMenuPopularCategories.value,
        taxonomyGroups: productsMenuTaxonomyGroups.value,
        copy,
        ctaHref: categoriesRoutePath.value,
        loading: isProductsMenuLoading.value,
        activePaths: productsMenuActivePaths.value,
      }
    }

    return {
      id: item.id,
      type: 'community' as const,
      label,
      sections: communitySections.value,
      activePaths: communityActivePaths.value,
    }
  })
)

const isActiveRoute = (path: string): boolean => {
  if (!path) {
    return false
  }

  if (path === '/') {
    return route.path === path
  }

  return route.path.startsWith(path)
}

const navigateToPage = (path: string): void => {
  router.push(path)
}

const isMenuItemActive = (item: MenuItem): boolean => {
  if (item.type === 'link') {
    return isActiveRoute(item.path)
  }

  return item.activePaths.some(path => isActiveRoute(path))
}
</script>

<style scoped lang="sass">
.main-menu-search
  display: flex
  align-items: center
  gap: 0.5rem

  &__activator
    color: rgb(var(--v-theme-text-neutral-strong))
    transition: color 0.3s ease, transform 0.3s ease
    &:hover
      color: rgb(var(--v-theme-accent-primary-highlight))
      transform: scale(1.05)

  &__field-wrapper
    width: clamp(220px, 28vw, 320px)
    max-width: 320px

  &__actions
    display: flex
    align-items: center
    gap: 0.25rem

.main-menu-search__field
  :deep(.v-field)
    border-radius: 999px
    background-color: rgba(var(--v-theme-surface-primary-080), 0.95)
    box-shadow: 0 12px 30px rgba(var(--v-theme-shadow-primary-600), 0.12)

  :deep(.v-field__outline)
    display: none

  :deep(.v-field__input)
    padding-inline-start: 1rem
    min-height: 44px

  :deep(.v-field__append-inner)
    padding-inline-end: 0.25rem

  :deep(.v-field__clearable)
    margin-inline-end: 0.25rem

.main-menu-search--open .main-menu-search__activator
  opacity: 0
  pointer-events: none

.main-menu-items
  color: rgb(var(--v-theme-text-neutral-strong))
  font-size: 1rem
  cursor: pointer
  font-weight: bolder
  transition: color 0.3s ease
  &:hover
    color: rgb(var(--v-theme-accent-supporting))
  &.active
    color: rgb(var(--v-theme-accent-supporting))
    font-weight: 900

.main-menu-items--community,
.main-menu-items--products
  min-height: 48px
  padding-inline-start: 0.75rem
  padding-inline-end: 0.625rem
  gap: 0.25rem

  :deep(.v-list-item__append)
    margin-inline-start: 0.25rem
    padding-inline: 0
    min-width: auto
    display: flex
    align-items: center

.main-menu-items__label
  display: inline-flex
  align-items: center
  gap: 0.5rem

.main-menu-items__toggle-icon
  color: currentColor
  opacity: 0.9
  transition: transform 0.2s ease

  .main-menu-items.active &
    opacity: 1

  .main-menu-item:hover &
    transform: translateY(-1px)

.community-menu
  width: min(720px, 80vw)
  padding: 1.5rem
  border-radius: 1.25rem
  background: linear-gradient(135deg, rgba(var(--v-theme-surface-primary-080), 0.95), rgba(var(--v-theme-surface-glass), 0.95))
  box-shadow: 0 24px 48px rgba(var(--v-theme-shadow-primary-600), 0.16)

  &__header
    display: flex
    align-items: center
    gap: 1rem
    margin-block-end: 0.75rem

  &__avatar
    background-color: rgba(var(--v-theme-surface-primary-120), 0.85)
    box-shadow: 0 10px 30px rgba(var(--v-theme-shadow-primary-600), 0.2)

  &__title
    margin: 0
    font-size: 1.1rem
    font-weight: 700
    color: rgb(var(--v-theme-text-neutral-strong))

  &__subtitle
    margin: 0
    font-size: 0.9rem
    color: rgb(var(--v-theme-text-neutral-secondary))

  &__divider
    margin-block: 0.5rem 1rem
    opacity: 0.4

  &__sections
    gap: 1rem
    margin: 0

  &__section
    display: flex
    flex-direction: column
    gap: 0.75rem

  &__section-title
    margin: 0
    font-weight: 600
    color: rgb(var(--v-theme-text-neutral-strong))

  &__section-description
    margin: 0
    font-size: 0.85rem
    color: rgb(var(--v-theme-text-neutral-secondary))
    min-height: 2.5rem

  &__section-list
    padding: 0
    background: transparent

  &__link
    border-radius: 0.75rem
    transition: background-color 0.2s ease, transform 0.2s ease
    margin-block: 0.125rem

    &:hover
      background-color: rgba(var(--v-theme-surface-primary-080), 0.8)
      transform: translateY(-1px)

  &__link-icon
    background-color: rgba(var(--v-theme-surface-primary-120), 0.9)

  &__link-label
    font-weight: 600
    color: rgb(var(--v-theme-text-neutral-strong))

  &__link-description
    color: rgb(var(--v-theme-text-neutral-secondary))
    white-space: normal

  &__external-icon
    color: rgb(var(--v-theme-accent-primary-highlight))

.products-menu
  width: min(860px, 85vw)
  padding: 1.5rem
  border-radius: 1.25rem
  background: linear-gradient(135deg, rgba(var(--v-theme-surface-primary-080), 0.95), rgba(var(--v-theme-surface-glass), 0.95))
  box-shadow: 0 24px 48px rgba(var(--v-theme-shadow-primary-600), 0.16)

  &__header
    display: flex
    align-items: center
    gap: 1rem
    margin-block-end: 1rem

  &__avatar
    background-color: rgba(var(--v-theme-surface-primary-120), 0.85)
    box-shadow: 0 10px 30px rgba(var(--v-theme-shadow-primary-600), 0.2)

  &__title
    margin: 0
    font-size: 1.1rem
    font-weight: 700
    color: rgb(var(--v-theme-text-neutral-strong))

  &__subtitle
    margin: 0.35rem 0 0
    color: rgb(var(--v-theme-text-neutral-secondary))

  &__divider
    opacity: 0.5

  &__divider--cta
    margin-top: 1rem

  &__sections
    margin: 0
    gap: 1.25rem

  &__section
    display: flex
    flex-direction: column
    gap: 0.75rem

  &__section-title
    margin: 0
    font-weight: 600
    color: rgb(var(--v-theme-text-neutral-strong))

  &__section-description
    margin: 0
    font-size: 0.9rem
    color: rgb(var(--v-theme-text-neutral-secondary))

  &__popular-list
    display: flex
    flex-direction: column
    gap: 0.75rem

  &__popular-card
    display: flex
    align-items: center
    gap: 0.5rem
    padding: 0.75rem 0.9rem
    border-radius: 1rem
    text-decoration: none
    background: rgba(var(--v-theme-surface-primary-080), 0.85)
    border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.2)
    color: rgb(var(--v-theme-text-neutral-strong))
    transition: transform 0.2s ease, box-shadow 0.2s ease, background-color 0.2s ease

    &:hover
      transform: translateY(-2px)
      background: rgba(var(--v-theme-surface-primary-100), 0.95)
      box-shadow: 0 12px 24px rgba(var(--v-theme-shadow-primary-600), 0.18)

  &__popular-avatar
    background: rgba(var(--v-theme-surface-glass), 0.9)
    width: clamp(28px, 4vw, 36px)
    height: clamp(28px, 4vw, 36px)
    min-width: clamp(28px, 4vw, 36px)
    min-height: clamp(28px, 4vw, 36px)

  &__popular-title
    margin: 0
    font-weight: 600
    color: rgb(var(--v-theme-text-neutral-strong))
    flex: 1
    min-width: 0
    font-size: 0.95rem
    line-height: 1.3
    text-overflow: ellipsis
    overflow: hidden
    white-space: nowrap

  &__popular-icon
    color: rgb(var(--v-theme-accent-primary-highlight))

  &__taxonomy
    display: flex
    flex-direction: column
    gap: 1rem

  &__taxonomy-group
    padding: 0.75rem
    border-radius: 1rem
    background: rgba(var(--v-theme-surface-primary-080), 0.5)
    border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.2)

  &__taxonomy-label
    margin: 0 0 0.35rem
    font-weight: 600
    color: rgb(var(--v-theme-text-neutral-strong))

  &__taxonomy-list
    padding: 0

  &__taxonomy-link
    border-radius: 0.75rem
    transition: background-color 0.2s ease

    &:hover
      background-color: rgba(var(--v-theme-surface-primary-100), 0.6)

  &__show-more
    display: inline-flex
    align-items: center
    gap: 0.35rem
    font-weight: 600
    font-size: 0.85rem
    text-decoration: none
    color: rgb(var(--v-theme-accent-primary-highlight))
    margin-top: 0.5rem

  &__skeleton
    margin-block: 0.25rem

  &__empty
    margin: 0
    font-size: 0.9rem
    color: rgb(var(--v-theme-text-neutral-secondary))

  &__nudge
    margin-top: 0.75rem
    padding: 0.85rem
    border-radius: 1rem
    border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.18)
    background: rgba(var(--v-theme-surface-primary-080), 0.65)
    display: flex
    flex-direction: column
    gap: 0.5rem

  &__nudge-copy
    display: flex
    flex-direction: column
    gap: 0.25rem

  &__nudge-title
    margin: 0
    font-weight: 700
    color: rgb(var(--v-theme-text-neutral-strong))

  &__nudge-description
    margin: 0
    color: rgb(var(--v-theme-text-neutral-secondary))
    font-size: 0.95rem
    line-height: 1.45

  &__nudge-button
    align-self: flex-start
    box-shadow: 0 10px 24px rgba(var(--v-theme-shadow-primary-600), 0.16)
    text-transform: none

  &__cta-wrapper
    display: flex
    justify-content: flex-end
    margin-top: 1rem

  &__cta-action
    display: inline-flex
    align-items: center
    gap: 0.5rem
    border-radius: 999px
    padding: 0.6rem 1.5rem
    font-weight: 600
    text-decoration: none
    background-color: rgb(var(--v-theme-accent-primary-highlight))
    color: rgb(var(--v-theme-text-on-accent))
    transition: box-shadow 0.2s ease, transform 0.2s ease

    &:hover
      box-shadow: 0 12px 24px rgba(var(--v-theme-shadow-primary-600), 0.25)
      transform: translateY(-1px)

.account-menu-activator
  text-transform: none
  color: rgb(var(--v-theme-text-neutral-strong))
  background-color: rgb(var(--v-theme-surface-primary-080))

  &:hover
    background-color: rgb(var(--v-theme-surface-primary-100))

  .account-username
    max-width: 140px
    display: inline-block

.account-menu
  background-color: rgb(var(--v-theme-surface-default))
  color: rgb(var(--v-theme-text-neutral-strong))

.role-chip
  background-color: rgb(var(--v-theme-surface-primary-120))
  border: 1px solid rgb(var(--v-theme-border-primary-strong))
  color: rgb(var(--v-theme-text-neutral-strong))

.text-neutral-soft
  color: rgb(var(--v-theme-text-neutral-soft))

@media (max-width: 1263px)
  .community-menu
    width: min(640px, 90vw)

  .account-menu-activator
    .account-username
      max-width: 96px
</style>
