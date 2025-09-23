<template>
  <div class="mobile-menu">
    <v-list class="pa-0">
      <v-list-item
        class="px-6 py-4 border-bottom d-flex justify-space-between align-center"
      >
        <v-list-item-title class="text-h6 font-weight-bold">
          {{ t('siteIdentity.menu.title') }}
        </v-list-item-title>
        <template #append>
          <v-btn
            icon
            variant="text"
            size="small"
            :aria-label="t('siteIdentity.menu.closeLabel')"
            @click="$emit('close')"
          >
            <v-icon>mdi-close</v-icon>
          </v-btn>
        </template>
      </v-list-item>

      <v-list-item
        v-for="(item, index) in menuItems"
        :key="index"
        :to="item.to"
        class="px-6 py-4"
        @click="emit('close')"
      >
        <template #prepend>
          <v-icon :icon="item.icon" class="me-4" />
        </template>
        <v-list-item-title class="text-body-1">
          {{ item.title }}
        </v-list-item-title>
      </v-list-item>

      <v-list-item
        v-if="isLoggedIn"
        class="px-6 py-4"
        data-testid="mobile-logout"
        @click="handleLogout"
      >
        <template #prepend>
          <v-icon icon="mdi-logout" class="me-4" />
        </template>
        <v-list-item-title class="text-body-1">
          Logout
        </v-list-item-title>
      </v-list-item>
    </v-list>
  </div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

interface MenuItem {
  title: string
  to: string
  icon: string
}

const emit = defineEmits<{
  close: []
}>()

const { isLoggedIn, logout } = useAuth()
const router = useRouter()

const handleLogout = async () => {
  if (!isLoggedIn.value) {
    return
  }

  try {
    await logout()
    await router.push('/')
  } catch (error) {
    console.error('Logout failed', error)
  } finally {
    emit('close')
  }
}

const menuItems: MenuItem[] = [
  {
    title: t('siteIdentity.menu.items.impactScore'),
    to: '/impact-score',
    icon: 'mdi-chart-line'
  },
  {
    title: t('siteIdentity.menu.items.products'),
    to: '/produits',
    icon: 'mdi-package-variant'
  },
  {
    title: t('siteIdentity.menu.items.blog'),
    to: '/blog',
    icon: 'mdi-post'
  },
  {
    title: t('siteIdentity.menu.items.contact'),
    to: '/contact',
    icon: 'mdi-email'
  }
]
</script>

<style scoped lang="sass">
.mobile-menu
  height: 100%

.border-bottom
  border-bottom: 1px solid rgba(0, 0, 0, 0.12)
</style>