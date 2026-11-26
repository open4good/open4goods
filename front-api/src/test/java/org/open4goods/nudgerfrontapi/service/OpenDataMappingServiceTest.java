package org.open4goods.nudgerfrontapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.text.NumberFormat;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.open4goods.model.localization.DomainLanguage;
import org.open4goods.nudgerfrontapi.dto.opendata.OpenDataDatasetDto;
import org.open4goods.nudgerfrontapi.dto.opendata.OpenDataOverviewDto;
import org.open4goods.nudgerfrontapi.service.opendata.OpenDataManifestService;
import org.open4goods.nudgerfrontapi.service.opendata.OpenDataManifestSnapshot;
import org.open4goods.nudgerfrontapi.service.opendata.OpenDataSampleService;
import org.open4goods.nudgerfrontapi.service.opendata.OpenDataManifestSnapshot.State;
import org.open4goods.services.opendata.config.OpenDataConfig;
import org.open4goods.services.opendata.model.ColumnManifest;
import org.open4goods.services.opendata.model.DatasetManifest;
import org.open4goods.services.opendata.model.OpenDataManifest;
import org.open4goods.services.opendata.service.OpenDataService;

/**
 * {@link OpenDataMappingService} reads rows, date, size and sha256 from the published manifest (GOU-2290), never the
 * live index, and serves nulls when the manifest is not configured or not readable.
 */
class OpenDataMappingServiceTest {

    private static final String GENERATED_AT = "2026-11-22T02:55:00Z";
    private static final String SHA256 = "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08";

    private OpenDataManifestService manifestService;
    private OpenDataConfig openDataConfig;
    private OpenDataMappingService service;

    private OpenDataSampleService sampleService;

    @BeforeEach
    void setUp() {
        manifestService = mock(OpenDataManifestService.class);
        openDataConfig = new OpenDataConfig();
        openDataConfig.setPublicBaseUrl("https://data.nudger.fr/");
        sampleService = mock(OpenDataSampleService.class);
        given(sampleService.rows(org.mockito.ArgumentMatchers.any())).willReturn(List.of());
        service = new OpenDataMappingService(manifestService, openDataConfig, sampleService);
    }

    @Test
    void datasetValuesComeFromTheManifest() {
        given(manifestService.snapshot()).willReturn(available(
                dataset("gtin", 115_786_654L, 6_400_000_000L, SHA256, GENERATED_AT),
                dataset("isbn", 8_703_397L, 210_000_000L, "abc", GENERATED_AT)));

        OpenDataDatasetDto gtin = service.gtin(DomainLanguage.en);
        OpenDataDatasetDto isbn = service.isbn(DomainLanguage.fr);

        Locale en = Locale.forLanguageTag(DomainLanguage.en.languageTag());
        assertThat(gtin.type()).isEqualTo("GTIN");
        assertThat(gtin.recordCount()).isEqualTo(NumberFormat.getIntegerInstance(en).format(115_786_654L));
        assertThat(gtin.recordCountValue()).isEqualTo(115_786_654L);
        assertThat(gtin.lastUpdated()).isEqualTo(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                .withLocale(en).withZone(OpenDataMappingService.DISPLAY_ZONE).format(Instant.parse(GENERATED_AT)));
        assertThat(gtin.fileSize()).isEqualTo("6.4 GB");
        assertThat(gtin.sha256()).isEqualTo(SHA256);
        assertThat(gtin.downloadUrl()).isEqualTo("https://data.nudger.fr/gtin-open-data.zip");
        assertThat(gtin.headers()).containsExactly(OpenDataService.GTIN_HEADER);

        assertThat(isbn.recordCountValue()).isEqualTo(8_703_397L);
        assertThat(isbn.sha256()).isEqualTo("abc");
        assertThat(isbn.downloadUrl()).isEqualTo("https://data.nudger.fr/isbn-open-data.zip");
        assertThat(isbn.headers()).containsExactly(OpenDataService.ISBN_HEADER);
    }

