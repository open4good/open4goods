package org.open4goods.nudgerfrontapi.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.open4goods.model.attribute.ReferentielKey;
import org.open4goods.model.price.AggregatedPrices;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.ProductCondition;
import org.open4goods.model.resource.Resource;
import org.open4goods.nudgerfrontapi.config.properties.GlobalSearchProperties;
import org.open4goods.nudgerfrontapi.config.properties.OfferFreshnessProperties;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import co.elastic.clients.elasticsearch._types.query_dsl.MatchPhraseQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;

/**
 * Unit tests of the global search relevance rules (GOU-2029).
 */
class GlobalSearchRelevanceTest {

    private final GlobalSearchRelevance relevance = new GlobalSearchRelevance(
            properties(List.of("filtre", "télécommande", "chambre à air", "joint")),
            new OfferFreshnessService(new OfferFreshnessProperties()));

    @Test
    void queryNamingAnAccessoryIsDetectedWithPluralAndWithoutAccents() {
        assertThat(relevance.queryMentionsAccessory("filtre machine a cafe")).isTrue();
        assertThat(relevance.queryMentionsAccessory("filtres dyson")).isTrue();
        assertThat(relevance.queryMentionsAccessory("telecommande lg")).isTrue();
        assertThat(relevance.queryMentionsAccessory("Télécommande LG")).isTrue();
        assertThat(relevance.queryMentionsAccessory("chambre a air 10 pouces")).isTrue();
        assertThat(relevance.queryMentionsAccessory("machine a cafe")).isFalse();
        assertThat(relevance.queryMentionsAccessory("jointure")).isFalse();
        assertThat(relevance.queryMentionsAccessory("chambre froide")).isFalse();
        assertThat(relevance.queryMentionsAccessory(null)).isFalse();
    }

    @Test
    void accessoryQuerySearchesTheTermsAtTheHeadOfTheOfferName() {
        Query query = relevance.accessoryQuery("machine a cafe");

        assertThat(query).isNotNull();
        assertThat(query.isIntervals()).isTrue();
        assertThat(query.intervals().field()).isEqualTo("offerNames");
        String json = query.toString();
        assertThat(json).contains("interval.start < 3")
                .contains("\"filtre\"", "\"filtres\"", "\"telecommande\"", "\"telecommandes\"", "\"chambre a air\"",
                        "\"chambres a air\"")
                .contains("\"max_gaps\":0", "\"ordered\":true");
    }

    @Test
    void noAccessoryQueryWhenTheQueryAsksForAnAccessoryOrNoTermIsConfigured() {
        assertThat(relevance.accessoryQuery("filtre a eau")).isNull();
        GlobalSearchRelevance withoutTerms = new GlobalSearchRelevance(new GlobalSearchProperties(),
                new OfferFreshnessService(new OfferFreshnessProperties()));
        assertThat(withoutTerms.accessoryQuery("machine a cafe")).isNull();
    }

    @Test
    void realLongTailAccessoriesAreDetectedFromTheRequestedObject() {
        GlobalSearchRelevance configured = configured();

        assertThat(configured.isAccessoryName("lave-linge", "Beko moteur de lave-linge compatible")).isTrue();
        assertThat(configured.isAccessoryName("refrigerateur",
                "Thermostat refrigerateur pour réfrigérateur gorenje")).isTrue();
        assertThat(configured.isAccessoryName("televiseur", "Module T-CON pour televiseur lg")).isTrue();
        assertThat(configured.isAccessoryName("lg", "Meuble Tv Bas Blanc LG")).isTrue();
        assertThat(configured.isAccessoryName("lave-vaisselle bosch", "INSERTION POUR LAVE VAISSELLE BOSCH"))
                .isTrue();
        assertThat(configured.isAccessoryName("lave linge", "Joint de hublot pour LAVE-LINGE")).isTrue();
        assertThat(configured.isAccessoryName("lave-linge", "Moteur de la lave-linge Bosch")).isTrue();
    }

    @Test
    void realProductsStayProducts() {
        GlobalSearchRelevance configured = configured();

        assertThat(configured.isAccessoryName("aspirateur", "Aspirateur pour animaux Dyson")).isFalse();
        assertThat(configured.isAccessoryName("lave-linge", "Lave-linge pour petits espaces Candy")).isFalse();
        assertThat(configured.isAccessoryName("thermostat", "Thermostat connecté Netatmo")).isFalse();
        assertThat(configured.isAccessoryName("refrigerateur", "Réfrigérateur combiné Samsung")).isFalse();
    }

