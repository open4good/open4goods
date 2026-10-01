<template>
  <v-container style="max-width: 400px">
    <v-form v-model="valid" @submit.prevent="onSubmit">
      <v-text-field
        v-model="username"
        label="Username"
        required
        prepend-inner-icon="mdi-account"
      />
      <v-text-field
        v-model="password"
        label="Password"
        type="password"
        required
        prepend-inner-icon="mdi-lock"
      />
      <v-btn
        type="submit"
        color="primary"
        class="mt-4"
        :loading="loading"
        :disabled="!valid"
      >
        Login
      </v-btn>
    </v-form>
    <v-alert v-if="error" type="error" class="mt-4">{{ error }}</v-alert>
    <template v-if="isGoogleSsoEnabled">
      <v-divider class="my-4" />
      <v-btn
        variant="outlined"
        block
        prepend-icon="mdi-google"
        :href="googleSignInHref"
        data-testid="google-sign-in"
      >
        {{ $t('auth.google.signIn') }}
      </v-btn>
    </template>
  </v-container>
</template>

<script setup lang="ts">
import { authService } from '~~/shared/api-client/services/auth.services'
import { isSafeRedirectTarget } from '~~/shared/utils/_safe-redirect'
import { useAuthStore } from '~/stores/useAuthStore'

const username = ref('')
const password = ref('')
const loading = ref(false)
const error = ref('')
const valid = ref(true)
const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const config = useRuntimeConfig()

const canonicalUrl = useCanonicalUrl()

const isGoogleSsoEnabled = computed(() => config.public.googleSsoEnabled)
const googleSignInHref = computed(
  () => `/auth/google?redirect=${encodeURIComponent(resolveRedirectTarget())}`
)

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

useSeoMeta({
  ogUrl: () => canonicalUrl.value || undefined,
})

const resolveRedirectTarget = () => {
  const redirectQuery = route.query.redirect

  if (Array.isArray(redirectQuery)) {
    const firstValidTarget = redirectQuery.find(
      (candidate): candidate is string => isSafeRedirectTarget(candidate)
    )
    if (firstValidTarget) {
      return firstValidTarget
    }
  }

  if (isSafeRedirectTarget(redirectQuery)) {
    return redirectQuery
  }

  const redirectedFrom = route.redirectedFrom?.fullPath

  if (isSafeRedirectTarget(redirectedFrom)) {
    return redirectedFrom
  }

  return '/'
}

const onSubmit = async () => {
  loading.value = true
  error.value = ''
  try {
    const { authState } = await authService.login(
      username.value,
      password.value
    )
    authStore.$patch(authState)
    await router.replace(resolveRedirectTarget())
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Login failed'
  } finally {
    loading.value = false
  }
}
</script>