    @Test
    void overviewSumsGtinAndIsbnOnly() {
        given(manifestService.snapshot()).willReturn(available(
                dataset("gtin", 1_000L, 2_000_000L, SHA256, GENERATED_AT),
                dataset("isbn", 500L, 1_000_000L, "abc", GENERATED_AT),
                dataset("impact-tv", 300L, 9_000_000L, "def", GENERATED_AT)));

        OpenDataOverviewDto overview = service.overview(DomainLanguage.en);

        assertThat(overview.totalProductCount()).isEqualTo(NumberFormat.getIntegerInstance(Locale.ENGLISH).format(1_500L));
        assertThat(overview.datasetCount()).isEqualTo("2");
        assertThat(overview.totalDatasetSize()).isEqualTo("3.0 MB");
    }

    @Test
    void overviewHasNoDownloadLimitsAnyMore() {
        assertThat(Arrays.stream(OpenDataOverviewDto.class.getRecordComponents()).map(component -> component.getName()))
                .containsExactly("totalProductCount", "datasetCount", "totalDatasetSize");
        assertThat(Arrays.stream(OpenDataDatasetDto.class.getRecordComponents()).map(component -> component.getName()))
                .containsExactly("type", "recordCount", "recordCountValue", "lastUpdated", "fileSize", "sha256",
                        "downloadUrl", "headers", "id", "generatedAt", "sizeBytes", "license", "columns", "sampleRows");
    }

    @Test
    void notConfiguredGivesNullValuesAndNoLink() {
        openDataConfig.setPublicBaseUrl("");
        given(manifestService.snapshot()).willReturn(OpenDataManifestSnapshot.NOT_CONFIGURED);

        OpenDataDatasetDto gtin = service.gtin(DomainLanguage.fr);
        OpenDataOverviewDto overview = service.overview(DomainLanguage.fr);

        assertThat(gtin.recordCount()).isNull();
        assertThat(gtin.recordCountValue()).isNull();
        assertThat(gtin.lastUpdated()).isNull();
        assertThat(gtin.fileSize()).isNull();
        assertThat(gtin.sha256()).isNull();
        assertThat(gtin.downloadUrl()).isNull();
        assertThat(gtin.headers()).containsExactly(OpenDataService.GTIN_HEADER);
        assertThat(overview.totalProductCount()).isNull();
        assertThat(overview.datasetCount()).isNull();
        assertThat(overview.totalDatasetSize()).isNull();
    }

    @Test
    void unreadableManifestGivesNullValuesButKeepsTheLink() {
        given(manifestService.snapshot()).willReturn(
                new OpenDataManifestSnapshot(State.UNAVAILABLE, null, null, "HTTP 404"));

        OpenDataDatasetDto isbn = service.isbn(DomainLanguage.en);

        assertThat(isbn.recordCount()).isNull();
        assertThat(isbn.lastUpdated()).isNull();
        assertThat(isbn.fileSize()).isNull();
        assertThat(isbn.downloadUrl()).isEqualTo("https://data.nudger.fr/isbn-open-data.zip");
        assertThat(service.overview(DomainLanguage.en).totalProductCount()).isNull();
    }

    @Test
    void invalidDateAndZeroSizeAreNullNeverEpochOrZeroBytes() {
        given(manifestService.snapshot()).willReturn(available(dataset("gtin", 10L, 0L, "", "not-a-date")));

        OpenDataDatasetDto gtin = service.gtin(DomainLanguage.fr);

        assertThat(gtin.lastUpdated()).isNull();
        assertThat(gtin.fileSize()).isNull();
        assertThat(gtin.sha256()).isNull();
        assertThat(gtin.recordCountValue()).isEqualTo(10L);
        assertThat(service.overview(DomainLanguage.fr).totalDatasetSize()).isNull();
    }

