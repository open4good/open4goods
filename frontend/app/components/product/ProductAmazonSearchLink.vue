<template>
  <div class="product-amazon-search">
    <!-- Text only, no Amazon logo; plain anchor (no prefetch), same rel,
         target and referrer policy as the other offer links (GOU-1396). -->
    <a
      :href="link"
      target="_blank"
      :rel="AFFILIATE_LINK_REL"
      :referrerpolicy="AFFILIATE_LINK_REFERRER_POLICY"
      class="product-amazon-search__link"
      @click="handleClick"
    >
      {{ t('product.amazonSearch.label') }}
    </a>
    <!-- Amazon Associates disclosure: verbatim, clear and visible. -->
    <p class="product-amazon-search__disclosure">
      {{ t('product.amazonSearch.disclosure') }}
    </p>
  </div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { useAnalytics } from '~/composables/useAnalytics'
import {
  AFFILIATE_LINK_REFERRER_POLICY,
  AFFILIATE_LINK_REL,
} from '~/utils/_product-pricing'

/** Merchant name of the Amazon search clicks, as recorded by front-api. */
const AMAZON_MERCHANT = 'amazon.fr'

const props = defineProps<{
  /** `/contrib/<token>` link built by front-api (`offers.amazonSearchLink`). */
  link: string
  /** Product GTIN, for the analytics event only. */
  gtin?: string | number | null
  /** Analytics placement of the link. */
  placement: string
}>()

const { t } = useI18n()
const { trackAffiliateClick, trackProductRedirect, extractTokenFromLink } =
  useAnalytics()

const handleClick = () => {
  const token = extractTokenFromLink(props.link)

  trackProductRedirect({
    token,
    placement: props.placement,
    source: AMAZON_MERCHANT,
    url: props.link,
  })

  trackAffiliateClick({
    token,
    url: props.link,
    merchantName: AMAZON_MERCHANT,
    placement: props.placement,
    productId: props.gtin ?? null,
    gtin: props.gtin ?? null,
  })
}
</script>

<style scoped>
.product-amazon-search {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.35rem;
  text-align: center;
}

.product-amazon-search__link {
  color: rgb(var(--v-theme-primary));
  font-weight: 600;
  text-decoration: underline;
  text-underline-offset: 2px;
}

.product-amazon-search__link:focus-visible {
  outline: 2px solid rgb(var(--v-theme-primary));
  outline-offset: 2px;
  border-radius: 2px;
}

.product-amazon-search__disclosure {
  margin: 0;
  font-size: 0.875rem;
  line-height: 1.4;
  color: rgb(var(--v-theme-text-neutral-secondary));
}
</style>
