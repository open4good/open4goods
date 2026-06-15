<template>
  <v-sheet class="inf-admin-page-header pa-5 pa-md-6" rounded="lg">
    <div class="d-flex flex-column flex-lg-row align-start align-lg-center ga-4">
      <div class="d-flex align-start ga-4 flex-grow-1">
        <v-avatar v-if="icon" color="primary" variant="tonal" rounded="lg" size="48">
          <v-icon :icon="icon" size="26" />
        </v-avatar>

        <div>
          <p v-if="badge" class="text-overline text-primary mb-1">{{ badge }}</p>
          <h1 class="text-h4 text-md-h3 font-weight-bold mb-2">
            <slot name="title">{{ title }}</slot>
          </h1>
          <p v-if="subtitle" class="text-body-1 text-medium-emphasis mb-0 inf-admin-page-header__subtitle">
            {{ subtitle }}
          </p>
          <div v-if="$slots.meta" class="d-flex flex-wrap ga-2 mt-4">
            <slot name="meta" />
          </div>
        </div>
      </div>

      <div v-if="$slots.actions" class="d-flex flex-wrap ga-2 justify-start justify-lg-end inf-admin-page-header__actions">
        <slot name="actions" />
      </div>
    </div>
  </v-sheet>
</template>

<script setup lang="ts">
defineProps<{
  title: string
  subtitle?: string
  badge?: string
  icon?: string
}>()
</script>

<style scoped>
.inf-admin-page-header {
  border: 1px solid rgba(var(--v-theme-primary), 0.16);
  background:
    linear-gradient(135deg, rgba(var(--v-theme-primary), 0.1), rgba(var(--v-theme-surface), 0.96) 42%),
    rgb(var(--v-theme-surface));
}

.inf-admin-page-header__subtitle {
  max-width: 760px;
}

.inf-admin-page-header__actions {
  min-width: min(100%, 260px);
}
</style>
