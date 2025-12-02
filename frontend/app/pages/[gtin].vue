<template>
  <div class="gtin-redirect" />
</template>

<script setup lang="ts">
import type { ProductDto } from '~~/shared/api-client'
import {
  extractRawGtinParam,
  isValidRawGtin,
} from '~~/shared/utils/_gtin'
import { resolveGtinRedirectTarget } from '~/utils/_gtin-redirect'

definePageMeta({
  validate: (route) => isValidRawGtin(route.params.gtin),
})

const route = useRoute()
const rawGtin = extractRawGtinParam(route.params.gtin)

if (!rawGtin) {
  throw createError({ statusCode: 404, statusMessage: 'Page not found' })
}

const requestHeaders = useRequestHeaders(['host', 'x-forwarded-host'])

const targetPath = await resolveGtinRedirectTarget(rawGtin, {
  fetchProduct: (gtin: string) =>
    $fetch<ProductDto>(`/api/products/${gtin}`, {
      headers: requestHeaders,
    }),
  createError,
})

await navigateTo(targetPath, { replace: true, redirectCode: 301 })
</script>

<style scoped>
.gtin-redirect {
  display: none;
}
</style>
