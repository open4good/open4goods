<template>
  <v-menu v-if="isAuthenticated">
    <template #activator="{ props }">
      <v-btn
        v-bind="props"
        class="identity-account"
        prepend-icon="mdi-account-circle-outline"
        append-icon="mdi-chevron-down"
        size="small"
        variant="text"
        rounded="pill"
      >
        {{ userLabel }}
      </v-btn>
    </template>
    <v-list density="compact" nav class="context-switcher-list">
      <v-list-item
        :title="orgLabel"
        :subtitle="roleLabel"
        prepend-icon="mdi-domain"
        to="/dashboard"
        :active="isDashboardRoute"
      />

      <template v-if="canAccessAdmin">
        <v-divider class="my-2" />
        <v-list-subheader>{{ t('nav.contexts.platform') }}</v-list-subheader>
        <v-list-item
          :title="t('nav.admin_overview')"
          prepend-icon="mdi-shield-crown-outline"
          color="primary"
          to="/admin"
          :active="isAdminRoute"
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
const { t } = useI18n()
const { session, logout } = useAuthSession()
const route = useRoute()
const router = useRouter()

const isAuthenticated = computed(() => Boolean(session.value))
const canAccessAdmin = computed(() => session.value?.user?.platformAdmin === true)
const userLabel = computed(() => session.value?.user?.displayName || session.value?.user?.email || t('nav.account'))
const orgLabel = computed(() => session.value?.organization?.name || t('nav.contexts.personal'))
const roleLabel = computed(() => session.value?.role ? t(`nav.org.roles.${session.value.role}`, session.value.role) : '')

const isAdminRoute = computed(() => route.path.startsWith('/admin'))
const isDashboardRoute = computed(() => route.path.startsWith('/dashboard'))

async function onLogout() {
  await logout()
  await router.push({ path: '/auth/login' })
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
