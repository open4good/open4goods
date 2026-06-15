<template>
  <div>
    <div class="d-flex align-center justify-space-between flex-wrap ga-3 mb-6">
      <div>
        <div class="text-overline text-primary font-weight-bold">Admin</div>
        <h1 class="text-h4 font-weight-bold">Platform overview</h1>
      </div>
    </div>
    <p class="text-body-2 text-medium-emphasis">Admin panel — to be implemented in P9.5.</p>
  </div>
</template>

<script setup lang="ts">
definePageMeta({ layout: 'default', middleware: ['authenticated'] })
useSeoMeta({ title: 'Admin - Product Data API' })
</script>
