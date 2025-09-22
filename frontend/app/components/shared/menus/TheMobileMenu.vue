<template>
  <div class="mobile-menu">
    <v-list class="pa-0">
      <v-list-item
        class="px-6 py-4 border-bottom d-flex justify-space-between align-center"
      >
        <v-list-item-title class="text-h6 font-weight-bold">
          Menu
        </v-list-item-title>
        <template #append>
          <v-btn
            icon
            variant="text"
            size="small"
            aria-label="Fermer le menu"
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
        @click="$emit('close')"
      >
        <template #prepend>
          <v-icon :icon="item.icon" class="me-4" />
        </template>
        <v-list-item-title class="text-body-1">
          {{ item.title }}
        </v-list-item-title>
      </v-list-item>
    </v-list>
  </div>
</template>

<script setup lang="ts">
interface MenuItem {
  title: string
  to: string
  icon: string
}

defineEmits<{
  close: []
}>()

const menuItems: MenuItem[] = [
  {
    title: 'Impact-score',
    to: '/impact-score',
    icon: 'mdi-chart-line'
  },
  {
    title: 'Products',
    to: '/products',
    icon: 'mdi-package-variant'
  },
  {
    title: 'Blog',
    to: '/blog',
    icon: 'mdi-post'
  },
  {
    title: 'Contact',
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