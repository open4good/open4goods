<template>
  <div class="pwa-landing">
    <v-container class="py-6" fluid>
      <v-row class="justify-center">
        <v-col cols="12" md="10" lg="8">
          <v-sheet
            class="pwa-landing__hero"
            color="surface-primary-080"
            rounded="xl"
            elevation="0"
            border
          >
            <div class="pwa-landing__hero-content">
              <h1 class="pwa-landing__title">
                {{ t('home.hero.title') }}
              </h1>
              <p class="pwa-landing__subtitle">
                {{ t('pwa.landing.hero.subtitle') }}
              </p>

              <v-card
                class="pwa-landing__search"
                variant="elevated"
                rounded="lg"
              >
                <v-card-title class="text-subtitle-1 font-weight-bold pb-1">
                  {{ t('pwa.landing.search.title') }}
                </v-card-title>
                <v-card-text class="pt-0">
                  <SearchSuggestField
                    :model-value="searchQuery"
                    :label="t('pwa.landing.search.label')"
                    :placeholder="t('pwa.landing.search.placeholder')"
                    :aria-label="t('pwa.landing.search.ariaLabel')"
                    :min-chars="minSuggestionQueryLength"
                    density="comfortable"
                    variant="outlined"
                    color="primary"
                    class="mb-3"
                    data-testid="pwa-search-input"
                    @update:model-value="$emit('update:searchQuery', $event)"
                    @submit="$emit('submit')"
                    @select-category="$emit('select-category', $event)"
                    @select-product="$emit('select-product', $event)"
                  />
                  <v-btn
                    block
                    color="primary"
                    variant="flat"
                    size="large"
                    class="font-weight-bold"
                    prepend-icon="mdi-arrow-right"
                    data-testid="pwa-search-submit"
                    @click="$emit('submit')"
                  >
                    {{ t('pwa.landing.search.cta') }}
                  </v-btn>
                </v-card-text>
              </v-card>
            </div>
          </v-sheet>
        </v-col>
      </v-row>

      <v-row class="justify-center mt-4" dense>
        <v-col
          v-for="action in quickActions"
          :key="action.key"
          cols="12"
          sm="6"
          lg="3"
        >
          <v-card
            class="pwa-landing__action-card"
            border
            rounded="xl"
            color="surface-default"
            @click="action.onClick()"
          >
            <v-card-item>
              <div class="d-flex align-center justify-space-between mb-2">
                <v-avatar size="40" color="surface-primary-120">
                  <v-icon
                    :icon="action.icon"
                    color="accent-primary-highlight"
                  />
                </v-avatar>
                <v-chip
                  v-if="action.badge"
                  size="x-small"
                  color="accent-supporting"
                  variant="flat"
                  class="font-weight-bold text-uppercase"
                >
                  {{ action.badge }}
                </v-chip>
              </div>
              <v-card-title class="text-subtitle-1 font-weight-bold px-0">
                {{ action.title }}
              </v-card-title>
              <v-card-subtitle class="text-body-2 text-neutral-secondary px-0">
                {{ action.description }}
              </v-card-subtitle>
            </v-card-item>
          </v-card>
        </v-col>
      </v-row>
    </v-container>

    <ClientOnly>
      <v-dialog
        v-model="isScannerOpen"
        max-width="520"
        transition="dialog-bottom-transition"
      >
        <v-card rounded="xl" class="pwa-landing__dialog-card">
          <v-card-title class="d-flex align-center justify-space-between">
            <span class="text-h6 font-weight-bold">
              {{ t('pwa.landing.actions.scan.title') }}
            </span>
            <v-btn
              icon="mdi-close"
              variant="text"
              @click="isScannerOpen = false"
            />
          </v-card-title>
          <v-card-text>
            <p class="text-body-2 text-neutral-secondary mb-3">
              {{ t('pwa.landing.actions.scan.helper') }}
            </p>
            <PwaBarcodeScanner
              :active="isScannerOpen"
              :loading-label="t('pwa.landing.actions.scan.loading')"
              :error-label="t('pwa.landing.actions.scan.error')"
              @decode="handleScanDecode"
              @error="handleScannerError"
            />
          </v-card-text>
        </v-card>
      </v-dialog>

      <v-dialog
        v-model="isWizardOpen"
        max-width="640"
        :fullscreen="display.smAndDown.value"
      >
        <v-card rounded="xl" class="pwa-landing__dialog-card">
          <v-card-title class="d-flex align-center justify-space-between">
            <span class="text-h6 font-weight-bold">
              {{ t('pwa.landing.actions.wizard.title') }}
            </span>
            <v-btn
              icon="mdi-close"
              variant="text"
              @click="isWizardOpen = false"
            />
          </v-card-title>
          <v-card-text class="pt-0">
            <NudgeToolWizard
              :verticals="verticals"
              @navigate="handleWizardNavigate"
              @close="isWizardOpen = false"
            />
          </v-card-text>
        </v-card>
      </v-dialog>
    </ClientOnly>

    <PwaMobileActionBar
      class="pwa-landing__action-bar"
      @scan="isScannerOpen = true"
      @wizard="isWizardOpen = true"
      @search="$emit('submit')"
    />

    <v-snackbar v-model="scannerError" color="error" timeout="3000">
      {{ scannerErrorMessage }}
    </v-snackbar>
  </div>
