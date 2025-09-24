<script lang="ts" setup></script>

<template>
  <v-footer
    app
    height="auto"
    color="secondary"
    class="main-footer text-white pa-0"
    tag="footer"
  >
    <slot name="footer" />
  </v-footer>
</template>

<style scoped>
.main-footer {
  width: 100%;
  display: block;
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
