<template>
  <v-container fluid class="pa-5">
    <v-row align="center" justify="space-between" no-gutters>
      <v-col cols="auto" class="flex-grow-1">
        <v-row
          align="center"
          :class="{
            'ga-4': $vuetify.display.mdAndUp,
            'ga-2': $vuetify.display.smAndDown,
          }"
        >
          <v-col cols="auto">
            <NuxtLink to="/impact-score" class="menu-link text-decoration-none">
              Impact-score
            </NuxtLink>
          </v-col>
          <v-col cols="auto">
            <NuxtLink to="/produits" class="menu-link text-decoration-none">
              Les produits
            </NuxtLink>
          </v-col>
          <v-col cols="auto">
            <NuxtLink to="/blog" class="menu-link text-decoration-none"> Blog </NuxtLink>
          </v-col>
        </v-row>
      </v-col>
      <v-col cols="auto">
        <v-btn
          class="btn-nudge-tools"
          variant="flat"
          color="success"
          to="/nudge-tools"
          :size="$vuetify.display.smAndDown ? 'small' : 'default'"
        >
          Nudge-tools
        </v-btn>
      </v-col>
    </v-row>
  </v-container>
</template>

<script setup lang="ts">
// No props or emits needed as specified
</script>

<style lang="sass" scoped>
@use '@/assets/sass/base/mixins' as *

.menu-link
  font-family: 'Nunito Sans', sans-serif
  font-weight: 900
  color: #203c26
  text-align: center
  line-height: 1.348
  @include transition()

  &:hover
    opacity: 0.8

.btn-nudge-tools
  font-family: 'Rubik One', sans-serif !important
  text-transform: none !important
  background-color: #00de9f !important
  color: black !important
  border: 2px solid black
  border-radius: 9px !important
  min-width: auto

  &:hover
    opacity: 0.9
</style>
