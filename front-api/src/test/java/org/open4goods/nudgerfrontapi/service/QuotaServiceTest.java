package org.open4goods.nudgerfrontapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.open4goods.commons.model.IpQuotaCategory;
import org.open4goods.commons.services.IpQuotaService;
import org.open4goods.nudgerfrontapi.config.properties.ReviewGenerationProperties;
import org.open4goods.nudgerfrontapi.dto.quota.IpQuotaStatusDto;
import org.open4goods.services.feedback.config.FeedbackConfiguration;

@ExtendWith(MockitoExtension.class)
class QuotaServiceTest
{
    private static final String CLIENT_IP = "127.0.0.1";

    @Mock
    private IpQuotaService ipQuotaService;

    private FeedbackConfiguration feedbackConfiguration;
    private ReviewGenerationProperties reviewGenerationProperties;
    private QuotaService quotaService;

    @BeforeEach
    void setUp()
    {
        feedbackConfiguration = new FeedbackConfiguration();
        feedbackConfiguration.getVoting().setMaxVotesPerIpPerDay(5);

        reviewGenerationProperties = new ReviewGenerationProperties();
        reviewGenerationProperties.getQuota().setMaxPerIp(3);
        reviewGenerationProperties.getQuota().setWindow(Duration.ofHours(12));

        quotaService = new QuotaService(ipQuotaService, feedbackConfiguration, reviewGenerationProperties);
    }

    @Test
    void shouldReturnFeedbackVoteQuotaStatus()
    {
        when(ipQuotaService.getUsage(IpQuotaCategory.FEEDBACK_VOTE.actionKey(), CLIENT_IP)).thenReturn(2);
        when(ipQuotaService.getRemaining(IpQuotaCategory.FEEDBACK_VOTE.actionKey(), CLIENT_IP, 5)).thenReturn(3);

        IpQuotaStatusDto status = quotaService.getQuotaStatus(IpQuotaCategory.FEEDBACK_VOTE, CLIENT_IP);

        assertThat(status.category()).isEqualTo(IpQuotaCategory.FEEDBACK_VOTE);
        assertThat(status.used()).isEqualTo(2);
        assertThat(status.remaining()).isEqualTo(3);
        assertThat(status.limit()).isEqualTo(5);
        assertThat(status.windowSeconds()).isEqualTo(Duration.ofDays(1).getSeconds());
    }

    @Test
    void shouldReturnReviewGenerationQuotaStatus()
    {
        Duration window = reviewGenerationProperties.getQuota().getWindow();
        when(ipQuotaService.getUsage(IpQuotaCategory.REVIEW_GENERATION.actionKey(), CLIENT_IP, window)).thenReturn(1);
        when(ipQuotaService.getRemaining(IpQuotaCategory.REVIEW_GENERATION.actionKey(), CLIENT_IP, 3, window)).thenReturn(2);

        IpQuotaStatusDto status = quotaService.getQuotaStatus(IpQuotaCategory.REVIEW_GENERATION, CLIENT_IP);

        verify(ipQuotaService).getUsage(IpQuotaCategory.REVIEW_GENERATION.actionKey(), CLIENT_IP, window);
        verify(ipQuotaService).getRemaining(IpQuotaCategory.REVIEW_GENERATION.actionKey(), CLIENT_IP, 3, window);
        assertThat(status.category()).isEqualTo(IpQuotaCategory.REVIEW_GENERATION);
        assertThat(status.used()).isEqualTo(1);
        assertThat(status.remaining()).isEqualTo(2);
        assertThat(status.limit()).isEqualTo(3);
        assertThat(status.windowSeconds()).isEqualTo(window.getSeconds());
    }
}
