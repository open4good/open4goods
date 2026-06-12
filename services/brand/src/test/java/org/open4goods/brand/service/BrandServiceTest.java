package org.open4goods.brand.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.open4goods.brand.model.Brand;
import org.open4goods.brand.model.Company;
import org.open4goods.services.remotefilecaching.service.RemoteFileCachingService;
import org.open4goods.services.serialisation.service.SerialisationService;

class BrandServiceTest {

    private final RemoteFileCachingService remoteFileCachingService = mock(RemoteFileCachingService.class);
    private final SerialisationService serialisationService = new SerialisationService();

    @Test
    void resolvesCanonicalBrandFromSynonym() throws Exception {
        BrandService brandService = service(v2Referential());

        Brand resolved = brandService.resolve("LG Electronics Inc.");

        assertThat(resolved.getBrandName()).isEqualTo("LG");
        assertThat(resolved.getCompanyName()).isEqualTo("LG Electronics, Inc.");
    }

    @Test
    void resolvesCompanyDetailsAndOfficialDomains() throws Exception {
        CompanyLoader companyLoader = id -> {
            if ("lg-electronics-inc".equals(id)) {
                return """
                        {
                          "id": "lg-electronics-inc",
                          "name": "LG Electronics, Inc.",
                          "factoryLocations": ["South Korea"],
                          "scorings": {"sustainalytics": 75}
                        }
                        """;
            }
            throw new IllegalArgumentException("Not found");
        };

        BrandService brandService = new BrandService(remoteFileCachingService, serialisationService,
                this::v2Referential, companyLoader);

        Brand resolved = brandService.resolve("LG");
        assertThat(resolved.getBrandName()).isEqualTo("LG");
        assertThat(resolved.getCompanyName()).isEqualTo("LG Electronics, Inc.");
        assertThat(resolved.getOfficialDomains()).containsExactly("lg.com");
        assertThat(resolved.getCompany()).isNotNull();
        assertThat(resolved.getCompany().getId()).isEqualTo("lg-electronics-inc");
        assertThat(resolved.getCompany().getFactoryLocations()).containsExactly("South Korea");
        assertThat(resolved.getCompany().getScorings()).containsEntry("sustainalytics", 75);
    }

    @Test
    void fallsBackToBasicCompanyOnLoaderFailure() throws Exception {
        CompanyLoader companyLoader = id -> {
            throw new RuntimeException("Simulated load error");
        };

        BrandService brandService = new BrandService(remoteFileCachingService, serialisationService,
                this::v2Referential, companyLoader);

        Brand resolved = brandService.resolve("LG");
        assertThat(resolved.getBrandName()).isEqualTo("LG");
        assertThat(resolved.getCompanyName()).isEqualTo("LG Electronics, Inc.");
        assertThat(resolved.getCompany()).isNotNull();
        assertThat(resolved.getCompany().getId()).isEqualTo("lg-electronics-inc");
        // Handled fallback should retain company name
        assertThat(resolved.getCompany().getName()).isEqualTo("LG Electronics, Inc.");
    }

    @Test
    void appliesLegacyAliasesBeforeCentralResolution() throws Exception {
        BrandService brandService = service(v2Referential());
        Map<String, String> aliases = new HashMap<>();
        aliases.put("LG Electronics Inc.", "LG");

        Brand resolved = brandService.resolve("LG Electronics Inc.", aliases);

        assertThat(resolved.getBrandName()).isEqualTo("LG");
        assertThat(resolved.getCompanyName()).isEqualTo("LG Electronics, Inc.");
    }

    @Test
    void incrementsMissCounterForUnknownBrands() throws Exception {
        BrandService brandService = service(v2Referential());

        Brand resolved = brandService.resolve("unknown maker");

        assertThat(resolved.getBrandName()).isEqualTo("UNKNOWN MAKER");
        assertThat(resolved.getCompanyName()).isNull();
        assertThat(brandService.getMissCounter()).containsEntry("UNKNOWN MAKER", 1L);
    }

    @Test
    void rejectsLegacyFlatJsonReferential() {
        String flatJson = """
                {
                  "LG": "LG Electronics, Inc.",
                  "SAMSUNG": "Samsung Electronics Co., Ltd."
                }
                """;

        assertThatThrownBy(() -> service(flatJson))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("v2 schema");
    }

    @Test
    void recordsIcecatEvidenceForCanonicalBrand() throws Exception {
        BrandService brandService = service(v2Referential());

        brandService.addSourceEvidence("LG", "icecat", "293");

        assertThat(brandService.getEvidenceByCanonicalName()).containsKey("LG");
        assertThat(brandService.getEvidenceByCanonicalName().get("LG"))
                .anySatisfy(evidence -> {
                    assertThat(evidence.getSource()).isEqualTo("icecat");
                    assertThat(evidence.getSourceId()).isEqualTo("293");
                });
    }

    @Test
    void suggestionGeneratorKeepsNoisyValuesOutOfReviewQueue() throws Exception {
        BrandService brandService = service(v2Referential());

        brandService.resolve("FOR SAMSUNG");
        brandService.resolve("OEM");
        brandService.resolve("Sans marque");
        brandService.resolve("Real Candidate");

        assertThat(brandService.generateSuggestions())
                .extracting("normalizedName")
                .containsExactly("REAL CANDIDATE");
    }

    private BrandService service(String json) throws Exception {
        return new BrandService(remoteFileCachingService, serialisationService, () -> json, id -> "{}");
    }

    private String v2Referential() {
        return """
                {
                  "version": 2,
                  "updatedAt": "2026-06-12",
                  "companyNameSource": "test",
                  "brands": [
                    {
                      "canonicalName": "LG",
                      "normalizedName": "LG",
                      "company-id": "lg-electronics-inc",
                      "companyName": "LG Electronics, Inc.",
                      "official-domains": [
                        "lg.com"
                      ],
                      "status": "reviewed",
                      "synonyms": [
                        "LG ELECTRONICS",
                        "LG ELECTRONICS INC",
                        "LG Electronics",
                        "LG Electronics Inc."
                      ],
                      "sources": [
                        {
                          "source": "icecat",
                          "sourceId": "293",
                          "rawName": "LG",
                          "count": 1
                        }
                      ]
                    }
                  ],
                  "suggestions": []
                }
                """;
    }
}
