<template>
  <div>
    <h1 class="text-h4 font-weight-bold mb-6">Your Profile</h1>
    <p class="text-body-2 text-medium-emphasis">Profile dashboard — to be implemented in P9.4.</p>
  </div>
</template>
<script setup lang="ts">
definePageMeta({ layout: 'default', middleware: ['authenticated'] })
useSeoMeta({ title: 'Profile - Product Data API' })
</script>
