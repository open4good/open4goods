package org.open4goods.nudgerfrontapi.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.open4goods.nudgerfrontapi.service.exception.InvalidAffiliationTokenException;
import org.open4goods.services.contribution.model.ContributionVote;
import org.open4goods.services.contribution.repository.ContributionVoteRepository;
import org.open4goods.services.contribution.service.ContributionService;
import org.open4goods.services.serialisation.service.SerialisationService;

/**
 * Unit tests for {@link AffiliationService}.
 */
@ExtendWith(MockitoExtension.class)
class AffiliationServiceTest {

    @Mock
    private ContributionVoteRepository contributionVoteRepository;

    private AffiliationService affiliationService;

    @BeforeEach
    void setUp() {
        affiliationService = new AffiliationService(new SerialisationService(), contributionVoteRepository);
    }

    @Test
    void encryptAndDecryptShouldRoundTripContributionVote() {
        String token = affiliationService.encryptAffiliationLink("datasource", "https://example.com");

        ContributionVote vote = affiliationService.decryptAffiliationLink(token);

        assertEquals("datasource", vote.getDatasourceName());
        assertEquals("https://example.com", vote.getUrl());
    }

    @Test
    void trackRedirectShouldPersistVoteWithIpAndTruncatedUserAgent() {
        String token = affiliationService.encryptAffiliationLink("datasource", "https://example.com");
        ContributionVote original = affiliationService.decryptAffiliationLink(token);
        when(contributionVoteRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        String longUserAgent = "Mozilla/5.0" + "a".repeat(120);
        String url = affiliationService.trackRedirect(token, "203.0.113.5", longUserAgent);

        ArgumentCaptor<ContributionVote> voteCaptor = ArgumentCaptor.forClass(ContributionVote.class);
        verify(contributionVoteRepository).save(voteCaptor.capture());

        ContributionVote saved = voteCaptor.getValue();
        assertEquals("203.0.113.5", saved.getIp());
        assertEquals(100, saved.getUa().length());
        assertEquals(ContributionService.DEFAULT_VOTE, saved.getVote());
        assertNotEquals(original.getId(), saved.getId());
        assertEquals(original.getUrl(), url);
    }

    @Test
    void decryptShouldThrowWhenTokenIsInvalid() {
        assertThrows(InvalidAffiliationTokenException.class,
                () -> affiliationService.decryptAffiliationLink("not-a-valid-token"));
    }
}
