package org.open4goods.eprelservice.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.open4goods.model.eprel.EprelProduct;
import org.open4goods.services.eprelservice.service.JsonZipEprelCatalogueParser;

/**
 * Tests for {@link JsonZipEprelCatalogueParser}.
 */
class JsonZipEprelCatalogueParserTest
{
    private final JsonZipEprelCatalogueParser parser = new JsonZipEprelCatalogueParser();

    @Test
    @DisplayName("The parser should convert JSON entries contained in a ZIP archive")
    void shouldParseJsonEntries() throws IOException
    {
        Path zipFile = Files.createTempFile("eprel-parser-test", ".zip");
        try
        {
            writeZipWithProducts(zipFile);
            List<EprelProduct> products = new ArrayList<>();
            parser.parse(zipFile, products::add);
            assertThat(products).hasSize(2);
            assertThat(products.get(0).getModelIdentifier()).isEqualTo("MODEL-1");
            assertThat(products.get(1).getCategorySpecificAttributes()).containsEntry("custom", "value");
        }
        finally
        {
            Files.deleteIfExists(zipFile);
        }
    }

    private void writeZipWithProducts(Path zipFile) throws IOException
    {
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(zipFile)))
        {
            zipOutputStream.putNextEntry(new ZipEntry("products.json"));
            String json = "[" +
                    "{\"eprelRegistrationNumber\":\"1\",\"productGroup\":\"televisions\",\"modelIdentifier\":\"MODEL-1\"}," +
                    "{\"eprelRegistrationNumber\":\"2\",\"productGroup\":\"televisions\",\"modelIdentifier\":\"MODEL-2\",\"custom\":\"value\"}" +
                    "]";
            zipOutputStream.write(json.getBytes(StandardCharsets.UTF_8));
            zipOutputStream.closeEntry();
        }
    }
}
