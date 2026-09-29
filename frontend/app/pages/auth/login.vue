<template>
  <v-container style="max-width: 400px">
    <v-btn color="primary" class="mt-4" prepend-icon="mdi-google" @click="signIn">
      {{ t('auth.loginWithGoogle') }}
    </v-btn>
  </v-container>
</template>

<script setup lang="ts">
const route = useRoute()
const { t } = useI18n()

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

const signIn = () => navigateTo(`/auth/google?redirect=${encodeURIComponent(resolveRedirectTarget())}`, { external: true })
</script>
