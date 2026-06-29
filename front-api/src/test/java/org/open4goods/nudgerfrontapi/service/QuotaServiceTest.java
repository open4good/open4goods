package org.open4goods.nudgerfrontapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.open4goods.commons.model.IpQuotaCategory;
import org.open4goods.commons.services.IpQuotaService;
import org.open4goods.nudgerfrontapi.dto.quota.IpQuotaStatusDto;
import org.open4goods.services.feedback.config.FeedbackConfiguration;

@ExtendWith(MockitoExtension.class)
class QuotaServiceTest
{
    private static final String CLIENT_IP = "127.0.0.1";

    @Mock
    private IpQuotaService ipQuotaService;

    private FeedbackConfiguration feedbackConfiguration;
    private QuotaService quotaService;

    @BeforeEach
    void setUp()
    {
        feedbackConfiguration = new FeedbackConfiguration();
        feedbackConfiguration.getVoting().setMaxVotesPerIpPerDay(5);

        quotaService = new QuotaService(ipQuotaService, feedbackConfiguration);
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

}
