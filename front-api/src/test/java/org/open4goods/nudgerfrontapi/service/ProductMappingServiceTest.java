package org.open4goods.nudgerfrontapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.open4goods.icecat.services.IcecatService;
import org.open4goods.model.Localisable;
import org.open4goods.model.constants.CacheConstants;
import org.open4goods.model.price.AggregatedPrice;
import org.open4goods.model.price.AggregatedPrices;
import org.open4goods.model.price.Currency;
import org.open4goods.model.product.EcoScoreRanking;
import org.open4goods.model.product.GtinInfo;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.ProductCondition;
import org.open4goods.model.product.ProductTexts;
import org.open4goods.model.product.Score;
import org.open4goods.model.resource.ImageInfo;
import org.open4goods.model.resource.PdfInfo;
import org.open4goods.model.resource.Resource;
import org.open4goods.model.resource.ResourceType;
import org.open4goods.model.review.ReviewGenerationStatus;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.nudgerfrontapi.config.properties.ApiProperties;
import org.open4goods.nudgerfrontapi.dto.category.VerticalConfigDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductScoreDto;
import org.open4goods.nudgerfrontapi.localization.DomainLanguage;
import org.open4goods.services.captcha.service.HcaptchaService;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.verticals.VerticalsConfigService;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;

import jakarta.servlet.http.HttpServletRequest;


class ProductMappingServiceTest {

    private ProductRepository repository;
    private ProductMappingService service;
    private ApiProperties apiProperties;
    private CategoryMappingService categoryMappingService;
    private VerticalsConfigService verticalsConfigService;
    private AffiliationService affiliationService;
    private IcecatService icecatService;
    private SearchService searchService;
    private CacheManager cacheManager;
    private ReviewGenerationClient reviewGenerationClient;
    private HcaptchaService hcaptchaService;
    private ProductTimelineService productTimelineService;
    private HttpServletRequest httpServletRequest;

    @BeforeEach
    void setUp() {
        repository = mock(ProductRepository.class);
        apiProperties = new ApiProperties();
        apiProperties.setResourceRootPath("https://static.example");
        categoryMappingService = mock(CategoryMappingService.class);
        verticalsConfigService = mock(VerticalsConfigService.class);
        affiliationService = mock(AffiliationService.class);
        icecatService = mock(IcecatService.class);
        searchService = mock(SearchService.class);
        cacheManager = mock(CacheManager.class);
        reviewGenerationClient = mock(ReviewGenerationClient.class);
        hcaptchaService = mock(HcaptchaService.class);
        productTimelineService = new ProductTimelineService();
        httpServletRequest = mock(HttpServletRequest.class);
        ConcurrentMapCache referenceCache = new ConcurrentMapCache(CacheConstants.ONE_HOUR_LOCAL_CACHE_NAME);
        when(cacheManager.getCache(CacheConstants.ONE_HOUR_LOCAL_CACHE_NAME)).thenReturn(referenceCache);
        service = new ProductMappingService(repository, apiProperties, categoryMappingService,
                verticalsConfigService, searchService, affiliationService, icecatService, cacheManager,
                reviewGenerationClient, hcaptchaService, productTimelineService);
    }

    @Test
    void getProductReturnsDtoWithBase() throws Exception {
        long gtin = 321L;
        Product product = new Product(gtin);
        product.setCreationDate(1L);
        product.setLastChange(2L);
        GtinInfo gtinInfo = new GtinInfo();
        gtinInfo.setCountry("FR");
        product.setGtinInfos(gtinInfo);
        product.setCoverImagePath("/covers/main.jpg");
        product.setOfferNames(Set.of("Eco Phone"));
        Score ecoscore = new Score("ECOSCORE", 12.5);
        HashMap<String, Score> scores = new HashMap<>();
        scores.put("ECOSCORE", ecoscore);
        product.setScores(scores);

        when(repository.getById(gtin)).thenReturn(product);

        ProductDto dto = service.getProduct(gtin, Locale.ENGLISH, Set.of("base"), DomainLanguage.en);

        assertThat(dto.base()).isNotNull();
        assertThat(dto.base().gtin()).isEqualTo(gtin);
        assertThat(dto.base().gtinInfo()).isNotNull();
        assertThat(dto.base().gtinInfo().countryCode()).isEqualTo("FR");
        assertThat(dto.base().gtinInfo().countryName()).isEqualTo("France");
        assertThat(dto.base().gtinInfo().countryFlagUrl()).isEqualTo("/images/flags/fr.webp");
        assertThat(dto.base().coverImagePath()).isEqualTo("https://static.example/covers/main.jpg");
        assertThat(dto.base().bestName()).isEqualTo("Eco Phone");
        assertThat(dto.base().ecoscoreValue()).isEqualTo(12.5);
        assertThat(dto.resources()).isNull();
    }

