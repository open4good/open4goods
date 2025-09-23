<template>
  <menu id="container-main-menu" class="d-none d-md-block">
  <!-- Desktop menu -->
    <div class="d-flex justify-end align-center ga-4">
      <v-list class="d-flex justify-end font-weight-bold">
        <v-list-item
          v-for="item in menuItems"
          :key="item.path"
          class="main-menu-items"
          :class="{ 'active': isActiveRoute(item.path) }"
          @click="navigateToPage(item.path)"
        >
          <v-list-item-title>{{ item.label }}</v-list-item-title>
        </v-list-item>
      </v-list>
      <v-btn
        v-if="isLoggedIn"
        color="secondary"
        rounded="pill"
        class="font-weight-bold"
        data-testid="hero-logout"
        @click="handleLogout"
      >
        <v-icon start icon="mdi-logout" />
        Logout
      </v-btn>
    </div>
  </menu>

  <!-- Mobile menu command -->
  <div class="d-flex justify-end d-md-none">
    <v-btn icon aria-label="Ouvrir le menu" @click="$emit('toggle-drawer')">
      <v-icon>mdi-menu</v-icon>
    </v-btn>
  </div>
</template>

<script setup lang="ts">
const route = useRoute();
const router = useRouter();
const { isLoggedIn, logout } = useAuth();

const handleLogout = async () => {
  if (!isLoggedIn.value) {
    return;
  }

  try {
    await logout();
    await router.push('/');
  } catch (error) {
    console.error('Logout failed', error);
  }
};

defineEmits<{
  "toggle-drawer": [];
}>();

interface MenuItem {
  label: string;
  path: string;
}

const menuItems: MenuItem[] = [
  { label: 'Impact-score', path: '/impact-score' },
  { label: 'Les produits', path: '/produits' },
  { label: 'Blog', path: '/blog' },
  { label: 'Contact', path: '/contact' }
];

const isActiveRoute = (path: string): boolean => {
  if (path === '/') {
    return route.path === '/';
  }
  return route.path.startsWith(path);
};

const navigateToPage = (path: string): void => {
  router.push(path);
};
</script>

<style scoped lang="sass">
.main-menu-items
  color: black
  font-size: 1rem
  cursor: pointer
  font-weight: bolder
  transition: color 0.3s ease
  &:hover
    color: green
  &.active
    color: green
    font-weight: 900
</style>
