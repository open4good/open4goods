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
  </v-container>
</template>

<script setup lang="ts">
import { authService } from '~~/shared/api-client/services/auth.services'
import { useAuthStore } from '~/stores/useAuthStore'

const username = ref('')
const password = ref('')
const loading = ref(false)
const error = ref('')
const valid = ref(true)
const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const canonicalUrl = useCanonicalUrl()

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

const isSafeRedirectTarget = (target: unknown): target is string =>
  typeof target === 'string' &&
  target.startsWith('/') &&
  !target.startsWith('//')

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
