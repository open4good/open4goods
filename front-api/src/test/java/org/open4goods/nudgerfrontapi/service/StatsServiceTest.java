package org.open4goods.nudgerfrontapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;
import org.open4goods.model.affiliation.AffiliationPartner;
import org.open4goods.nudgerfrontapi.dto.stats.ActiveMerchantDomainsDto;
import org.open4goods.nudgerfrontapi.dto.stats.CategoriesStatsDto;
import org.open4goods.nudgerfrontapi.model.stats.ActiveMerchantDomains;
import org.open4goods.model.localization.DomainLanguage;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.open4goods.nudgerfrontapi.service.opendata.OpenDataManifestService;
import org.open4goods.nudgerfrontapi.service.opendata.OpenDataManifestSnapshot;
import org.open4goods.services.opendata.model.DatasetManifest;
import org.open4goods.services.opendata.model.OpenDataManifest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;

class StatsServiceTest {

    private static final String DEFAULT_YAML = "id: default\nenabled: false\n";
    private static final String ENABLED_YAML = "id: enabled\nenabled: true\n";
    private static final String DISABLED_YAML = "id: disabled\nenabled: false\n";

    @Test
    void categoriesCountsEnabledVerticalConfigs() throws Exception {
        SerialisationService serialisationService = new SerialisationService();
        ResourcePatternResolver resolver = mock(ResourcePatternResolver.class);
        AffiliationPartnerService partnerService = mock(AffiliationPartnerService.class);
        OpenDataManifestService openDataManifestService = mock(OpenDataManifestService.class);
        ProductRepository productRepository = mock(ProductRepository.class);
        ProductMappingService productMappingService = mock(ProductMappingService.class);

        Resource defaultResource = resource("_default.yml", DEFAULT_YAML);
        Resource enabledResource = resource("enabled.yml", ENABLED_YAML);
        Resource disabledResource = resource("disabled.yml", DISABLED_YAML);

        given(resolver.getResource("classpath:/verticals/_default.yml")).willReturn(defaultResource);
        given(resolver.getResources("classpath:/verticals/*.yml"))
                .willReturn(new Resource[]{defaultResource, enabledResource, disabledResource});
        given(openDataManifestService.snapshot()).willReturn(OpenDataManifestSnapshot.NOT_CONFIGURED);
        given(productRepository.countMainIndexHavingVertical("enabled")).willReturn(42L);
        given(productRepository.countMainIndexHavingImpactScore()).willReturn(5_925L);
        given(productRepository.countMainIndexWithoutVertical()).willReturn(3_210L);
        given(productRepository.countMainIndexValidAndRated()).willReturn(9_876L);
        given(partnerService.getPartners()).willReturn(List.of(mock(AffiliationPartner.class), mock(AffiliationPartner.class)));

        MerchantDomainsService merchantDomainsService = mock(MerchantDomainsService.class);
        Instant computedAt = Instant.parse("2026-11-10T08:00:00Z");
        SortedMap<String, List<String>> domains = new TreeMap<>(Map.of(
                "darty.com", List.of("darty.com"), "carrefour.fr", List.of("Carrefour - marketplace")));
        given(merchantDomainsService.current()).willReturn(new ActiveMerchantDomains(domains, computedAt, false,
                computedAt.minusSeconds(864_000), List.of(), Map.of()));

        StatsService service = new StatsService(serialisationService, resolver, partnerService, openDataManifestService, productRepository, productMappingService, merchantDomainsService);

        CategoriesStatsDto dto = service.categories(DomainLanguage.fr);

        assertThat(dto.enabledVerticalConfigs()).isEqualTo(1);
        assertThat(dto.affiliationPartnersCount()).isEqualTo(2);
        assertThat(dto.activeMerchantDomains()).isEqualTo(new ActiveMerchantDomainsDto(2, computedAt, false));
        assertThat(dto.gtinOpenDataItemsCount()).isNull();
        assertThat(dto.isbnOpenDataItemsCount()).isNull();
        assertThat(dto.impactScoreProductsCount()).isEqualTo(5_925L);
        assertThat(dto.productsWithoutVerticalCount()).isEqualTo(3_210L);
        assertThat(dto.productsCountByCategory()).isEqualTo(Map.of("enabled", 42L));
        assertThat(dto.productsCountSum()).isEqualTo(42L);
        assertThat(dto.ratedProductsCount()).isEqualTo(9_876L);
    }

    private Resource resource(String filename, String yaml) {
        return new ByteArrayResource(yaml.getBytes()) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
    }
    @Test
    void categoriesReadOpenDataRowsFromManifest() throws Exception {
        ResourcePatternResolver resolver = mock(ResourcePatternResolver.class);
        Resource defaultResource = resource("_default.yml", DEFAULT_YAML);
        given(resolver.getResource("classpath:/verticals/_default.yml")).willReturn(defaultResource);
        given(resolver.getResources("classpath:/verticals/*.yml")).willReturn(new Resource[]{defaultResource});
        OpenDataManifestService manifestService = mock(OpenDataManifestService.class);
        given(manifestService.snapshot()).willReturn(new OpenDataManifestSnapshot(
                OpenDataManifestSnapshot.State.AVAILABLE,
                new OpenDataManifest(1, "2026-11-22T00:00:00Z", "ODbL", List.of(
                        dataset("gtin", 4_000_000L), dataset("isbn", 8_700_000L))),
                Instant.now(), null));
        MerchantDomainsService merchantDomainsService = mock(MerchantDomainsService.class);
        StatsService service = new StatsService(new SerialisationService(), resolver, mock(AffiliationPartnerService.class),
                manifestService, mock(ProductRepository.class), mock(ProductMappingService.class), merchantDomainsService);

        CategoriesStatsDto dto = service.categories(DomainLanguage.fr);

        assertThat(dto.gtinOpenDataItemsCount()).isEqualTo(4_000_000L);
        assertThat(dto.isbnOpenDataItemsCount()).isEqualTo(8_700_000L);
    }

    private static DatasetManifest dataset(String id, long rows) {
        return new DatasetManifest(1, id, id + ".zip", null, DatasetManifest.FORMAT, "ODbL", 1L, "x", rows, 0L, null, List.of(), null);
    }

    @Test
    void randomPassesVerticalId() {
        ProductRepository productRepository = mock(ProductRepository.class);
        StatsService service = new StatsService(mock(SerialisationService.class), mock(ResourcePatternResolver.class), mock(AffiliationPartnerService.class), mock(OpenDataManifestService.class), productRepository, mock(ProductMappingService.class), mock(MerchantDomainsService.class));
        
        service.random(5, 2, "my-vertical", DomainLanguage.fr);
        
        verify(productRepository).getRandomProducts(5, 2, "my-vertical");
    }
}