    @Test
    void noAccessoryWhenTheQueryAsksForIt() {
        GlobalSearchRelevance configured = configured();

        assertThat(configured.isAccessoryName("moteur lave-linge", "Beko moteur de lave-linge compatible")).isFalse();
        assertThat(configured.accessoryQuery("moteur lave-linge")).isNull();
        assertThat(configured.isAccessoryName("lave-linge compatible", "Lave linge compatible Bosch")).isFalse();
        assertThat(configured.isAccessoryName("pour lave-linge", "Tuyau pour lave-linge")).isFalse();
    }

    @Test
    void accessoryQueryHoldsTheObjectClauses() {
        String json = configured().accessoryQuery("lave-linge").toString();

        assertThat(json).contains("\"pour lave linge\"", "\"pour linge\"", "\"compatible\"", "\"du\"",
                "\"max_gaps\":1");
        assertThat(configured().accessoryQuery("tuyau pour lave-linge compatible").toString())
                .doesNotContain("\"pour lave linge\"", "\"compatible\"");
    }

    private static GlobalSearchRelevance configured() {
        return new GlobalSearchRelevance(
                properties(List.of("moteur", "module", "insertion", "meuble tv", "thermostat pour",
                        "thermostat réfrigérateur", "joint", "support")),
                new OfferFreshnessService(new OfferFreshnessProperties()));
    }

    @Test
    void rankAddsThePhraseBonusAndTheOffersCountToTheTextScore() {
        Query base = Query.of(q -> q.matchAll(m -> m));

        Query ranked = relevance.rank(base, "machine a cafe");

        assertThat(ranked.isFunctionScore()).isTrue();
        String json = ranked.toString();
        assertThat(json).contains("\"match_phrase\"", "\"machine a cafe\"", "\"offersCount\"", "\"log1p\"",
                "\"boost_mode\":\"sum\"");
    }

    /**
     * Measured case (prod v1.0.80): {@code sèche-linge} has no vertical and brought back washing machines matching
     * only on « linge ». The exact expression bonus ranks the offer names holding « sèche-linge » first, and the
     * query is not read as an accessory request, so real accessories stay demoted.
     */
    @Test
    void secheLingeGetsTheExactExpressionBonusAndIsNotAnAccessoryRequest() throws Exception {
        GlobalSearchProperties configured = configuredProperties();
        GlobalSearchRelevance configuredRelevance = new GlobalSearchRelevance(configured,
                new OfferFreshnessService(new OfferFreshnessProperties()));
        Query base = Query.of(q -> q.matchAll(m -> m));

        Query ranked = configuredRelevance.rank(base, "sèche-linge");

        assertThat(ranked.isFunctionScore()).isTrue();
        Query scored = ranked.functionScore().query();
        assertThat(scored.isBool()).isTrue();
        assertThat(scored.bool().must()).containsExactly(base);
        assertThat(scored.bool().should()).hasSize(1);
        MatchPhraseQuery phrase = scored.bool().should().get(0).matchPhrase();
        assertThat(phrase.field()).isEqualTo("offerNames");
        assertThat(phrase.query()).isEqualTo("sèche-linge");
        assertThat(configured.getPhraseBoost()).isPositive();
        assertThat(phrase.boost()).isEqualTo((float) configured.getPhraseBoost());
        assertThat(phrase.slop()).isEqualTo(configured.getPhraseSlop());

        assertThat(configured.getAccessoryTerms()).isNotEmpty();
        assertThat(configuredRelevance.queryMentionsAccessory("sèche-linge")).isFalse();
        assertThat(configuredRelevance.queryMentionsAccessory("seche linge")).isFalse();
        assertThat(configuredRelevance.accessoryQuery("sèche-linge")).isNotNull();
    }

    @Test
    void rankWithoutQueryOrSignalsKeepsTheBaseQuery() {
        Query base = Query.of(q -> q.matchAll(m -> m));
        GlobalSearchProperties disabled = new GlobalSearchProperties();
        disabled.setOffersCountWeight(0);
        disabled.setPhraseBoost(0);
        GlobalSearchRelevance noSignals = new GlobalSearchRelevance(disabled,
                new OfferFreshnessService(new OfferFreshnessProperties()));

        assertThat(noSignals.rank(base, "machine a cafe")).isSameAs(base);
        assertThat(relevance.rank(base, null).toString()).doesNotContain("match_phrase");
    }

