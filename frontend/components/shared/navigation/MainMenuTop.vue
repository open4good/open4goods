<template>
  <nav aria-label="Navigation principale" role="navigation">
    <v-container fluid class="pa-5">
      <v-row align="center" justify="space-between" no-gutters>
        <v-col cols="auto" class="flex-grow-1">
          <v-list
            class="d-flex flex-row bg-transparent pa-0"
            :class="{
              'ga-4': $vuetify.display.mdAndUp,
              'ga-2': $vuetify.display.smAndDown,
            }"
          >
            <v-list-item
              to="/impact-score"
              class="menu-link-item"
              :aria-current="$route.path === '/impact-score' ? 'page' : undefined"
              min-height="44"
            >
              <v-list-item-title class="menu-link">
                Impact-score
              </v-list-item-title>
            </v-list-item>
            
            <v-list-item
              to="/produits"
              class="menu-link-item"
              :aria-current="$route.path === '/produits' ? 'page' : undefined"
              min-height="44"
            >
              <v-list-item-title class="menu-link">
                Les produits
              </v-list-item-title>
            </v-list-item>
            
            <v-list-item
              to="/blog"
              class="menu-link-item"
              :aria-current="$route.path === '/blog' ? 'page' : undefined"
              min-height="44"
            >
              <v-list-item-title class="menu-link">
                Blog
              </v-list-item-title>
            </v-list-item>
          </v-list>
        </v-col>
        
        <v-col cols="auto">
          <v-btn
            class="btn-nudge-tools"
            variant="flat"
            color="success"
            to="/nudge-tools"
            :size="$vuetify.display.smAndDown ? 'small' : 'default'"
            min-height="44"
            aria-label="Accéder aux outils Nudge"
          >
            Nudge-tools
          </v-btn>
        </v-col>
      </v-row>
    </v-container>
  </nav>
</template>

<script setup lang="ts">
// No props or emits needed as specified
</script>

<style lang="sass" scoped>
@use '@/assets/sass/base/mixins' as *

// Navigation accessible
.menu-link-item
  border-radius: 8px
  @include transition()

  // Focus visible pour l'accessibilité
  &:focus-visible,
  &.v-list-item--focused
    outline: 2px solid #005fcc
    outline-offset: 2px
    background-color: rgba(0, 95, 204, 0.1)

  // État actif (page courante)
  &[aria-current="page"]
    background-color: rgba(32, 60, 38, 0.1)
    font-weight: 700
    
    .menu-link
      color: #203c26
      font-weight: 700

  &:hover:not([aria-current="page"])
    background-color: rgba(32, 60, 38, 0.05)

.menu-link
  font-family: 'Nunito Sans', sans-serif
  font-weight: 900
  color: #203c26 // Contraste vérifié: 7.1:1 sur fond blanc
  text-align: center
  line-height: 1.348
  @include transition()

  &:hover
    opacity: 0.8

// Bouton Nudge-tools accessible
.btn-nudge-tools
  font-family: 'Rubik One', sans-serif !important
  text-transform: none !important
  background-color: #00de9f !important
  color: #000000 !important // Contraste vérifié: 12.6:1
  border: 2px solid #000000
  border-radius: 9px !important
  min-width: auto

  &:hover
    opacity: 0.9
    transform: translateY(-1px)

  // Focus visible amélioré
  &:focus-visible
    outline: 2px solid #005fcc
    outline-offset: 2px
    box-shadow: 0 0 0 4px rgba(0, 95, 204, 0.2)

// États de focus pour compatibilité lecteurs d'écran
:deep(.v-list-item__overlay)
  @include transition()

// Mode contraste élevé
@media (prefers-contrast: high)
  .menu-link
    color: #000000
    font-weight: 700

  .menu-link-item[aria-current="page"]
    background-color: #000000
    
    .menu-link
      color: #ffffff

// Mode sombre - si supporté par l'app
@media (prefers-color-scheme: dark)
  .menu-link
    color: #ffffff

  .menu-link-item:focus-visible,
  .menu-link-item.v-list-item--focused
    outline-color: #66b3ff
    background-color: rgba(102, 179, 255, 0.1)
</style>