    @Test
    void scoresIncludeReferencedProducts() throws Exception {
        long gtin = 555L;
        Product product = new Product(gtin);
        product.setVertical("phones");

        Score score = new Score("ENERGY", 10.0);
        HashMap<String, Score> scores = new HashMap<>();
        scores.put("ENERGY", score);
        product.setScores(scores);

        EcoScoreRanking ranking = new EcoScoreRanking();
        ranking.setGlobalBest(111L);
        ranking.setGlobalBetter(222L);
        product.setRanking(ranking);

        when(repository.getById(gtin)).thenReturn(product);

        Product lowest = new Product(111L);
        lowest.setVertical("phones");
        ProductTexts lowestTexts = new ProductTexts();
        Localisable<String, String> lowestUrl = new Localisable<>();
        lowestUrl.put("en", "lowest");
        lowestTexts.setUrl(lowestUrl);
        lowest.setNames(lowestTexts);
        lowest.setOfferNames(Set.of("Lowest offer"));

        Product highest = new Product(222L);
        highest.setVertical("phones");
        ProductTexts highestTexts = new ProductTexts();
        Localisable<String, String> highestUrl = new Localisable<>();
        highestUrl.put("en", "highest");
        highestTexts.setUrl(highestUrl);
        highest.setNames(highestTexts);
        highest.setOfferNames(Set.of("Highest offer"));

        Map<String, Product> referenced = new HashMap<>();
        referenced.put("111", lowest);
        referenced.put("222", highest);
        when(repository.multiGetById(anyCollection())).thenReturn(referenced);

        VerticalConfig verticalConfig = new VerticalConfig();
        verticalConfig.setId("phones");
        when(verticalsConfigService.getConfigById("phones")).thenReturn(verticalConfig);
        when(verticalsConfigService.getConfigByIdOrDefault("phones")).thenReturn(verticalConfig);

        VerticalConfigDto verticalDto = new VerticalConfigDto("phones", true, true, null, null, null, null, null, null,
                "Phones", "Phones description", "phones", List.of());
        when(categoryMappingService.toVerticalConfigDto(verticalConfig, DomainLanguage.en)).thenReturn(verticalDto);

        ProductDto dto = service.getProduct(gtin, Locale.ENGLISH, Set.of("scores"), DomainLanguage.en);

        assertThat(dto.scores()).isNotNull();
        ProductScoreDto mappedScore = dto.scores().scores().get("ENERGY");
        assertThat(mappedScore).isNotNull();
        assertThat(dto.scores().ranking()).isNotNull();
        assertThat(dto.scores().ranking().globalBest()).isNotNull();
        assertThat(dto.scores().ranking().globalBest().id()).isEqualTo(111L);
        assertThat(dto.scores().ranking().globalBest().fullSlug()).isEqualTo("/phones/lowest");
    }