    @Test
    void datasetExposesIsoDateSizeLicenseAndColumnsFromTheManifest() {
        DatasetManifest withColumns = new DatasetManifest(1, "gtin", "gtin-open-data.zip", null,
                DatasetManifest.FORMAT, "ODbL-1.0", 6_400_000_000L, SHA256, 10, 10, GENERATED_AT,
                List.of(new ColumnManifest("gtin", 1.0),
                        new ColumnManifest("brand", 0.42, null, "Marque", "EPREL")), null, null);
        given(manifestService.snapshot()).willReturn(available(withColumns));

        OpenDataDatasetDto gtin = service.gtin(DomainLanguage.fr);

        assertThat(gtin.id()).isEqualTo("gtin");
        assertThat(gtin.generatedAt()).isEqualTo(GENERATED_AT);
        assertThat(gtin.sizeBytes()).isEqualTo(6_400_000_000L);
        assertThat(gtin.license()).isEqualTo("ODbL-1.0");
        assertThat(gtin.columns()).extracting(column -> column.name() + "=" + column.fillRate())
                .containsExactly("gtin=1.0", "brand=0.42");
        assertThat(gtin.columns().get(1).description()).isEqualTo("Marque");
        assertThat(gtin.columns().get(1).source()).isEqualTo("EPREL");
    }

    @Test
    void impactDatasetsListEveryVerticalButNotTheGlobalArchive() {
        given(manifestService.snapshot()).willReturn(available(
                dataset("gtin", 10, 100, SHA256, GENERATED_AT),
                dataset("impact-dishwasher", 5, 50, "d1", GENERATED_AT),
                dataset("impact-oven", 7, 70, "d2", GENERATED_AT),
                dataset("impact-all", 12, 120, "d3", GENERATED_AT)));

        List<OpenDataDatasetDto> impact = service.impactDatasets(DomainLanguage.fr);

        assertThat(impact).extracting(OpenDataDatasetDto::id).containsExactly("impact-dishwasher", "impact-oven");
        assertThat(impact).allSatisfy(dataset -> assertThat(dataset.type()).isEqualTo("IMPACT"));
        assertThat(impact.get(0).downloadUrl()).isEqualTo("https://data.nudger.fr/impact-dishwasher-open-data.zip");
        assertThat(impact.get(0).sha256()).isEqualTo("d1");
        assertThat(service.impactDataset(DomainLanguage.fr, "oven")).map(OpenDataDatasetDto::recordCountValue)
                .contains(7L);
        assertThat(service.impactDataset(DomainLanguage.fr, "all")).isEmpty();
        assertThat(service.impactDataset(DomainLanguage.fr, "unknown")).isEmpty();
    }

    @Test
    void impactDatasetsAreEmptyWithoutManifest() {
        given(manifestService.snapshot()).willReturn(OpenDataManifestSnapshot.NOT_CONFIGURED);

        assertThat(service.impactDatasets(DomainLanguage.fr)).isEmpty();
        assertThat(service.impactDataset(DomainLanguage.fr, "oven")).isEmpty();
    }

    private static OpenDataManifestSnapshot available(DatasetManifest... datasets) {
        OpenDataManifest manifest = new OpenDataManifest(OpenDataManifest.SCHEMA_VERSION, GENERATED_AT,
                OpenDataManifest.LICENSE, List.of(datasets));
        return new OpenDataManifestSnapshot(State.AVAILABLE, manifest, Instant.parse("2026-11-23T10:00:00Z"), null);
    }

    private static DatasetManifest dataset(String id, long rows, long bytes, String sha256, String generatedAt) {
        return new DatasetManifest(OpenDataManifest.SCHEMA_VERSION, id, id + "-open-data.zip", null,
                DatasetManifest.FORMAT, OpenDataManifest.LICENSE, bytes, sha256, rows, rows, generatedAt, List.of(), 1, null);
    }
}
