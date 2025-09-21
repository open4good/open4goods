<script setup lang="ts"></script>

<template>
  <v-container fluid>
    <v-row>
      <v-col cols="3">
        <the-main-logo />
      </v-col>
      <v-col cols="9">
        <the-hero-menu />
      </v-col>
    </v-row>
  </v-container>
  <v-container fluid class="fill-height align-start">
    <v-row class="fill-height">
      <v-col cols="12">
        <The-hero-video />
      </v-col>
    </v-row>
  </v-container>

  <The-items-selector />

  <Bloc3Hz />
</template>

<style scoped lang="sass"></style>
