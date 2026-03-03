<template>
  <v-footer class="app-footer">
    <v-container class="d-flex flex-wrap justify-space-between ga-3 py-4">
      <span>{{ t('footer.placeholder') }}</span>
      <span>{{ t('footer.legal') }}</span>
    </v-container>
  </v-footer>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
const { t } = useI18n()
</script>

<style scoped>
.app-footer {
  border-top: 1px solid rgba(var(--v-theme-on-surface), 0.08);
  background: rgb(var(--v-theme-surface));
}
</style>
