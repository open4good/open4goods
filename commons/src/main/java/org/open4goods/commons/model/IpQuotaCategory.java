package org.open4goods.commons.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Enumerates the supported IP-based quota categories shared across services.
 * <p>
 * Each category maps to a stable action key stored by {@code IpQuotaService}
 * so that independent services can read and write the same counters.
 * </p>
 */
@Schema(enumAsRef = true)
public enum IpQuotaCategory
{
    FEEDBACK_VOTE("feedback.vote"),
    REVIEW_GENERATION("review-generation"),
    CONTACT_MESSAGE("contact.message");

    private final String actionKey;

    /**
     * Create a category with its action key used in the quota store.
     *
     * @param actionKey action key persisted in {@code IpQuotaService}
     */
    IpQuotaCategory(String actionKey)
    {
        this.actionKey = actionKey;
    }

    /**
     * Returns the action key persisted in the quota store.
     *
     * @return action key used to count actions per IP
     */
    public String actionKey()
    {
        return actionKey;
    }
}
