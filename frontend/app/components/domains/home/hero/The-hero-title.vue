<script setup lang="ts">
</script>

<template>
    <div class="hero-title-container">
        <h1 class="hero-title-text">
            Le comparateur qui pense à demain
        </h1>
        <h2 class="hero-subtitle-text">
            Ton assistant achat, Libre et Indépendant.
        </h2>
        <div class="pt-2">
            <TheSearchBar />
        </div>
    </div>
</template>

<style scoped lang="sass">
.hero-title-container
  position: absolute
  padding: 20px
  top: 50%
  left: 34%
  width: 66%
  transform: translate(-50%, -50%)
  z-index: 10
  text-align: left


.hero-title-text, .hero-subtitle-text
    color: white

.hero-title-text
    font-size: clamp(1.5rem, 6vw, 4rem)
    font-weight: 700

.hero-subtitle-text
    font-size: 2rem
    font-weight: 600
</style>
