<script lang="ts" setup></script>

<template>
  <v-footer height="auto" color="secondary" class="main-footer text-white pa-0">
    <slot name="footer" />
  </v-footer>
</template>

<style scoped>
.main-footer {
  width: 100%;
  --v-layout-left: 0px;
  --v-layout-right: 0px;
}

.main-footer :deep(a) {
  color: inherit;
}

.main-footer :deep(a:hover) {
  text-decoration: underline;
}
</style>
