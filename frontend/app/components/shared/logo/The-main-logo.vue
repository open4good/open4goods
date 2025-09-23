<script setup lang="ts">
</script>

<template>
  <NuxtLink id="main-logo-link" to="/" class="logo-link">
    <img
      src="@/assets/images/nudger-logo.png"
      alt="Nudger home page"
      class="main-logo"
    />
  </NuxtLink>
</template>


<style scoped lang="sass">
.logo-link
  display: inline-block
  text-decoration: none

.main-logo
  height: auto
  max-width: 100%
  display: block
  transition: opacity 0.2s ease

  &:hover
    opacity: 0.8
</style>