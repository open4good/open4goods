<template>
  <div>
    <B2bPageHeader :title="t('pricing.title')" :subtitle="t('pricing.subtitle')" eyebrow="Pricing" />

    <B2bAsyncState
      v-if="pending"
      state="loading"
      :title="t('pricing.loading')"
      :description="t('pricing.loadingDescription')"
    />
    <B2bAsyncState
      v-else-if="error"
      state="error"
      :title="t('pricing.error')"
      :description="t('pricing.errorDescription')"
    />
    <div v-else class="d-flex flex-column ga-10">
      <section>
        <h2 class="text-h5 font-weight-bold mb-4">{{ t('pricing.packs') }}</h2>
        <B2bBillingCatalog :items="packItems" :cta-label="t('pricing.buyPack')" />
      </section>
      <section>
        <h2 class="text-h5 font-weight-bold mb-4">{{ t('pricing.subscriptions') }}</h2>
        <B2bBillingCatalog :items="subscriptionItems" :cta-label="t('pricing.startPlan')" />
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import B2bAsyncState from '~/components/B2bAsyncState.vue'
import B2bBillingCatalog from '~/components/B2bBillingCatalog.vue'
import B2bPageHeader from '~/components/B2bPageHeader.vue'

interface BillingCatalog {
  packs: Array<{ id: string; amountEur: number | string; credits: number }>
  subscriptions: Array<{ id: string; amountEur: number | string; monthlyCredits: number; rolloverCapMonths: number }>
}

const { t, n } = useI18n()
const { get } = useApiClient()

useLocalizedPageSeo({
  titleKey: 'pricing.seo.title',
  descriptionKey: 'pricing.seo.description'
})

const { data, pending, error } = await useAsyncData('billing-catalog', () => get<BillingCatalog>('/api/v1/customer/billing/catalog'))

const packItems = computed(() => (data.value?.packs ?? []).map((pack) => ({
  id: pack.id,
  name: pack.id,
  description: t('pricing.packDescription'),
  price: n(Number(pack.amountEur), { style: 'currency', currency: 'EUR' }),
  credits: n(pack.credits)
})))

const subscriptionItems = computed(() => (data.value?.subscriptions ?? []).map((subscription) => ({
  id: subscription.id,
  name: subscription.id,
  description: t('pricing.subscriptionDescription', { months: subscription.rolloverCapMonths }),
  price: `${n(Number(subscription.amountEur), { style: 'currency', currency: 'EUR' })} / mo`,
  credits: n(subscription.monthlyCredits)
})))
</script>
