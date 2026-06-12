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
        BrandService brandService = serviceWithCompany();

        Brand resolved = brandService.resolve("LG Electronics Inc.");

        assertThat(resolved.getBrandName()).isEqualTo("LG");
        assertThat(resolved.getCompanyName()).isEqualTo("LG Electronics, Inc.");
    }

    @Test
    void resolvesCompanyDetailsManufacturingAndScores() throws Exception {
        BrandService brandService = serviceWithCompany();

        Brand resolved = brandService.resolve("LG");
        assertThat(resolved.getBrandName()).isEqualTo("LG");
        assertThat(resolved.getCompanyName()).isEqualTo("LG Electronics, Inc.");
        assertThat(resolved.getOfficialDomains()).containsExactly("lg.com");
        assertThat(resolved.getCompany()).isNotNull();
        assertThat(resolved.getCompany().getId()).isEqualTo("lg-electronics-inc");
        assertThat(resolved.getCompany().getManufacturing()).hasSize(1);
        assertThat(resolved.getCompany().getManufacturing().get(0).getCountry()).isEqualTo("KR");
        assertThat(resolved.getCompany().getScores()).containsKey("cdp");

        // Category-aware manufacturing lookup
        assertThat(brandService.manufacturingSites("LG", "tv")).hasSize(1);
        assertThat(brandService.manufacturingSites("LG", "smartphone")).isEmpty();
        assertThat(brandService.getCompany("lg-electronics-inc")).isPresent();
    }

    @Test
    void fallsBackToBasicCompanyOnLoaderFailure() throws Exception {
        CompanyLoader companyLoader = id -> {
            throw new RuntimeException("Simulated load error");
        };

        BrandService brandService = new BrandService(remoteFileCachingService, serialisationService,
                this::v3Referential, companyLoader);

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
        BrandService brandService = serviceWithCompany();
        Map<String, String> aliases = new HashMap<>();
        aliases.put("LG Electronics Inc.", "LG");

        Brand resolved = brandService.resolve("LG Electronics Inc.", aliases);

        assertThat(resolved.getBrandName()).isEqualTo("LG");
        assertThat(resolved.getCompanyName()).isEqualTo("LG Electronics, Inc.");
    }

    @Test
    void incrementsMissCounterForUnknownBrands() throws Exception {
        BrandService brandService = serviceWithCompany();

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
                .hasMessageContaining("v3 schema");
    }

    @Test
    void rejectsLegacyV2Referential() {
        String v2 = """
                {
                  "version": 2,
                  "brands": []
                }
                """;

        assertThatThrownBy(() -> service(v2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported brand referential version");
    }

    @Test
    void recordsIcecatEvidenceForCanonicalBrand() throws Exception {
        BrandService brandService = serviceWithCompany();

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
        BrandService brandService = serviceWithCompany();

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

    private BrandService serviceWithCompany() throws Exception {
        CompanyLoader companyLoader = id -> {
            if ("lg-electronics-inc".equals(id)) {
                return """
                        {
                          "schemaVersion": 3,
                          "id": "lg-electronics-inc",
                          "name": "LG Electronics, Inc.",
                          "manufacturing": [
                            { "categories": ["tv"], "country": "KR", "city": "Gumi",
                              "lat": 36.12, "lon": 128.34, "type": "factory",
                              "sources": [{ "url": "https://example.org", "label": "test", "retrievedAt": "2026-06-01" }] }
                          ],
                          "scores": {
                            "cdp": { "value": 7, "rating": "A-",
                                     "scale": { "min": 0, "max": 8, "higherIsBetter": true },
                                     "url": "https://cdp.example", "retrievedAt": "2026-06-01" }
                          }
                        }
                        """;
            }
            throw new IllegalArgumentException("Not found");
        };
        return new BrandService(remoteFileCachingService, serialisationService, this::v3Referential, companyLoader);
    }

    private String v3Referential() {
        return """
                {
                  "version": 3,
                  "updatedAt": "2026-06-12",
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
