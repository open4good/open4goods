package org.open4goods.nudgerfrontapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.open4goods.nudgerfrontapi.service.exception.InvalidAffiliationTokenException;
import org.open4goods.services.contribution.model.AffiliationClick;
import org.open4goods.services.contribution.model.ContributionVote;
import org.open4goods.services.contribution.repository.AffiliationClickRepository;
import org.open4goods.services.contribution.repository.ContributionVoteRepository;
import org.open4goods.services.contribution.service.ContributionService;
import org.open4goods.services.serialisation.service.SerialisationService;
import tools.jackson.databind.ObjectMapper;

/**
 * Unit tests for {@link AffiliationService}.
 */
@ExtendWith(MockitoExtension.class)
class AffiliationServiceTest {

    private static final String AWIN_URL = "https://www.awin1.com/cread.php?awinmid=1&awinaffid=2&clickref=x";

    @Mock
    private ContributionVoteRepository contributionVoteRepository;

    @Mock
    private AffiliationClickRepository affiliationClickRepository;

    private AffiliationService affiliationService;

    @BeforeEach
    void setUp() {
        affiliationService = new AffiliationService(new SerialisationService(), contributionVoteRepository,
                affiliationClickRepository);
    }

    @Test
    void encryptAndDecryptShouldRoundTripContributionVote() {
        String token = affiliationService.encryptAffiliationLink("datasource", "https://example.com");

        ContributionVote vote = affiliationService.decryptAffiliationLink(token);

        assertEquals("datasource", vote.getDatasourceName());
        assertEquals("https://example.com", vote.getUrl());
        assertThat(vote.getGtin()).isNull();
    }

    @Test
    void encryptShouldCarryTheProductGtin() {
        String token = affiliationService.encryptAffiliationLink("datasource", "https://example.com", "0123456789012");

        assertThat(affiliationService.decryptAffiliationLink(token).getGtin()).isEqualTo("0123456789012");
    }

    @Test
    void trackRedirectShouldRecordAClickWithoutPersonalData() {
        String token = affiliationService.encryptAffiliationLink("darty.com", AWIN_URL, "0123456789012");
        long before = System.currentTimeMillis();

        String url = affiliationService.trackRedirect(token, "/fours/four-encastrable");

        assertEquals(AWIN_URL, url);
        ArgumentCaptor<AffiliationClick> clickCaptor = ArgumentCaptor.forClass(AffiliationClick.class);
        verify(affiliationClickRepository).save(clickCaptor.capture());
        AffiliationClick click = clickCaptor.getValue();
        assertThat(click.getId()).isNotBlank();
        assertThat(click.getTs()).isBetween(before, System.currentTimeMillis());
        assertThat(click.getNetwork()).isEqualTo("awin");
        assertThat(click.getMerchant()).isEqualTo("darty.com");
        assertThat(click.getGtin()).isEqualTo("0123456789012");
        assertThat(click.getOriginPath()).isEqualTo("/fours/four-encastrable");
        assertThat(click.getTargetHost()).isEqualTo("www.awin1.com");

        // The serialised document holds no IP, no user agent and not the full target URL.
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(click);
        assertThat(mapper.readValue(json, java.util.Map.class).keySet())
                .containsExactlyInAnyOrder("id", "ts", "network", "merchant", "gtin", "originPath", "targetHost");
        assertThat(json).doesNotContain("cread.php", "clickref");
    }

    @Test
    void trackRedirectShouldAcceptLegacyTokensWithoutGtinNorOrigin() {
        String token = affiliationService.encryptAffiliationLink("shop", "https://shop.example/p/1");

        affiliationService.trackRedirect(token, null);

        ArgumentCaptor<AffiliationClick> clickCaptor = ArgumentCaptor.forClass(AffiliationClick.class);
        verify(affiliationClickRepository).save(clickCaptor.capture());
        assertThat(clickCaptor.getValue().getGtin()).isNull();
        assertThat(clickCaptor.getValue().getOriginPath()).isNull();
        assertThat(clickCaptor.getValue().getNetwork()).isEqualTo("other");
    }

    @Test
    void trackRedirectShouldKeepWritingTheVoteWithoutIpNorUserAgent() {
        // A forged token may carry ip/ua: they must never be stored.
        ContributionVote forged = new ContributionVote("datasource", "https://example.com");
        forged.setIp("203.0.113.5");
        forged.setUa("Mozilla/5.0");
        String token = encode(forged);
        ContributionVote original = affiliationService.decryptAffiliationLink(token);

        String url = affiliationService.trackRedirect(token, null);

        ArgumentCaptor<ContributionVote> voteCaptor = ArgumentCaptor.forClass(ContributionVote.class);
        verify(contributionVoteRepository).save(voteCaptor.capture());
        ContributionVote saved = voteCaptor.getValue();
        assertThat(saved.getIp()).isNull();
        assertThat(saved.getUa()).isNull();
        assertEquals(ContributionService.DEFAULT_VOTE, saved.getVote());
        assertNotEquals(original.getId(), saved.getId());
        assertEquals(original.getUrl(), url);
    }

    @Test
    void decryptShouldThrowWhenTokenIsInvalid() {
        assertThrows(InvalidAffiliationTokenException.class,
                () -> affiliationService.decryptAffiliationLink("not-a-valid-token"));
    }

    @Test
    void trackRedirectShouldFailOpenWhenPersistenceThrows() {
        String token = affiliationService.encryptAffiliationLink("datasource", "https://example.com");
        doThrow(new RuntimeException("Elasticsearch unavailable")).when(affiliationClickRepository).save(any());
        doThrow(new RuntimeException("Elasticsearch unavailable")).when(contributionVoteRepository).save(any());

        String url = affiliationService.trackRedirect(token, "/fours/foo");

        assertEquals("https://example.com", url);
    }

    @Test
    void trackRedirectShouldStillWriteTheVoteWhenTheClickFails() {
        String token = affiliationService.encryptAffiliationLink("datasource", "https://example.com");
        doThrow(new RuntimeException("Elasticsearch unavailable")).when(affiliationClickRepository).save(any());

        affiliationService.trackRedirect(token, null);

        verify(contributionVoteRepository).save(any(ContributionVote.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/", "/fours/four-encastrable", "/fr-FR/fours/foo-0123456789012", "/a%20b"})
    void sanitiseOriginPathShouldKeepBarePaths(String path) {
        assertThat(AffiliationService.sanitiseOriginPath(path)).isEqualTo(path);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"fours/foo", "https://nudger.fr/fours/foo", "//evil.example/x", "/fours/foo?utm=1",
            "/fours/foo#offers", "/fours foo", "/fours/\tfoo", "/fours/\u0000foo"})
    void sanitiseOriginPathShouldIgnoreAnythingElse(String path) {
        assertThat(AffiliationService.sanitiseOriginPath(path)).isNull();
    }

    @Test
    void sanitiseOriginPathShouldCapTheLength() {
        String max = "/" + "a".repeat(AffiliationService.MAX_ORIGIN_PATH_LENGTH - 1);

        assertThat(AffiliationService.sanitiseOriginPath(max)).isEqualTo(max);
        assertThat(AffiliationService.sanitiseOriginPath(max + "a")).isNull();
    }

    private static String encode(ContributionVote vote) {
        try {
            SerialisationService serialisation = new SerialisationService();
            return java.net.URLEncoder.encode(serialisation.compressString(serialisation.toJson(vote)),
                    java.nio.charset.Charset.defaultCharset());
        }
        catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
