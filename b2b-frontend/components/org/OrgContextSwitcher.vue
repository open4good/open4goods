<template>
  <v-menu v-if="isAuthenticated">
    <template #activator="{ props }">
      <v-btn
        v-bind="props"
        class="identity-account"
        prepend-icon="mdi-domain"
        append-icon="mdi-chevron-down"
        size="small"
        variant="text"
        rounded="pill"
      >
        {{ currentContextName }}
      </v-btn>
    </template>
    <v-list density="compact" nav class="context-switcher-list">
      <v-list-subheader>{{ t('nav.contexts.personal') }}</v-list-subheader>
      <v-list-item
        :title="userLabel"
        prepend-icon="mdi-account-circle-outline"
        :to="'/profile'"
        :active="!isOrgRoute && !isAdminRoute"
        @click="switchContext('personal')"
      />

      <template v-if="organizations.length > 0">
        <v-divider class="my-2" />
        <v-list-subheader>{{ t('nav.contexts.organizations') }}</v-list-subheader>
        <v-list-item
          v-for="org in organizations"
          :key="org.slug"
          :title="org.name"
          prepend-icon="mdi-domain"
          :to="`/org/${org.slug}`"
          :active="currentOrgSlug === org.slug"
          @click="switchContext('org', org.slug)"
        >
          <template #append>
            <v-chip v-if="org.status === 'PENDING_VALIDATION'" size="x-small" color="warning" variant="tonal">
              {{ t('org.status.pending') }}
            </v-chip>
          </template>
        </v-list-item>
      </template>

      <template v-if="canAccessAdmin">
        <v-divider class="my-2" />
        <v-list-subheader>{{ t('nav.contexts.platform') }}</v-list-subheader>
        <v-list-item
          :title="t('nav.admin_overview')"
          prepend-icon="mdi-shield-crown-outline"
          color="primary"
          :to="'/admin'"
          :active="isAdminRoute"
          @click="switchContext('admin')"
        />
      </template>

      <v-divider class="my-2" />
      <v-list-item
        :title="t('nav.logout')"
        prepend-icon="mdi-logout"
        @click="onLogout"
      />
    </v-list>
  </v-menu>
</template>

<script setup lang="ts">
import type { OrganizationResponse } from '~/composables/useCustomerOrganizationRepository'

const { t } = useI18n()
const { session, logout } = useAuthSession()
const route = useRoute()
const router = useRouter()
const organizationRepository = useCustomerOrganizationRepository()

const organizations = ref<OrganizationResponse[]>([])

const isAuthenticated = computed(() => Boolean(session.value))
const canAccessAdmin = computed(() => session.value?.level === 'admin' || Boolean(session.value?.roles?.includes('ROLE_ADMIN')))
const userLabel = computed(() => session.value?.name || session.value?.email || session.value?.subject?.slice(0, 12) || t('nav.account'))

const isAdminRoute = computed(() => route.path.startsWith('/admin'))
const isOrgRoute = computed(() => route.path.startsWith('/org/'))
const currentOrgSlug = computed(() => isOrgRoute.value ? route.params.slug as string : null)

const currentContextName = computed(() => {
  if (isAdminRoute.value) return t('nav.contexts.admin_context')
  if (isOrgRoute.value && currentOrgSlug.value) {
    const org = organizations.value.find(o => o.slug === currentOrgSlug.value)
    return org?.name || currentOrgSlug.value
  }
  return userLabel.value
})

onMounted(async () => {
  if (isAuthenticated.value) {
    try {
      organizations.value = await organizationRepository.list()
    } catch {
      // Silently fail if we can't load organizations
    }
  }
})

function switchContext(_type: 'personal' | 'org' | 'admin', _slug?: string) {
  // Navigation is handled by the :to prop on the list-item
}

async function onLogout() {
  await logout()
  await router.push({ path: '/auth/login', query: { next: route.fullPath } })
}
</script>

<style scoped lang="scss">
.identity-account {
  text-transform: none;
  font-weight: 600;
  border-radius: 999px;
  border: 1px solid color-mix(in oklab, var(--inf-token-color-line-subtle) 70%, transparent);
  background: color-mix(in oklab, var(--inf-token-color-bg-elevated) 35%, transparent);
}

.context-switcher-list {
  min-width: 240px;
}
</style>