    @Test
    void resourcesFacetContainsCoverImagePathAndBuiltUrls() throws Exception {
        long gtin = 987L;
        Product product = new Product(gtin);
        product.setCoverImagePath("/covers/main.jpg");

        Resource image = new Resource();
        image.setResourceType(ResourceType.IMAGE);
        image.setFileName("main-image");
        image.setCacheKey("img123");
        image.setExtension("jpg");
        image.setUrl("https://legacy.example/images/main.jpg");
        ImageInfo imageInfo = new ImageInfo();
        imageInfo.setHeight(800);
        imageInfo.setWidth(600);
        image.setImageInfo(imageInfo);

        Resource pdf = new Resource();
        pdf.setResourceType(ResourceType.PDF);
        pdf.setFileName("manual");
        pdf.setCacheKey("pdf456");
        pdf.setExtension("pdf");
        pdf.setUrl("https://legacy.example/pdfs/manual.pdf");
        PdfInfo pdfInfo = new PdfInfo();
        pdfInfo.setMetadataTitle("User manual");
        pdfInfo.setExtractedTitle("Manual extracted");
        pdfInfo.setNumberOfPages(42);
        pdfInfo.setAuthor("Example Corp");
        pdfInfo.setLanguage("en");
        pdfInfo.setLanguageConfidence(0.95);
        pdf.setPdfInfo(pdfInfo);

        Resource video = new Resource();
        video.setResourceType(ResourceType.VIDEO);
        video.setFileName("teaser");
        video.setCacheKey("vid789");
        video.setExtension("mp4");
        video.setUrl("https://legacy.example/videos/teaser.mp4");

        product.setResources(new HashSet<>(Set.of(image, pdf, video)));

        when(repository.getById(gtin)).thenReturn(product);

        ProductDto dto = service.getProduct(gtin, Locale.ENGLISH, Set.of("resources"), DomainLanguage.en);

        assertThat(dto.resources()).isNotNull();
        assertThat(dto.resources().coverImagePath()).isEqualTo("https://static.example/covers/main.jpg");
        assertThat(dto.resources().externalCover()).isEqualTo("https://legacy.example/images/main.jpg");
        assertThat(dto.resources().images()).hasSize(1);
        assertThat(dto.resources().videos()).hasSize(1);
        assertThat(dto.resources().pdfs()).hasSize(1);

        assertThat(dto.resources().images().get(0).url()).isEqualTo("https://static.example/images/main-image_img123.webp");
        assertThat(dto.resources().images().get(0).height()).isEqualTo(800);
        assertThat(dto.resources().images().get(0).width()).isEqualTo(600);

        assertThat(dto.resources().pdfs().get(0).url()).isEqualTo("https://static.example/pdfs/manual_pdf456.pdf");
        assertThat(dto.resources().pdfs().get(0).metadataTitle()).isEqualTo("User manual");
        assertThat(dto.resources().pdfs().get(0).numberOfPages()).isEqualTo(42);
        assertThat(dto.resources().pdfs().get(0).language()).isEqualTo("en");
        assertThat(dto.resources().pdfs().get(0).languageConfidence()).isEqualTo(0.95);

        assertThat(dto.resources().videos().get(0).url()).isEqualTo("https://static.example/videos/teaser_vid789.mp4");
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
        VerticalConfigDto configDto = new VerticalConfigDto("electronics", true, false, null, null, 1,
                null, null, null, null, null, "telephones-reconditionnes", List.of());
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
    void createReviewVerifiesCaptchaAndDelegatesToClient() throws Exception {
        long gtin = 42L;
        when(httpServletRequest.getHeader("X-Real-Ip")).thenReturn(null);
        when(httpServletRequest.getHeader("X-Forwarded-For")).thenReturn(null);
        when(httpServletRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        Product product = new Product(gtin);
        when(repository.getById(gtin)).thenReturn(product);
        when(reviewGenerationClient.triggerGeneration(gtin)).thenReturn(gtin);

        long scheduled = service.createReview(gtin, "token", httpServletRequest);

        verify(hcaptchaService).verifyRecaptcha("127.0.0.1", "token");
        verify(repository).getById(gtin);
        verify(reviewGenerationClient).triggerGeneration(gtin);
        assertThat(scheduled).isEqualTo(gtin);
    }

    @Test
    void getReviewStatusDelegatesToClient() {
        long gtin = 77L;
        ReviewGenerationStatus status = new ReviewGenerationStatus();
        status.setUpc(gtin);
        when(reviewGenerationClient.getStatus(gtin)).thenReturn(status);

        ReviewGenerationStatus returned = service.getReviewStatus(gtin);

        assertThat(returned).isEqualTo(status);
    }
}
