<template>
  <div>
    <B2bPageHeader :title="t('dashboard.settings.title')" :subtitle="t('dashboard.settings.subtitle')" />

    <v-tabs v-model="tab" class="mb-6">
      <v-tab value="profile">{{ t('dashboard.settings.tabs.profile') }}</v-tab>
      <v-tab value="org">{{ t('dashboard.settings.tabs.org') }}</v-tab>
      <v-tab value="security">{{ t('dashboard.settings.tabs.security') }}</v-tab>
    </v-tabs>

    <v-tabs-window v-model="tab">
      <!-- Profile tab -->
      <v-tabs-window-item value="profile">
        <v-card rounded="lg" variant="outlined" class="pa-6" max-width="560">
          <div class="d-flex align-center ga-4 mb-6">
            <v-avatar
              :image="session?.user?.avatarUrl ?? undefined"
              :color="session?.user?.avatarUrl ? undefined : 'primary'"
              size="56"
            >
              <template v-if="!session?.user?.avatarUrl">
                <span class="text-h6 font-weight-bold">{{ initials }}</span>
              </template>
            </v-avatar>
            <div>
              <div class="text-subtitle-1 font-weight-bold">{{ session?.user?.displayName || '-' }}</div>
              <div class="text-caption text-medium-emphasis">{{ session?.user?.email }}</div>
              <v-chip v-if="session?.user?.platformAdmin" size="x-small" color="primary" variant="tonal" class="mt-1">
                {{ t('dashboard.settings.platformAdmin') }}
              </v-chip>
            </div>
          </div>

          <v-alert type="info" variant="tonal" density="compact">
            {{ t('dashboard.settings.profileReadOnly') }}
          </v-alert>
        </v-card>
      </v-tabs-window-item>

      <!-- Organization tab -->
      <v-tabs-window-item value="org">
        <v-card rounded="lg" variant="outlined" class="pa-6" max-width="560">
          <v-list lines="two">
            <v-list-item :title="t('dashboard.settings.orgName')" :subtitle="session?.organization?.name ?? '-'" />
            <v-list-item :title="t('dashboard.settings.orgSlug')" :subtitle="session?.organization?.slug ?? '-'" />
            <v-list-item :title="t('dashboard.settings.role')" :subtitle="session?.role ?? '-'" />
            <v-list-item
              :title="t('dashboard.settings.balance')"
              :subtitle="`${session?.organization?.balanceCredits ?? 0} ${t('dashboard.billing.creditsUnit')}`"
            />
          </v-list>
        </v-card>
      </v-tabs-window-item>

      <!-- Security tab -->
      <v-tabs-window-item value="security">
        <v-card rounded="lg" variant="outlined" class="pa-6" max-width="560">
          <div class="text-subtitle-2 font-weight-bold mb-3">{{ t('dashboard.settings.oidcProvider') }}</div>
          <p class="text-body-2 text-medium-emphasis mb-6">{{ t('dashboard.settings.oidcHint') }}</p>

          <div class="text-subtitle-2 font-weight-bold mb-3">{{ t('dashboard.settings.apiKeyGuidance') }}</div>
          <p class="text-body-2 text-medium-emphasis">{{ t('dashboard.settings.apiKeyGuidanceHint') }}</p>
          <v-btn class="mt-3" variant="outlined" to="/dashboard/api-keys" prepend-icon="mdi-key-variant">
            {{ t('dashboard.keys.title') }}
          </v-btn>
        </v-card>
      </v-tabs-window-item>
    </v-tabs-window>
  </div>
</template>

<script setup lang="ts">
definePageMeta({ layout: 'default', middleware: ['authenticated'] })

const { t } = useI18n()
const { session } = useAuthSession()
useSeoMeta({ title: t('dashboard.settings.seo.title') })

const tab = ref('profile')

const initials = computed(() => {
  const name = session.value?.user?.displayName || session.value?.user?.email || '?'
  return name.slice(0, 2).toUpperCase()
})
</script>
