package org.open4goods.nudgerfrontapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.FileOutputStream;
import java.text.NumberFormat;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.open4goods.nudgerfrontapi.dto.opendata.OpenDataDatasetDto;
import org.open4goods.model.localization.DomainLanguage;
import org.open4goods.nudgerfrontapi.config.properties.ApiProperties;
import org.open4goods.services.opendata.config.OpenDataConfig;
import org.open4goods.services.opendata.service.OpenDataService;

class OpenDataMappingServiceTest {

    @Test
    void gtinDatasetIncludesRawRecordCount() throws Exception {
        OpenDataService openDataService = mock(OpenDataService.class);
        OpenDataConfig openDataConfig = mock(OpenDataConfig.class);
        ApiProperties apiProperties = mock(ApiProperties.class);

        File datasetFile = File.createTempFile("gtin-dataset", ".zip");
        datasetFile.deleteOnExit();
        try (FileOutputStream outputStream = new FileOutputStream(datasetFile)) {
            outputStream.write("data".getBytes());
        }

        given(openDataService.totalItemsGTIN()).willReturn(9_876L);
        given(openDataConfig.gtinZipFile()).willReturn(datasetFile);
        given(apiProperties.getResourceRootPath()).willReturn("https://static.example");

        OpenDataMappingService service = new OpenDataMappingService(openDataService, openDataConfig, apiProperties);

        OpenDataDatasetDto dto = service.gtin(DomainLanguage.en);

        Locale locale = Locale.forLanguageTag(DomainLanguage.en.languageTag());
        String expectedFormattedCount = NumberFormat.getIntegerInstance(locale).format(9_876L);

        assertThat(dto.recordCount()).isEqualTo(expectedFormattedCount);
        assertThat(dto.recordCountValue()).isEqualTo(9_876L);
        assertThat(dto.downloadUrl()).isEqualTo("https://static.example/opendata/gtin-open-data.zip");
    }

    @Test
    void downloadUrlUsesPublicBaseUrlWhenConfigured() throws Exception {
        OpenDataService openDataService = mock(OpenDataService.class);
        OpenDataConfig openDataConfig = mock(OpenDataConfig.class);
        ApiProperties apiProperties = mock(ApiProperties.class);

        given(openDataConfig.gtinZipFile()).willReturn(new File("missing-gtin.zip"));
        given(openDataConfig.isbnZipFile()).willReturn(new File("missing-isbn.zip"));
        given(openDataConfig.getPublicBaseUrl()).willReturn("https://data.nudger.fr/");
        given(apiProperties.getResourceRootPath()).willReturn("https://static.example");

        OpenDataMappingService service = new OpenDataMappingService(openDataService, openDataConfig, apiProperties);

        assertThat(service.gtin(DomainLanguage.en).downloadUrl()).isEqualTo("https://data.nudger.fr/gtin-open-data.zip");
        assertThat(service.isbn(DomainLanguage.en).downloadUrl()).isEqualTo("https://data.nudger.fr/isbn-open-data.zip");
    }
}
