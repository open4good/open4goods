package org.open4goods.nudgerfrontapi.service;

import java.time.Duration;

import org.open4goods.commons.model.IpQuotaCategory;
import org.open4goods.commons.services.IpQuotaService;
import org.open4goods.nudgerfrontapi.config.properties.ReviewGenerationProperties;
import org.open4goods.nudgerfrontapi.dto.quota.IpQuotaStatusDto;
import org.open4goods.services.feedback.config.FeedbackConfiguration;
import org.springframework.stereotype.Service;

/**
 * Service exposing IP quota usage and limits for supported categories.
 * <p>
 * The service aggregates multiple quota sources (feedback votes, review
 * generation, etc.) and normalises them into a single DTO for the frontend.
 * </p>
 */
@Service
public class QuotaService
{
    private static final Duration DAILY_WINDOW = Duration.ofDays(1);

    private final IpQuotaService ipQuotaService;
    private final FeedbackConfiguration feedbackConfiguration;
    private final ReviewGenerationProperties reviewGenerationProperties;

    /**
     * Create a quota service using the configured quota sources.
     *
     * @param ipQuotaService quota storage service
     * @param feedbackConfiguration feedback configuration for vote limits
     * @param reviewGenerationProperties review generation quota configuration
     */
    public QuotaService(IpQuotaService ipQuotaService,
                        FeedbackConfiguration feedbackConfiguration,
                        ReviewGenerationProperties reviewGenerationProperties)
    {
        this.ipQuotaService = ipQuotaService;
        this.feedbackConfiguration = feedbackConfiguration;
        this.reviewGenerationProperties = reviewGenerationProperties;
    }

    /**
     * Resolve the current quota status for the requested category and IP.
     *
     * @param category quota category to inspect
     * @param clientIp client IP address
     * @return status describing usage and remaining actions
     */
    public IpQuotaStatusDto getQuotaStatus(IpQuotaCategory category, String clientIp)
    {
        QuotaDefinition definition = resolveDefinition(category);
        int used = definition.window() == null
                ? ipQuotaService.getUsage(definition.actionKey(), clientIp)
                : ipQuotaService.getUsage(definition.actionKey(), clientIp, definition.window());
        int remaining = definition.window() == null
                ? ipQuotaService.getRemaining(definition.actionKey(), clientIp, definition.limit())
                : ipQuotaService.getRemaining(definition.actionKey(), clientIp, definition.limit(),
                        definition.window());

        return new IpQuotaStatusDto(category, definition.limit(), used, remaining, definition.windowSeconds());
    }

    /**
     * Resolve the quota definition for the given category.
     *
     * @param category category to resolve
     * @return resolved quota definition
     */
    private QuotaDefinition resolveDefinition(IpQuotaCategory category)
    {
        return switch (category) {
            case FEEDBACK_VOTE -> new QuotaDefinition(
                    category.actionKey(),
                    feedbackConfiguration.getVoting().getMaxVotesPerIpPerDay(),
                    null,
                    DAILY_WINDOW.getSeconds());
            case REVIEW_GENERATION -> {
                ReviewGenerationProperties.Quota quota = reviewGenerationProperties.getQuota();
                yield new QuotaDefinition(
                        category.actionKey(),
                        quota.getMaxPerIp(),
                        quota.getWindow(),
                        quota.getWindow().getSeconds());
            }
            case CONTACT_MESSAGE -> new QuotaDefinition(
                    category.actionKey(),
                    0,
                    null,
                    DAILY_WINDOW.getSeconds());
        };
    }

    /**
     * Immutable quota definition resolved for a category.
     *
     * @param actionKey     key used by {@link IpQuotaService}
     * @param limit         maximum actions allowed per window
     * @param window        window duration (null for daily buckets)
     * @param windowSeconds window duration in seconds for display
     */
    private record QuotaDefinition(String actionKey, int limit, Duration window, Long windowSeconds)
    {
    }
}
