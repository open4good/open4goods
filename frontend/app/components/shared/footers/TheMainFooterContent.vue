<script lang="ts" setup>
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
</script>

<template>
  <div class="text-center pa-4">
    &copy; {{ new Date().getFullYear() }}
    <a href="https://open4goods.org" target="_blank" rel="noopener">Open4Goods</a>.
    {{ t('siteIdentity.footer.rightsReserved') }}
  </div>
</template>

<style lang="postcss" scoped></style>