</template>

<script setup lang="ts">
import type { VerticalConfigDto } from '~~/shared/api-client'
import SearchSuggestField, {
  type CategorySuggestionItem,
  type ProductSuggestionItem,
} from '~/components/search/SearchSuggestField.vue'
import NudgeToolWizard from '~/components/nudge-tool/NudgeToolWizard.vue'
import PwaBarcodeScanner from './PwaBarcodeScanner.vue'
import PwaMobileActionBar from './PwaMobileActionBar.vue'
import { useDisplay } from 'vuetify'
import { computed, ref } from 'vue'

interface Props {
  verticals?: VerticalConfigDto[]
  searchQuery?: string
  minSuggestionQueryLength?: number
}

const props = withDefaults(defineProps<Props>(), {
  verticals: () => [],
  searchQuery: '',
  minSuggestionQueryLength: 2,
})

const emit = defineEmits<{
  'update:searchQuery': [value: string]
  submit: []
  'select-category': [payload: CategorySuggestionItem]
  'select-product': [payload: ProductSuggestionItem]
}>()

const { t } = useI18n()
const display = useDisplay()

const isScannerOpen = ref(false)
const isWizardOpen = ref(false)
const scannerError = ref(false)
const scannerErrorMessage = ref('')

const quickActions = computed(() => [
  {
    key: 'scan',
    icon: 'mdi-barcode-scan',
    title: t('pwa.landing.actions.scan.title'),
    description: t('pwa.landing.actions.scan.description'),
    onClick: () => (isScannerOpen.value = true),
  },
  {
    key: 'wizard',
    icon: 'mdi-sparkles',
    title: t('pwa.landing.actions.wizard.title'),
    description: t('pwa.landing.actions.wizard.description'),
    onClick: () => (isWizardOpen.value = true),
    badge: null,
  },
  {
    key: 'search',
    icon: 'mdi-magnify',
    title: t('pwa.landing.actions.search.title'),
    description: t('pwa.landing.actions.search.description'),
    onClick: () => emit('submit'),
    badge: null,
  },
])

const handleScanDecode = (value: string) => {
  isScannerOpen.value = false
  emit('update:searchQuery', value)
  emit('submit')
}

const handleScannerError = (message: string) => {
  scannerErrorMessage.value = message
  scannerError.value = true
}

const handleWizardNavigate = () => {
  isWizardOpen.value = false
}

const verticals = computed(() => props.verticals)
</script>

<style scoped lang="sass">
.pwa-landing
  padding-bottom: calc(env(safe-area-inset-bottom) + 88px)
  background: linear-gradient(135deg, rgba(var(--v-theme-surface-primary-100), 0.8), rgba(var(--v-theme-surface-default), 0.9))

.pwa-landing__hero
  padding: clamp(1.5rem, 4vw, 2rem)
  background: radial-gradient(circle at 20% 20%, rgba(var(--v-theme-hero-gradient-start), 0.08), transparent 35%), radial-gradient(circle at 80% 30%, rgba(var(--v-theme-hero-gradient-mid), 0.08), transparent 40%), rgba(var(--v-theme-surface-default), 0.85)
  backdrop-filter: blur(8px)

.pwa-landing__hero-content
  display: flex
  flex-direction: column
  gap: 0.85rem

.pwa-landing__title
  font-size: clamp(1.35rem, 5vw, 1.85rem)
  font-weight: 800
  margin: 0
  color: rgb(var(--v-theme-text-neutral-strong))

.pwa-landing__subtitle
  margin: 0
  color: rgb(var(--v-theme-text-neutral-secondary))
  font-size: 1rem

.pwa-landing__search
  background: rgb(var(--v-theme-surface-default))

.pwa-landing__action-card
  cursor: pointer
  transition: transform 0.2s ease, box-shadow 0.2s ease
  height: 100%
  background: rgba(var(--v-theme-surface-default), 0.92)

  &:hover
    transform: translateY(-4px)
    box-shadow: 0 16px 40px rgba(var(--v-theme-shadow-primary-600), 0.16)



.pwa-landing__dialog-card
  background: rgb(var(--v-theme-surface-default))

.pwa-landing__action-bar
  box-shadow: 0 -10px 32px rgba(var(--v-theme-shadow-primary-600), 0.12)

@media (min-width: 960px)
  .pwa-landing
    max-width: 1200px
    margin: 0 auto

  .pwa-landing__title
    font-size: 2rem
</style>
