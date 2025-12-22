<template>
  <div class="team-page">
    <PageHeader
      variant="hero-standard"
      :title="t('team.hero.title')"
      :subtitle="t('team.hero.subtitle')"
      :description-bloc-id="HERO_CORE_BLOC_ID"
      background="gradient"
      layout="single-column"
      container="xl"
      content-align="center"
      heading-level="h1"
      schema-type="AboutPage"
      :og-image="ogImageUrl"
      padding-y="clamp(3rem, 8vw, 5.5rem)"
    />

    <v-progress-linear
      v-if="pending"
      indeterminate
      color="primary"
      class="team-page__loader"
      :aria-label="t('team.loading')"
      role="progressbar"
    />

    <v-container v-if="error" class="py-6 px-4 mx-auto" max-width="xl">
      <v-alert
        type="error"
        variant="tonal"
        border="start"
        prominent
        class="mb-4"
        role="alert"
      >
        {{ t('team.errors.loadFailed') }}
      </v-alert>
      <v-btn color="primary" variant="tonal" @click="refresh">
        {{ t('common.actions.retry') }}
      </v-btn>
    </v-container>

    <TeamMembersSection
      id="core-team"
      :title="t('team.sections.core.title')"
      :description-bloc-id="HERO_CORE_BLOC_ID"
      :members="coreMembers"
      member-variant="core"
      variant="light"
    />

    <TeamMembersSection
      id="contributors"
      :title="t('team.sections.contributors.title')"
      :description-bloc-id="CONTRIBUTORS_HERO_BLOC_ID"
      :members="contributors"
      member-variant="contributor"
      variant="muted"
    />

    <TeamCallouts
      :partners-title="t('team.callouts.partners.title')"
      :partners-cta="t('team.callouts.partners.cta')"
      :partners-link="partnersLink"
      :partners-bloc-id="PARTNERS_BLOC_ID"
      :contact-title="t('team.callouts.contact.title')"
      :contact-description="t('team.callouts.contact.description')"
      :contact-cta="t('team.callouts.contact.cta')"
      :contact-link="contactLink"
    />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { TeamProperties } from '~~/shared/api-client'

import PageHeader from '~/components/shared/header/PageHeader.vue'

const HERO_CORE_BLOC_ID = 'pages:team:hero-core-team:'
const CONTRIBUTORS_HERO_BLOC_ID = 'pages:team:hero-contributors-team:'
const PARTNERS_BLOC_ID = 'pages:team:partenaires:'

const { t } = useI18n()
const localePath = useLocalePath()
const requestURL = useRequestURL()
const requestHeaders = useRequestHeaders(['host', 'x-forwarded-host'])

const { data, pending, error, refresh } = await useAsyncData<TeamProperties>(
  'team-roster',
  () =>
    $fetch<TeamProperties>('/api/team', {
      headers: requestHeaders,
    })
)

const coreMembers = computed(() => data.value?.cores ?? [])
const contributors = computed(() => data.value?.contributors ?? [])

const partnersLink = computed(() => {
  const link = String(t('team.callouts.partners.link'))
  if (link.startsWith('http')) {
    return link
  }

  if (link.startsWith('/')) {
    return link
  }

  return localePath(link)
})

const contactLink = computed(() => localePath('contact'))

const ogImageUrl = computed(() =>
  new URL('/nudger-icon-512x512.png', requestURL.origin).toString()
)

// Note: SEO metadata (title, description, OG tags, canonical, JSON-LD)
// is now handled automatically by PageHeader component via useHeaderSeo composable
</script>

<style scoped lang="sass">
.team-page
  display: flex
  flex-direction: column
  gap: 0

  &__loader
    margin: 0
</style>