    @Test
    void readableTitleNeedsThreeWordsAndAWordOtherThanTheBrand() {
        assertThat(relevance.hasReadableTitle(product(1, "SAMSUNG", "Machine à café Delonghi Magnifica"))).isTrue();
        assertThat(relevance.hasReadableTitle(product(2, "SAMSUNG", "SAMSUNG UE55AU7105"))).isFalse();
        assertThat(relevance.hasReadableTitle(product(3, "SAMSUNG", "SAMSUNG UE55 AU7105"))).isFalse();
        assertThat(relevance.hasReadableTitle(product(4, "SAMSUNG", "TV 4K"))).isFalse();
        assertThat(relevance.hasReadableTitle(product(5, "SAMSUNG", "SAMSUNG UE55AU7105", "Téléviseur Samsung 55 pouces")))
                .isTrue();
        assertThat(relevance.hasReadableTitle(new Product(6L))).isFalse();
    }

    @Test
    void imageIsACoverOrAMerchantImageThatIsNotAPlaceholder() throws Exception {
        Product cover = product(1, null, "Machine à café Delonghi");
        cover.setCoverImagePath("/images/cover.webp");
        Product merchantImage = product(2, null, "Machine à café Delonghi");
        merchantImage.setResources(Set.of(new Resource("https://cdn.example/machine.jpg")));
        Product placeholder = product(3, null, "Machine à café Delonghi");
        placeholder.setResources(Set.of(new Resource("https://images2.productserve.com/noimage.gif")));
        Product none = product(4, null, "Machine à café Delonghi");

        assertThat(relevance.hasImage(cover)).isTrue();
        assertThat(relevance.hasImage(merchantImage)).isTrue();
        assertThat(relevance.hasImage(placeholder)).isFalse();
        assertThat(relevance.hasImage(none)).isFalse();
    }

    @Test
    void displayableNeedsAFreshOffer() throws Exception {
        Product fresh = displayable(1, "Machine à café Delonghi Magnifica", System.currentTimeMillis());
        Product stale = displayable(2, "Machine à café Delonghi Magnifica",
                System.currentTimeMillis() - java.time.Duration.ofDays(20).toMillis());

        assertThat(relevance.isDisplayable(fresh)).isTrue();
        assertThat(relevance.isDisplayable(stale)).isFalse();
        assertThat(relevance.isDisplayable(null)).isFalse();
    }

    @Test
    void longTailCandidatesAreBoundedAndDrivePageReach() {
        assertThat(relevance.longTailCandidates(0, 20)).isEqualTo(60);
        assertThat(relevance.longTailCandidates(2, 20)).isEqualTo(180);
        assertThat(relevance.longTailCandidates(100, 20)).isEqualTo(1200);
        assertThat(relevance.longTailReachablePages(20)).isEqualTo(20);
        assertThat(relevance.longTailReachablePages(100)).isEqualTo(4);
    }

    /**
     * Global search properties bound from the shipped {@code application.yml}.
     */
    static GlobalSearchProperties configuredProperties() throws Exception {
        List<PropertySource<?>> sources = new YamlPropertySourceLoader().load("application.yml",
                new ClassPathResource("application.yml"));
        return new Binder(ConfigurationPropertySources.from(sources.get(0)))
                .bind("front.global-search", GlobalSearchProperties.class)
                .orElseThrow(() -> new IllegalStateException("front.global-search missing from application.yml"));
    }

    static GlobalSearchProperties properties(List<String> accessoryTerms) {
        GlobalSearchProperties properties = new GlobalSearchProperties();
        properties.setAccessoryTerms(accessoryTerms);
        return properties;
    }

    static Product product(long id, String brand, String... offerNames) {
        Product product = new Product(id);
        if (brand != null) {
            product.getAttributes().getReferentielAttributes().put(ReferentielKey.BRAND, brand);
        }
        product.setOfferNames(new java.util.LinkedHashSet<>(List.of(offerNames)));
        return product;
    }

    /**
     * Product passing the long tail filter when its offer is fresh.
     */
    static Product displayable(long id, String offerName, long offerTimestamp) throws Exception {
        Product product = product(id, "DELONGHI", offerName);
        product.setResources(Set.of(new Resource("https://cdn.example/" + id + ".jpg")));
        product.setPrice(new AggregatedPrices());
        product.getPrice().getOffers().add(OfferFreshnessServiceTest.offer("fnac", 99.0, ProductCondition.NEW,
                offerTimestamp));
        return product;
    }
}
