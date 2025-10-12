package org.open4goods.nudgerfrontapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.open4goods.icecat.services.IcecatService;
import org.open4goods.model.Localisable;
import org.open4goods.model.ai.AiReview;
import org.open4goods.model.exceptions.ResourceNotFoundException;
import org.open4goods.model.product.AiReviewHolder;
import org.open4goods.model.product.GtinInfo;
import org.open4goods.model.product.Product;
import org.open4goods.model.price.AggregatedPrice;
import org.open4goods.model.price.AggregatedPrices;
import org.open4goods.model.price.Currency;
import org.open4goods.model.product.ProductCondition;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.nudgerfrontapi.config.properties.ApiProperties;
import org.open4goods.nudgerfrontapi.dto.category.VerticalConfigDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductDto;
import org.open4goods.nudgerfrontapi.localization.DomainLanguage;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.verticals.VerticalsConfigService;

class ProductMappingServiceTest {

    private ProductRepository repository;
    private ProductMappingService service;
    private ApiProperties apiProperties;
    private CategoryMappingService categoryMappingService;
    private VerticalsConfigService verticalsConfigService;
    private AffiliationService affiliationService;
    private IcecatService icecatService;

    @BeforeEach
    void setUp() {
        repository = mock(ProductRepository.class);
        apiProperties = new ApiProperties();
        apiProperties.setResourceRootPath("https://static.example");
        categoryMappingService = mock(CategoryMappingService.class);
        verticalsConfigService = mock(VerticalsConfigService.class);
        affiliationService = mock(AffiliationService.class);
        icecatService = mock(IcecatService.class);
        service = new ProductMappingService(repository, apiProperties, categoryMappingService,
                verticalsConfigService, affiliationService, icecatService);
    }

    @Test
    void getProductReturnsDtoWithAiReview() throws Exception {
        long gtin = 123L;
        Product product = new Product(gtin);
        AiReviewHolder holder = new AiReviewHolder();
        holder.setReview(new AiReview());
        holder.setEnoughData(true);
        holder.setTotalTokens(10);
        holder.setCreatedMs(5L);
        holder.setSources(Map.of());
        Localisable<String, AiReviewHolder> map = new Localisable<>();
        map.put("en", holder);
        product.setReviews(map);

        when(repository.getById(gtin)).thenReturn(product);

        ProductDto dto = service.getProduct(gtin, Locale.ENGLISH, Set.of("aiReview"), DomainLanguage.en);

        assertThat(dto.aiReview()).isNotNull();
        assertThat(dto.aiReview().review()).isEqualTo(holder.getReview());
    }

    @Test
    void getProductReturnsDtoWithBase() throws Exception {
        long gtin = 321L;
        Product product = new Product(gtin);
        product.setCreationDate(1L);
        product.setLastChange(2L);
        product.setCoverImagePath("/covers/main.jpg");
        GtinInfo gtinInfo = new GtinInfo();
        gtinInfo.setCountry("FR");
        product.setGtinInfos(gtinInfo);

        when(repository.getById(gtin)).thenReturn(product);

        ProductDto dto = service.getProduct(gtin, Locale.ENGLISH, Set.of("base"), DomainLanguage.en);

        assertThat(dto.base()).isNotNull();
        assertThat(dto.base().gtin()).isEqualTo(gtin);
        assertThat(dto.base().coverImagePath()).isEqualTo("https://static.example/covers/main.jpg");
        assertThat(dto.base().gtinInfo()).isNotNull();
        assertThat(dto.base().gtinInfo().countryCode()).isEqualTo("FR");
        assertThat(dto.base().gtinInfo().countryName()).isEqualTo("France");
        assertThat(dto.base().gtinInfo().countryFlagUrl()).isEqualTo("/images/flags/fr.webp");
    }

    @Test
    void getProductComputesFullSlugWhenVerticalHomeUrlExists() throws Exception {
        long gtin = 12L;
        Product product = new Product(gtin);
        product.setVertical("electronics");
        Localisable<String, String> urlLocalisable = new Localisable<>();
        urlLocalisable.put("en", "fairphone-4");
        product.getNames().setUrl(urlLocalisable);

        when(repository.getById(gtin)).thenReturn(product);
        VerticalConfig verticalConfig = new VerticalConfig();
        verticalConfig.setId("electronics");
        VerticalConfigDto configDto = new VerticalConfigDto("electronics", true, null, null, 1,
                null, null, null, null, null, "telephones-reconditionnes");
        when(verticalsConfigService.getConfigById("electronics")).thenReturn(verticalConfig);
        when(categoryMappingService.toVerticalConfigDto(verticalConfig, DomainLanguage.en)).thenReturn(configDto);

        ProductDto dto = service.getProduct(gtin, Locale.ENGLISH, Set.of("base"), DomainLanguage.en);

        assertThat(dto.slug()).isEqualTo("fairphone-4");
        assertThat(dto.fullSlug()).isEqualTo("/telephones-reconditionnes/fairphone-4");
    }

    @Test
    void offersUrlsAreEncryptedAndConditionRenamed() throws Exception {
        long gtin = 55L;
        Product product = new Product(gtin);
        AggregatedPrice aggregatedPrice = new AggregatedPrice();
        aggregatedPrice.setDatasourceName("amazon");
        aggregatedPrice.setUrl("https://example.com/product");
        aggregatedPrice.setOfferName("Eco Phone");
        aggregatedPrice.setCompensation(0.2);
        aggregatedPrice.setProductState(ProductCondition.NEW);
        aggregatedPrice.setAffiliationToken("raw-token");
        aggregatedPrice.setPrice(199.99);
        aggregatedPrice.setCurrency(Currency.EUR);
        aggregatedPrice.setTimeStamp(123L);

        AggregatedPrices aggregatedPrices = new AggregatedPrices();
        aggregatedPrices.setOffers(Set.of(aggregatedPrice));
        aggregatedPrices.setMinPrice(aggregatedPrice);
        aggregatedPrices.setConditions(EnumSet.of(ProductCondition.NEW));
        aggregatedPrices.setTrends(null);

        product.setPrice(aggregatedPrices);
        product.setOffersCount(1);

        when(repository.getById(gtin)).thenReturn(product);
        when(affiliationService.encryptAffiliationLink("amazon", "https://example.com/product")).thenReturn("encrypted-token");

        ProductDto dto = service.getProduct(gtin, Locale.ENGLISH, Set.of("offers"), DomainLanguage.en);

        assertThat(dto.offers()).isNotNull();
        assertThat(dto.offers().bestPrice()).isNotNull();
        assertThat(dto.offers().bestPrice().url()).isEqualTo("/contrib/encrypted-token");
        assertThat(dto.offers().bestPrice().condition()).isEqualTo(ProductCondition.NEW);
        assertThat(dto.offers().bestPrice().affiliationToken()).isEqualTo("raw-token");
    }



    @Test
    void createReviewCallsRepository() throws Exception {
        long gtin = 42L;
        when(repository.getById(gtin)).thenReturn(new Product(gtin));

        service.createReview(gtin, "token", null);

        verify(repository).getById(gtin);
    }
}
