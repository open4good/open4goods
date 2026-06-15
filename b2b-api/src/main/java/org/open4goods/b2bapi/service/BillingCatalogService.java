package org.open4goods.b2bapi.service;

import java.util.List;
import org.open4goods.b2bapi.config.BillingCatalogProperties;
import org.open4goods.b2bapi.dto.billing.BillingCatalogDto;
import org.open4goods.b2bapi.dto.billing.PackDto;
import org.open4goods.b2bapi.dto.billing.SubscriptionDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service for exposing the B2B billing catalog of packs and subscriptions.
 */
@Service
public class BillingCatalogService {

    private final BillingCatalogProperties catalogProperties;

    @Autowired
    public BillingCatalogService(final BillingCatalogProperties catalogProperties) {
        this.catalogProperties = catalogProperties;
    }

    /**
     * Translates catalog properties from b2b-catalog.yml into a public-safe DTO.
     *
     * @return catalog DTO containing packs and subscriptions
     */
    public BillingCatalogDto getCatalog() {
        final List<PackDto> packs = catalogProperties.getBilling().getPacks().entrySet().stream()
                .map(entry -> new PackDto(
                        entry.getKey(),
                        entry.getValue().getAmountEur(),
                        entry.getValue().getCredits(),
                        entry.getValue().getStripePriceId()
                ))
                .toList();

        final List<SubscriptionDto> subs = catalogProperties.getBilling().getSubscriptions().entrySet().stream()
                .map(entry -> new SubscriptionDto(
                        entry.getKey(),
                        entry.getValue().getAmountEur(),
                        entry.getValue().getMonthlyCredits(),
                        entry.getValue().getRolloverCapMonths(),
                        entry.getValue().getStripePriceId()
                ))
                .toList();

        return new BillingCatalogDto(packs, subs);
    }
}
