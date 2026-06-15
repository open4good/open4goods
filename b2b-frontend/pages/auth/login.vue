<template>
  <div class="auth-page">
    <section class="auth-page__visual" aria-hidden="true">
      <v-img
        src="/images/auth-visual.png"
        alt=""
        cover
        class="auth-page__visual-image"
      />
      <div class="auth-page__visual-overlay" />

      <div class="auth-page__visual-inner">
        <v-img
          :src="logoStackedSrc"
          :alt="t('app.name')"
          height="96"
          width="96"
          contain
          class="auth-page__symbol"
        />

        <h1 class="auth-page__tagline">
          {{ t('auth.login.tagline') }}
        </h1>
      </div>
    </section>

    <section class="auth-page__form" :aria-label="t('auth.login.title')">
      <v-sheet class="auth-page__form-inner">
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

        <div class="auth-page__heading mb-8">
          <h2 class="auth-page__title">{{ t('auth.login.title') }}</h2>
          <p class="auth-page__subtitle text-medium-emphasis mt-2">
            {{ t('auth.login.subtitle') }}
          </p>
        </div>

        <div class="d-flex flex-column ga-3">
          <v-tooltip
            v-for="(provider, i) in primaryProviders"
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
                  class="auth-page__provider-btn"
                  :class="{ 'auth-page__provider-btn--unavailable': !provider.configured }"
                  :style="{ '--stagger': i }"
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
          <span class="text-caption text-medium-emphasis text-no-wrap">
            {{ t('auth.login.altProvider') }}
          </span>
          <v-divider />
        </div>

        <div class="d-flex flex-column ga-3">
          <v-btn
            block
            size="large"
            variant="text"
            class="auth-page__france-connect"
            @click="franceConnectDialog = true"
          >
            <v-img
              src="/images/france-connect.svg"
              :alt="t('auth.login.providers.france_connect')"
              width="210"
              height="54"
              contain
            />
          </v-btn>
        </div>

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
              <NuxtLink to="/docs/legal/terms" class="auth-page__link">{{ t('auth.login.terms') }}</NuxtLink>
              {{ t('auth.login.and') }}
              <!-- eslint-disable-next-line link-checker/valid-route, link-checker/valid-sitemap-link -->
              <NuxtLink to="/docs/legal/privacy" class="auth-page__link">{{ t('auth.login.privacy') }}</NuxtLink>.
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
    </section>

    <v-dialog v-model="franceConnectDialog" max-width="360">
      <v-card rounded="lg">
        <v-card-title class="text-h6">
          {{ t('auth.login.franceConnect.title') }}
        </v-card-title>
        <v-card-text class="text-body-2 text-medium-emphasis">
          {{ t('auth.login.franceConnect.body') }}
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn color="primary" variant="text" @click="franceConnectDialog = false">
            {{ t('common.close') }}
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>

<script setup lang="ts">
import { useTheme } from 'vuetify'

definePageMeta({ layout: 'auth', width: 'semi-fluid' })

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
    ? '/brand/logo/svg/infera_lockup_horizontal_reverse.svg'
    : '/brand/logo/svg/infera_lockup_horizontal_primary.svg'
)

const logoStackedSrc = computed(() =>
  vuetifyTheme.current.value.dark
    ? '/brand/logo/svg/infera_symbol_reverse.svg'
    : '/brand/logo/svg/infera_symbol_primary.svg'
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
  // If we have an error in the query, do not auto-redirect even if we have a session.
  // This prevents infinite redirect loops when a user has insufficient permissions.
  if (route.query.error) {
    return
  }

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

<style scoped lang="scss">
.auth-page {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(420px, 1fr);
  height: calc(100vh - 104px);
  width: 100%;
  position: relative;
  overflow: hidden;
  background: var(--inf-token-color-bg-base);
  border-radius: 24px;
  box-shadow: var(--infera-shadow-soft);
  border: 1px solid var(--inf-token-color-line-subtle);
  margin: 24px 0;

  @media (max-width: 1024px) {
    grid-template-columns: 1fr;
    height: auto;
    min-height: calc(100vh - 56px);
    margin: 0;
    border-radius: 0;
    border: none;
  }
}

.auth-page__visual {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  background: var(--infera-night);
  animation: slideInLeft 0.55s cubic-bezier(0.22, 1, 0.36, 1) both;

  @media (max-width: 1024px) {
    display: none;
  }
}

.auth-page__visual-image {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  opacity: 0.8;
}

.auth-page__visual-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(to right, rgba(7, 17, 31, 0.4), rgba(7, 17, 31, 0.1));
  z-index: 1;
}

.auth-page__visual-inner {
  position: relative;
  z-index: 2;
  display: grid;
  gap: 24px;
  width: min(100%, 480px);
  padding: 48px;
  text-align: left;
}

.auth-page__symbol {
  filter: drop-shadow(0 0 24px rgba(var(--v-theme-primary), 0.4));
}

.auth-page__tagline {
  font-size: clamp(2.5rem, 4vw, 3.5rem);
  font-weight: 800;
  line-height: 1.1;
  color: white;
  letter-spacing: -0.02em;
  text-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
}

.auth-page__form {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px;
  background: var(--inf-token-color-bg-elevated);
  animation: slideInRight 0.55s cubic-bezier(0.22, 1, 0.36, 1) both;
  animation-delay: 0.07s;
}

.auth-page__form-inner {
  width: 100%;
  max-width: 420px;
  padding: 0;
  background: transparent !important;
  border: none !important;
  box-shadow: none !important;
}

.auth-page__title {
  font-size: 2.25rem;
  font-weight: 700;
  letter-spacing: -0.01em;
  color: var(--inf-token-color-text-primary);
}

.auth-page__subtitle {
  font-size: 1rem;
  line-height: 1.6;
  color: var(--inf-token-color-text-secondary);
}

.auth-page__provider-btn {
  min-height: 52px;
  text-transform: none;
  letter-spacing: 0.01em;
  font-weight: 600;
  border-width: 1.5px;
  border-color: var(--inf-token-color-line-subtle);
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  animation: fadeUp 0.45s cubic-bezier(0.22, 1, 0.36, 1) both;
  animation-delay: calc(0.2s + var(--stagger, 0) * 0.08s);

  &:hover:not(:disabled) {
    border-color: rgb(var(--v-theme-primary));
    background: rgba(var(--v-theme-primary), 0.04);
    transform: translateY(-2px);
    box-shadow: 0 8px 24px rgba(var(--v-theme-primary), 0.12);
  }
}

.auth-page__provider-btn--unavailable {
  border-style: dashed;
  opacity: 0.6;
}

.auth-page__france-connect {
  min-height: 60px;
  transition: all 0.25s ease;

  &:hover {
    background: rgba(0, 0, 145, 0.04);
    transform: translateY(-1px);
  }
}

.auth-page__link {
  color: rgb(var(--v-theme-primary));
  text-decoration: none;
  font-weight: 500;

  &:hover {
    text-decoration: underline;
  }
}

@keyframes slideInLeft {
  from { opacity: 0; transform: translateX(-30px); }
  to { opacity: 1; transform: none; }
}

@keyframes slideInRight {
  from { opacity: 0; transform: translateX(30px); }
  to { opacity: 1; transform: none; }
}

@keyframes fadeUp {
  from { opacity: 0; transform: translateY(16px); }
  to { opacity: 1; transform: none; }
}

@media (prefers-reduced-motion: reduce) {
  .auth-page__visual, .auth-page__form, .auth-page__provider-btn {
    animation: none;
  }
}
</style>
