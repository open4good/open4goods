<template>
  <v-container style="max-width: 400px">
    <template v-if="isGoogleSsoEnabled">
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
import { isSafeRedirectTarget } from '~~/shared/utils/_safe-redirect'

const route = useRoute()
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

</script>
