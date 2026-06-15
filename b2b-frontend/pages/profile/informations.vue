<template>
  <div>
    <h1 class="text-h4 font-weight-bold mb-6">Account Information</h1>
    <p class="text-body-2 text-medium-emphasis">Account information — to be implemented in P9.4.</p>
  </div>
</template>
<script setup lang="ts">
definePageMeta({ layout: 'default', middleware: ['authenticated'] })
useSeoMeta({ title: 'Account - Product Data API' })
</script>
