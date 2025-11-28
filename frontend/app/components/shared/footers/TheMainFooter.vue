<script lang="ts" setup></script>

<template>
  <v-footer height="auto" class="main-footer pa-0">
    <slot name="footer" />
  </v-footer>
</template>

<style scoped>
.main-footer {
  width: 100%;
  --v-layout-left: 0px;
  --v-layout-right: 0px;
  position: relative;
  background: linear-gradient(
      135deg,
      rgba(var(--v-theme-hero-gradient-start), 0.92) 0%,
      rgba(var(--v-theme-hero-gradient-mid), 0.9) 45%,
      rgba(var(--v-theme-hero-gradient-end), 0.92) 100%
    );
  color: rgb(var(--v-theme-hero-overlay-strong));
  border-top: 1px solid rgba(var(--v-theme-border-primary-strong), 0.4);
  box-shadow: 0 -18px 48px rgba(var(--v-theme-shadow-primary-600), 0.22);
}
.v-theme--nudger.main-footer {
  background: #0083A4!important;
}

.main-footer :deep(a) {
  color: inherit;
}

.main-footer :deep(a:hover) {
  text-decoration: underline;
}
</style>
