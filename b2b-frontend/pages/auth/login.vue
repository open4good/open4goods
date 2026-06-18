<template>
  <div>
    <v-row no-gutters class="fill-height" style="min-height: calc(100vh - 56px);">
      <!-- Left visual panel — hidden on mobile -->
      <v-col
        cols="12"
        md="6"
        class="d-none d-md-flex flex-column justify-center align-center bg-surface-variant"
        aria-hidden="true"
      >
        <v-img
          :src="logoStackedSrc"
          :alt="t('app.name')"
          height="80"
          width="80"
          contain
          class="mb-8"
        />
        <div class="text-h3 font-weight-bold text-center px-8" style="max-width: 480px;">
          {{ t('auth.login.tagline') }}
        </div>
      </v-col>

      <!-- Right form panel -->
      <v-col
        cols="12"
        md="6"
        class="d-flex align-center justify-center pa-8 bg-surface"
      >
        <v-sheet max-width="420" width="100%" color="transparent">
          <div class="d-flex justify-start mb-8">
            <v-img
              :src="logoSrc"
              :alt="t('app.name')"
              height="28"
              width="100"
              contain
              position="left"
            />
          </div>

          <v-alert
            v-if="accessErrorMessage"
            type="warning"
            variant="tonal"
            rounded="lg"
            class="mb-6"
          >
            {{ accessErrorMessage }}
          </v-alert>

          <div class="mb-8">
            <h2 class="text-h5 font-weight-bold">{{ t('auth.login.title') }}</h2>
            <p class="text-body-2 text-medium-emphasis mt-1">{{ t('auth.login.subtitle') }}</p>
          </div>

          <div class="d-flex flex-column ga-3">
            <v-tooltip
              v-for="provider in primaryProviders"
              :key="provider.key"
              :text="provider.configured ? '' : t('auth.login.providerUnavailable')"
              location="top"
              :disabled="provider.configured"
            >
              <template #activator="{ props }">
                <div v-bind="props">
                  <v-btn
                    block
                    size="large"
                    variant="outlined"
                    :prepend-icon="provider.icon"
                    :append-icon="provider.configured ? undefined : 'mdi-lock-outline'"
                    :loading="loadingProvider === provider.key"
                    :disabled="isDisabled(provider)"
                    @click="startProviderLogin(provider.key)"
                  >
                    {{ t('auth.login.signInWith', { provider: t(`auth.login.providers.${provider.key}`) }) }}
                  </v-btn>
                </div>
              </template>
            </v-tooltip>
          </div>

          <div class="d-flex align-center ga-3 my-5">
            <v-divider />
            <span class="text-caption text-medium-emphasis text-no-wrap">{{ t('auth.login.altProvider') }}</span>
            <v-divider />
          </div>

          <v-btn
            block
            size="large"
            variant="outlined"
            class="mb-4"
            @click="franceConnectDialog = true"
          >
            <v-img
              src="/images/france-connect.svg"
              :alt="t('auth.login.providers.france_connect')"
              width="180"
              height="44"
              contain
            />
          </v-btn>

          <v-divider class="my-5" />

          <v-checkbox
            v-model="acceptedTerms"
            hide-details
            color="primary"
            density="compact"
          >
            <template #label>
              <span class="text-body-2">
                {{ t('auth.login.acceptPrefix') }}
                <!-- eslint-disable-next-line link-checker/valid-route, link-checker/valid-sitemap-link -->
                <NuxtLink to="/docs/legal/terms" class="text-primary text-decoration-none">{{ t('auth.login.terms') }}</NuxtLink>
                {{ t('auth.login.and') }}
                <!-- eslint-disable-next-line link-checker/valid-route, link-checker/valid-sitemap-link -->
                <NuxtLink to="/docs/legal/privacy" class="text-primary text-decoration-none">{{ t('auth.login.privacy') }}</NuxtLink>.
              </span>
            </template>
          </v-checkbox>

          <v-slide-y-reverse-transition>
            <v-alert
              v-if="error"
              type="error"
              variant="tonal"
              density="compact"
              rounded="lg"
              class="mt-4"
            >
              {{ error }}
            </v-alert>
          </v-slide-y-reverse-transition>
        </v-sheet>
      </v-col>
    </v-row>

    <v-dialog v-model="franceConnectDialog" max-width="360">
      <v-card rounded="lg">
        <v-card-title class="text-h6">{{ t('auth.login.franceConnect.title') }}</v-card-title>
        <v-card-text class="text-body-2 text-medium-emphasis">{{ t('auth.login.franceConnect.body') }}</v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn color="primary" variant="text" @click="franceConnectDialog = false">{{ t('common.close') }}</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>

