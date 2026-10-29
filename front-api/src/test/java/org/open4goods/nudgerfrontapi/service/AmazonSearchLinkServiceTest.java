package org.open4goods.nudgerfrontapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.open4goods.nudgerfrontapi.config.properties.AmazonSearchLinkProperties;
import org.open4goods.services.contribution.model.AffiliationClick;
import org.open4goods.services.contribution.model.ContributionVote;
import org.open4goods.services.contribution.repository.AffiliationClickRepository;
import org.open4goods.services.contribution.repository.ContributionVoteRepository;
import org.open4goods.services.serialisation.service.SerialisationService;

/**
 * Unit tests for {@link AmazonSearchLinkService}, with a real {@link AffiliationService} so the {@code /contrib/}
 * token is decoded and tracked as the Nuxt route would do it.
 */
class AmazonSearchLinkServiceTest {

    private static final String VALID_EAN = "4006381333931";

    private AmazonSearchLinkProperties properties;
    private AffiliationClickRepository affiliationClickRepository;
    private AffiliationService affiliationService;
    private AmazonSearchLinkService service;

    @BeforeEach
    void setUp() {
        properties = new AmazonSearchLinkProperties();
        affiliationClickRepository = mock(AffiliationClickRepository.class);
        affiliationService = new AffiliationService(new SerialisationService(), mock(ContributionVoteRepository.class),
                affiliationClickRepository);
        service = new AmazonSearchLinkService(properties, affiliationService);
    }

    @Test
    void buildsAContribLinkTowardsTheAmazonSearchByEanWithTheTag() {
        String link = service.buildContribLink(VALID_EAN, false);

        assertThat(link).startsWith(AffiliationPartnerService.CONTRIB_ENDPOINT);
        ContributionVote vote = affiliationService.decryptAffiliationLink(
                link.substring(AffiliationPartnerService.CONTRIB_ENDPOINT.length()));
        assertThat(vote.getUrl()).isEqualTo("https://www.amazon.fr/s?k=4006381333931&tag=nudger-21");
        assertThat(vote.getDatasourceName()).isEqualTo("amazon.fr");
        assertThat(vote.getGtin()).isEqualTo(VALID_EAN);
    }

    @Test
    void followingTheLinkRecordsAnAffiliationClick() {
        String link = service.buildContribLink(VALID_EAN, false);

        String target = affiliationService.trackRedirect(
                link.substring(AffiliationPartnerService.CONTRIB_ENDPOINT.length()), "/fours/four-x");

        assertThat(target).isEqualTo("https://www.amazon.fr/s?k=4006381333931&tag=nudger-21");
        ArgumentCaptor<AffiliationClick> captor = ArgumentCaptor.forClass(AffiliationClick.class);
        verify(affiliationClickRepository).save(captor.capture());
        assertThat(captor.getValue().getMerchant()).isEqualTo("amazon.fr");
        assertThat(captor.getValue().getTargetHost()).isEqualTo("www.amazon.fr");
        assertThat(captor.getValue().getGtin()).isEqualTo(VALID_EAN);
    }

    @Test
    void normalisesTheGtinToItsEanForm() {
        assertThat(AmazonSearchLinkService.validEan("036000291452")).isEqualTo("0036000291452");
        assertThat(AmazonSearchLinkService.validEan("96385074")).isEqualTo("96385074");
        assertThat(AmazonSearchLinkService.validEan("0000096385074")).isEqualTo("96385074");
        assertThat(AmazonSearchLinkService.validEan(" 4006381333931 ")).isEqualTo(VALID_EAN);
        assertThat(AmazonSearchLinkService.validEan("9780306406157")).isEqualTo("9780306406157");
    }

    @Test
    void noLinkWhenTheProductHasOffers() {
        assertThat(service.buildContribLink(VALID_EAN, true)).isNull();
    }

    @Test
    void noLinkWhenDisabled() {
        properties.setEnabled(false);

        assertThat(service.buildContribLink(VALID_EAN, false)).isNull();
    }

    @Test
    void noLinkWithoutTag() {
        properties.setTag(" ");

        assertThat(service.buildContribLink(VALID_EAN, false)).isNull();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { " ", "-1", "4006381333932", "40063813339AB", "123", "0000000000123", "0000000000000", "00000000000000000", "0000000000000x", "4006381333931&x=1" })
    void noLinkForAnInvalidGtin(String gtin) {
        assertThat(service.buildContribLink(gtin, false)).isNull();
    }
}
