package org.open4goods.services.feedservice.provider;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.open4goods.model.affiliation.AffiliationCapability;
import org.open4goods.model.affiliation.AffiliationProgram;
import org.open4goods.model.affiliation.AffiliationPromotion;
import org.open4goods.model.affiliation.AffiliationTransaction;

/**
 * SPI interface for affiliation providers.
 * 
 * @author open4goods
 */
public interface AffiliationProvider
{
    /**
     * Returns the name of this affiliation provider (e.g. "Effiliation", "Awin").
     * 
     * @return the provider name
     */
    String getProviderName();

    /**
     * Returns the capabilities supported by this provider.
     * 
     * @return set of capabilities
     */
    default Set<AffiliationCapability> getCapabilities()
    {
        return Collections.emptySet();
    }

    /**
     * Retrieves all normalized affiliation programs for this provider.
     * 
     * @return collection of programs
     */
    default Collection<AffiliationProgram> getPrograms()
    {
        return Collections.emptyList();
    }

    /**
     * Retrieves all active promotions/voucher codes for this provider.
     * 
     * @return collection of promotions
     */
    default Collection<AffiliationPromotion> getPromotions()
    {
        return Collections.emptyList();
    }

    /**
     * Retrieves transactions that occurred within the specified date range.
     * 
     * @param from start date/time
     * @param to end date/time
     * @return collection of transactions
     */
    default Collection<AffiliationTransaction> getTransactions(Instant from, Instant to)
    {
        return Collections.emptyList();
    }

    /**
     * Builds a customized tracking redirection link for a target URL and optional sub-IDs.
     * 
     * @param programId target program/advertiser ID
     * @param targetUrl destination merchant landing URL
     * @param subIds map of publisher sub-IDs
     * @return the tracking redirect link
     */
    default String buildTrackingLink(String programId, String targetUrl, Map<String, String> subIds)
    {
        throw new UnsupportedOperationException("Tracking link building not supported by " + getProviderName());
    }
}
