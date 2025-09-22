<script setup lang="ts">
defineEmits<{
  'toggle-drawer': []
}>()
</script>

<template>
  <v-container fluid>
    <v-row>
      <v-col cols="3">
        <TheMainLogo />
      </v-col>
      <v-col cols="9">
        <TheHeroMenu @toggle-drawer="$emit('toggle-drawer')" />
      </v-col>
    </v-row>
  </v-container>
  <v-container fluid class="fill-height align-start">
    <v-row class="fill-height">
      <v-col cols="12">
        <TheHeroVideo />
      </v-col>
    </v-row>
  </v-container>

  <TheItemsSelector />

  <Bloc3Hz />
</template>

<style scoped lang="sass"></style>