<script setup lang="ts">
import { useTheme } from 'vuetify'

definePageMeta({ layout: 'auth', width: 'full-bleed' })

const { t } = useI18n()
const route = useRoute()
const vuetifyTheme = useTheme()
const { session, fetchMe } = useAuthSession()

const acceptedTerms = ref(true)
const franceConnectDialog = ref(false)
const loadingProvider = ref('')
const error = ref('')

type ProviderKey = 'google' | 'microsoft' | 'github' | 'apple'

interface LoginProvider {
  key: ProviderKey
  icon: string
  configured: boolean
}

const { data: configuredProvidersData } = await useFetch<Record<ProviderKey, boolean>>('/api/v1/auth/oidc/providers', {
  default: () => ({
    google: false,
    microsoft: false,
    github: false,
    apple: false
  })
})

const configuredProviders = computed<Record<ProviderKey, boolean>>(() => {
  const providers = configuredProvidersData.value ?? {
    google: false,
    microsoft: false,
    github: false,
    apple: false
  }

  return {
    google: Boolean(providers.google),
    microsoft: Boolean(providers.microsoft),
    github: Boolean(providers.github),
    apple: Boolean(providers.apple)
  }
})

const primaryProviders = computed<LoginProvider[]>(() => [
  { key: 'google', icon: 'mdi-google', configured: configuredProviders.value.google },
  { key: 'microsoft', icon: 'mdi-microsoft', configured: configuredProviders.value.microsoft },
  { key: 'apple', icon: 'mdi-apple', configured: configuredProviders.value.apple },
  { key: 'github', icon: 'mdi-github', configured: configuredProviders.value.github }
])

const logoSrc = computed(() =>
  vuetifyTheme.current.value.dark
    ? '/brand/logo/svg/pdapi_lockup_horizontal_reverse.svg'
    : '/brand/logo/svg/pdapi_lockup_horizontal_primary.svg'
)

const logoStackedSrc = computed(() =>
  vuetifyTheme.current.value.dark
    ? '/brand/logo/svg/pdapi_symbol_reverse.svg'
    : '/brand/logo/svg/pdapi_symbol_primary.svg'
)

const next = computed(() =>
  typeof route.query.next === 'string' ? route.query.next : '/'
)

const accessErrorMessage = computed(() =>
  route.query.error === 'insufficient_role'
    ? t('auth.login.insufficientRole', {
        required: route.query.required ?? 'admin',
        current: route.query.current ?? 'public'
      })
    : ''
)

const isDisabled = (provider: LoginProvider) =>
  !provider.configured || !acceptedTerms.value || loadingProvider.value.length > 0

useSeoMeta({
  title: t('auth.login.seo.title'),
  description: t('auth.login.seo.description')
})

onMounted(async () => {
  if (route.query.error) return
  if (session.value || await fetchMe()) {
    await navigateTo(next.value)
  }
})

async function startProviderLogin(provider: ProviderKey) {
  if (!configuredProviders.value[provider]) {
    error.value = t('auth.login.providerUnavailable')
    return
  }

  if (!acceptedTerms.value) {
    error.value = t('auth.login.acceptTermsRequired')
    return
  }
  loadingProvider.value = provider
  error.value = ''
  try {
    const callbackUrl = `/auth/callback/${provider}`
    const authorizeUrl = `/api/v1/auth/oidc/authorize?provider=${encodeURIComponent(provider)}&next=${encodeURIComponent(next.value)}&callback=${encodeURIComponent(callbackUrl)}`
    await navigateTo(authorizeUrl, { external: true })
  } catch {
    error.value = t('auth.login.failed')
  } finally {
    loadingProvider.value = ''
  }
}
</script>
